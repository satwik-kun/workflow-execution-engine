package workflow.models;

public class Task {
    private final int taskId;
    private final String taskName;
    private final String assignedRole;
    private String status;

    public Task(int taskId, String taskName, String assignedRole, String status) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.assignedRole = assignedRole;
        this.status = status;
    }

    public int getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getAssignedRole() {
        return assignedRole;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
