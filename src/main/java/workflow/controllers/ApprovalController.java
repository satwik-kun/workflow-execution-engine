package workflow.controllers;

import workflow.models.WorkflowInstance;
import workflow.services.ApprovalService;
import workflow.services.ApproveTaskDecisionStrategy;
import workflow.services.ExecutionService;
import workflow.services.RejectTaskDecisionStrategy;

public class ApprovalController {
    private final ApprovalService approvalService;

    public ApprovalController() {
        this(new ExecutionService(false));
    }

    public ApprovalController(ExecutionService executionService) {
        this(
            new ApprovalService(
                executionService,
                java.util.List.of(
                    new ApproveTaskDecisionStrategy(),
                    new RejectTaskDecisionStrategy()
                )
            )
        );
    }

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    public void approveTask(WorkflowInstance instance) {
        approvalService.approveTask(instance);
    }

    public void rejectTask(WorkflowInstance instance) {
        approvalService.rejectTask(instance);
    }
}
