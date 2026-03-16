package workflow.controllers;

import workflow.models.Task;
import workflow.models.WorkflowInstance;
import workflow.services.ExecutionService;

public class ApprovalController {
    private static final String TASK_STATUS_APPROVED = "APPROVED";
    private static final String TASK_STATUS_REJECTED = "REJECTED";

    private final ExecutionService executionService;

    public ApprovalController() {
        this(new ExecutionService());
    }

    public ApprovalController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    public void approveTask(WorkflowInstance instance) {
        Task currentTask = getCurrentTask(instance);
        if (currentTask == null) {
            return;
        }

        currentTask.setStatus(TASK_STATUS_APPROVED);
        instance.addHistory(
            "Approval decision recorded: task " + currentTask.getTaskId() + " approved"
        );
        executionService.moveToNextTask(instance);
    }

    public void rejectTask(WorkflowInstance instance) {
        Task currentTask = getCurrentTask(instance);
        if (currentTask == null) {
            return;
        }

        currentTask.setStatus(TASK_STATUS_REJECTED);
        instance.setStatus(WorkflowInstance.STATE_FAILED);
        instance.addHistory(
            "Approval decision recorded: task " + currentTask.getTaskId() + " rejected"
        );
        instance.addHistory("Workflow stopped after rejection.");
    }

    private Task getCurrentTask(WorkflowInstance instance) {
        if (instance == null || instance.getWorkflow() == null) {
            return null;
        }

        Task currentTask = instance.getWorkflow().getTaskById(instance.getCurrentTask());
        if (currentTask == null) {
            instance.addHistory("Approval requested with no active task.");
        }

        return currentTask;
    }
}
