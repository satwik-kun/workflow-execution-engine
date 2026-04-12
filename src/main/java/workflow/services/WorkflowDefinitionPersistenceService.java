package workflow.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import workflow.models.Task;
import workflow.models.TaskStatus;
import workflow.models.Workflow;
import workflow.persistence.WorkflowDefinitionRecord;
import workflow.persistence.WorkflowDefinitionRepository;

@Service
public class WorkflowDefinitionPersistenceService {
    private final WorkflowDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;
    private int nextWorkflowId;

    public WorkflowDefinitionPersistenceService(
        WorkflowDefinitionRepository definitionRepository,
        ObjectMapper objectMapper
    ) {
        this.definitionRepository = definitionRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeSequence() {
        this.nextWorkflowId = definitionRepository.findMaxId() + 1;
    }

    public synchronized int allocateWorkflowId() {
        int id = nextWorkflowId;
        nextWorkflowId++;
        return id;
    }

    public void save(Workflow workflow) {
        WorkflowDefinitionRecord record = new WorkflowDefinitionRecord();
        record.setId(workflow.getWorkflowId());
        record.setWorkflowName(workflow.getWorkflowName());
        record.setWorkflowSnapshot(serializeWorkflow(workflow));
        definitionRepository.save(record);
    }

    public Workflow findById(int workflowId) {
        WorkflowDefinitionRecord record = definitionRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + workflowId));
        return deserializeWorkflow(record.getWorkflowSnapshot());
    }

    private String serializeWorkflow(Workflow workflow) {
        WorkflowSnapshot snapshot = WorkflowSnapshot.from(workflow);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize workflow definition", e);
        }
    }

    private Workflow deserializeWorkflow(String json) {
        try {
            WorkflowSnapshot snapshot = objectMapper.readValue(json, WorkflowSnapshot.class);
            Workflow workflow = new Workflow(snapshot.workflowId, snapshot.workflowName);
            for (TaskSnapshot task : snapshot.tasks) {
                workflow.addTask(new Task(task.taskId, task.taskName, task.assignedRole, TaskStatus.from(task.status)));
            }
            snapshot.transitions.forEach((fromTaskId, nextTaskIds) -> {
                for (Integer toTaskId : nextTaskIds) {
                    workflow.addTransition(fromTaskId, toTaskId);
                }
            });
            return workflow;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize workflow definition", e);
        }
    }

    private static class WorkflowSnapshot {
        public int workflowId;
        public String workflowName;
        public List<TaskSnapshot> tasks;
        public java.util.Map<Integer, List<Integer>> transitions;

        public static WorkflowSnapshot from(Workflow workflow) {
            WorkflowSnapshot snapshot = new WorkflowSnapshot();
            snapshot.workflowId = workflow.getWorkflowId();
            snapshot.workflowName = workflow.getWorkflowName();
            snapshot.tasks = new ArrayList<>();
            for (Task task : workflow.getTasks()) {
                TaskSnapshot taskSnapshot = new TaskSnapshot();
                taskSnapshot.taskId = task.getTaskId();
                taskSnapshot.taskName = task.getTaskName();
                taskSnapshot.assignedRole = task.getAssignedRole();
                taskSnapshot.status = task.getTaskStatus().name();
                snapshot.tasks.add(taskSnapshot);
            }
            snapshot.transitions = workflow.getTransitions();
            return snapshot;
        }
    }

    private static class TaskSnapshot {
        public int taskId;
        public String taskName;
        public String assignedRole;
        public String status;
    }
}
