package workflow.models;

public class WorkflowInstance {
    private final String id;
    private final String workflowId;
    private String status;

    public WorkflowInstance(String id, String workflowId, String status) {
        this.id = id;
        this.workflowId = workflowId;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
