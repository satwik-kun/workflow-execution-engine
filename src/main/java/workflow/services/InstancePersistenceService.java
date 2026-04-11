package workflow.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;
import workflow.persistence.WorkflowHistoryRecord;
import workflow.persistence.WorkflowHistoryRepository;
import workflow.persistence.WorkflowInstanceRecord;
import workflow.persistence.WorkflowInstanceRepository;

@Service
public class InstancePersistenceService {
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;
    private int nextInstanceId;

    public InstancePersistenceService(
        WorkflowInstanceRepository instanceRepository,
        WorkflowHistoryRepository historyRepository,
        ObjectMapper objectMapper
    ) {
        this.instanceRepository = instanceRepository;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeIdSequence() {
        int maxId = instanceRepository.findMaxId();
        this.nextInstanceId = maxId + 1;
    }

    public synchronized int allocateInstanceId() {
        int id = nextInstanceId;
        nextInstanceId++;
        return id;
    }

    @Transactional
    public void save(WorkflowInstance instance) {
        WorkflowInstanceRecord record = toRecord(instance);
        instanceRepository.save(record);

        historyRepository.deleteByInstanceId(instance.getInstanceId());
        List<WorkflowHistoryRecord> historyRecords = new ArrayList<>();
        int sequence = 1;
        for (String event : instance.getHistory()) {
            WorkflowHistoryRecord historyRecord = new WorkflowHistoryRecord();
            historyRecord.setInstanceId(instance.getInstanceId());
            historyRecord.setSequenceNo(sequence);
            historyRecord.setEventText(event);
            historyRecords.add(historyRecord);
            sequence++;
        }
        historyRepository.saveAll(historyRecords);
    }

    public WorkflowInstance findById(int instanceId) {
        WorkflowInstanceRecord record = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + instanceId));

        Workflow workflow = deserializeWorkflow(record.getWorkflowSnapshot());
        WorkflowInstance instance = new WorkflowInstance(record.getId(), workflow);
        instance.setCurrentTask(record.getCurrentTaskId());
        instance.setStatus(record.getState());

        for (int i = 0; i < record.getRetryCount(); i++) {
            instance.incrementRetryCount();
        }
        instance.setLastFailureDetails(record.getLastFailureDetails());

        List<WorkflowHistoryRecord> history = historyRepository.findByInstanceIdOrderBySequenceNoAsc(instanceId);
        for (WorkflowHistoryRecord event : history) {
            instance.addHistory(event.getEventText());
        }

        return instance;
    }

    private WorkflowInstanceRecord toRecord(WorkflowInstance instance) {
        WorkflowInstanceRecord record = new WorkflowInstanceRecord();
        record.setId(instance.getInstanceId());
        record.setWorkflowId(instance.getWorkflow().getWorkflowId());
        record.setWorkflowName(instance.getWorkflow().getWorkflowName());
        record.setCurrentTaskId(instance.getCurrentTask());
        record.setState(instance.getState());
        record.setRetryCount(instance.getRetryCount());
        record.setLastFailureDetails(instance.getLastFailureDetails());
        record.setWorkflowSnapshot(serializeWorkflow(instance.getWorkflow()));
        return record;
    }

    private String serializeWorkflow(Workflow workflow) {
        WorkflowSnapshot snapshot = WorkflowSnapshot.from(workflow);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize workflow snapshot", e);
        }
    }

    private Workflow deserializeWorkflow(String json) {
        try {
            WorkflowSnapshot snapshot = objectMapper.readValue(json, WorkflowSnapshot.class);
            Workflow workflow = new Workflow(snapshot.workflowId, snapshot.workflowName);
            for (TaskSnapshot task : snapshot.tasks) {
                workflow.addTask(new Task(task.taskId, task.taskName, task.assignedRole, task.status));
            }
            snapshot.transitions.forEach((fromTaskId, nextTaskIds) -> {
                for (Integer toTaskId : nextTaskIds) {
                    workflow.addTransition(fromTaskId, toTaskId);
                }
            });
            return workflow;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize workflow snapshot", e);
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
                taskSnapshot.status = task.getStatus();
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
