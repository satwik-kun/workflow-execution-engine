package workflow.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import workflow.controllers.ApprovalController;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;

@Service
public class WorkflowRuntimeService {
    private final Map<Integer, Workflow> workflowDefinitions = new ConcurrentHashMap<>();
    private final AtomicInteger workflowSequence = new AtomicInteger(1000);

    private final ValidationService validationService;
    private final ExecutionService executionService;
    private final RetryService retryService;
    private final ApprovalController approvalController;
    private final InstancePersistenceService persistenceService;

    public WorkflowRuntimeService(
        InstancePersistenceService persistenceService,
        @Value("${workflow.execution.demo-mode:false}") boolean deterministicDemoMode
    ) {
        this.validationService = new ValidationService();
        this.executionService = new ExecutionService(deterministicDemoMode);
        this.retryService = new RetryService(executionService);
        this.approvalController = new ApprovalController(executionService);
        this.persistenceService = persistenceService;
    }

    public Workflow createWorkflow(String workflowName, List<Task> tasks, List<int[]> transitions) {
        validateCreateInputs(workflowName, tasks, transitions);

        int workflowId = workflowSequence.incrementAndGet();
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

        workflowDefinitions.put(workflowId, workflow);
        return workflow;
    }

    public WorkflowInstance startInstance(int workflowId) {
        Workflow workflow = workflowDefinitions.get(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + workflowId);
        }

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
        executionService.executeTask(instance);
        persistenceService.save(instance);
        return instance;
    }

    public WorkflowInstance retryCurrentTask(int instanceId) {
        WorkflowInstance instance = persistenceService.findById(instanceId);
        retryService.handleFailure(instance);
        persistenceService.save(instance);
        return instance;
    }

    public WorkflowInstance approveCurrentTask(int instanceId) {
        WorkflowInstance instance = persistenceService.findById(instanceId);
        approvalController.approveTask(instance);
        persistenceService.save(instance);
        return instance;
    }

    public WorkflowInstance rejectCurrentTask(int instanceId) {
        WorkflowInstance instance = persistenceService.findById(instanceId);
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
