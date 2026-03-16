package workflow.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkflowInstance {
    public static final String STATE_CREATED = "CREATED";
    public static final String STATE_RUNNING = "RUNNING";
    public static final String STATE_COMPLETED = "COMPLETED";
    public static final String STATE_FAILED = "FAILED";

    private final int instanceId;
    private final Workflow workflow;
    private int currentTaskId;
    private String state;
    private final List<String> executionHistory;
    private int retryCount;
    private String lastFailureDetails;

    public WorkflowInstance(int instanceId, Workflow workflow) {
        this.instanceId = instanceId;
        this.workflow = workflow;
        this.currentTaskId = -1;
        this.state = STATE_CREATED;
        this.executionHistory = new ArrayList<>();
        this.retryCount = 0;
        this.lastFailureDetails = "";
    }

    public int getInstanceId() {
        return instanceId;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void start() {
        state = STATE_RUNNING;
    }

    public void setCurrentTask(int taskId) {
        this.currentTaskId = taskId;
    }

    public int getCurrentTask() {
        return currentTaskId;
    }

    public void addHistory(String event) {
        executionHistory.add(event);
    }

    public List<String> getHistory() {
        return Collections.unmodifiableList(executionHistory);
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        retryCount++;
    }

    public void resetRetryCount() {
        retryCount = 0;
    }

    public String getLastFailureDetails() {
        return lastFailureDetails;
    }

    public void setLastFailureDetails(String lastFailureDetails) {
        this.lastFailureDetails = lastFailureDetails == null ? "" : lastFailureDetails;
    }

    public String getState() {
        return state;
    }

    // Backward-compatible alias for existing callers that still use status wording.
    public String getStatus() {
        return getState();
    }

    // Backward-compatible alias for existing callers that still set status directly.
    public void setStatus(String status) {
        this.state = status;
    }
}
