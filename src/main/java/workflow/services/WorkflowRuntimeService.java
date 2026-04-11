package workflow.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import workflow.controllers.ApprovalController;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;

@Service
public class WorkflowRuntimeService {
    private final ValidationService validationService;
    private final ExecutionService executionService;
    private final RetryService retryService;
    private final ApprovalController approvalController;
    private final InstancePersistenceService persistenceService;
    private final WorkflowDefinitionPersistenceService definitionPersistenceService;

    public WorkflowRuntimeService(
        InstancePersistenceService persistenceService,
        WorkflowDefinitionPersistenceService definitionPersistenceService,
        @Value("${workflow.execution.demo-mode:false}") boolean deterministicDemoMode
    ) {
        this.validationService = new ValidationService();
        this.executionService = new ExecutionService(deterministicDemoMode);
        this.retryService = new RetryService(executionService);
        this.approvalController = new ApprovalController(executionService);
        this.persistenceService = persistenceService;
        this.definitionPersistenceService = definitionPersistenceService;
    }

    public Workflow createWorkflow(String workflowName, List<Task> tasks, List<int[]> transitions) {
        validateCreateInputs(workflowName, tasks, transitions);

        int workflowId = definitionPersistenceService.allocateWorkflowId();
        Workflow workflow = new Workflow(workflowId, workflowName);
        for (Task task : tasks) {
            workflow.addTask(task);
        }
        for (int[] transition : transitions) {
            workflow.addTransition(transition[0], transition[1]);
        }

        if (!validationService.validateWorkflow(workflow)) {
            throw new IllegalArgumentException("Workflow validation failed.");
        }

        definitionPersistenceService.save(workflow);
        return workflow;
    }

    public WorkflowInstance startInstance(int workflowId) {
        Workflow workflow = definitionPersistenceService.findById(workflowId);

        Workflow instanceWorkflow = cloneWorkflow(workflow);
        int instanceId = persistenceService.allocateInstanceId();
        WorkflowInstance instance = new WorkflowInstance(instanceId, instanceWorkflow);
        instance.start();

        if (!instanceWorkflow.getTasks().isEmpty()) {
            int firstTaskId = instanceWorkflow.getTasks().get(0).getTaskId();
            instance.setCurrentTask(firstTaskId);
            instance.addHistory("Activated first task: " + firstTaskId);
        }
        instance.addHistory("Workflow instance " + instance.getInstanceId() + " started in state " + instance.getState());

        persistenceService.save(instance);
        return instance;
    }

    public WorkflowInstance executeCurrentTask(int instanceId) {
        WorkflowInstance instance = persistenceService.findById(instanceId);
        ensureInstanceInState(instance, WorkflowInstance.STATE_RUNNING, "execute task");
        executionService.executeTask(instance);
        persistenceService.save(instance);
        return instance;
    }

    public WorkflowInstance retryCurrentTask(int instanceId) {
        WorkflowInstance instance = persistenceService.findById(instanceId);
        ensureInstanceInState(instance, WorkflowInstance.STATE_RUNNING, "retry task");

        Task currentTask = getCurrentTaskOrThrow(instance);
        if (!"FAILURE".equalsIgnoreCase(currentTask.getStatus())) {
            throw new IllegalArgumentException(
                "Retry is allowed only when current task is in FAILURE state"
            );
        }

        retryService.handleFailure(instance);
        persistenceService.save(instance);
        return instance;
    }

    public WorkflowInstance approveCurrentTask(int instanceId) {
        WorkflowInstance instance = persistenceService.findById(instanceId);
        ensureInstanceInState(instance, WorkflowInstance.STATE_RUNNING, "approve task");
        Task currentTask = getCurrentTaskOrThrow(instance);
        if (!"MANAGER".equalsIgnoreCase(currentTask.getAssignedRole())) {
            throw new IllegalArgumentException("Only manager tasks can be approved");
        }
        if (!"PENDING".equalsIgnoreCase(currentTask.getStatus())) {
            throw new IllegalArgumentException("Only PENDING tasks can be approved");
        }

        approvalController.approveTask(instance);
        persistenceService.save(instance);
        return instance;
    }

    public WorkflowInstance rejectCurrentTask(int instanceId) {
        WorkflowInstance instance = persistenceService.findById(instanceId);
        ensureInstanceInState(instance, WorkflowInstance.STATE_RUNNING, "reject task");
        Task currentTask = getCurrentTaskOrThrow(instance);
        if (!"MANAGER".equalsIgnoreCase(currentTask.getAssignedRole())) {
            throw new IllegalArgumentException("Only manager tasks can be rejected");
        }
        if (!"PENDING".equalsIgnoreCase(currentTask.getStatus())) {
            throw new IllegalArgumentException("Only PENDING tasks can be rejected");
        }

        approvalController.rejectTask(instance);
        persistenceService.save(instance);
        return instance;
    }

    public WorkflowInstance getInstance(int instanceId) {
        return persistenceService.findById(instanceId);
    }

    private Workflow cloneWorkflow(Workflow source) {
        Workflow clone = new Workflow(source.getWorkflowId(), source.getWorkflowName());
        for (Task task : source.getTasks()) {
            clone.addTask(new Task(task.getTaskId(), task.getTaskName(), task.getAssignedRole(), task.getStatus()));
        }
        for (Map.Entry<Integer, List<Integer>> transition : source.getTransitions().entrySet()) {
            for (Integer nextTask : transition.getValue()) {
                clone.addTransition(transition.getKey(), nextTask);
            }
        }
        return clone;
    }

    public static List<int[]> toTransitions(List<TransitionEdge> edges) {
        List<int[]> transitions = new ArrayList<>();
        for (TransitionEdge edge : edges) {
            transitions.add(new int[] {edge.fromTaskId(), edge.toTaskId()});
        }
        return transitions;
    }

    public record TransitionEdge(int fromTaskId, int toTaskId) {
    }

    private void ensureInstanceInState(WorkflowInstance instance, String expectedState, String operation) {
        if (!expectedState.equals(instance.getState())) {
            throw new IllegalArgumentException(
                "Cannot " + operation + " when instance state is " + instance.getState()
            );
        }
    }

    private Task getCurrentTaskOrThrow(WorkflowInstance instance) {
        Task currentTask = instance.getWorkflow().getTaskById(instance.getCurrentTask());
        if (currentTask == null) {
            throw new IllegalArgumentException("No active current task found for instance");
        }
        return currentTask;
    }

    private void validateCreateInputs(String workflowName, List<Task> tasks, List<int[]> transitions) {
        if (workflowName == null || workflowName.isBlank()) {
            throw new IllegalArgumentException("workflowName is required");
        }
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("At least one task is required");
        }
        if (transitions == null || transitions.isEmpty()) {
            throw new IllegalArgumentException("At least one transition is required");
        }

        Set<Integer> seenTaskIds = new HashSet<>();
        for (Task task : tasks) {
            if (!seenTaskIds.add(task.getTaskId())) {
                throw new IllegalArgumentException("Duplicate taskId found: " + task.getTaskId());
            }
        }

        for (int[] transition : transitions) {
            if (transition == null || transition.length != 2) {
                throw new IllegalArgumentException("Each transition must contain fromTaskId and toTaskId");
            }
            if (transition[0] == transition[1]) {
                throw new IllegalArgumentException("Self-loop transitions are not allowed for taskId: " + transition[0]);
            }
        }
    }
}
