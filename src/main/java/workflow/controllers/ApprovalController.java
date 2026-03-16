package workflow.controllers;

public class ApprovalController {
    public boolean approveTask(String taskId) {
        return taskId != null && !taskId.isBlank();
    }
}
