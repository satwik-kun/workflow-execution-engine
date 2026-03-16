package workflow.controllers;

import workflow.models.WorkflowInstance;
import workflow.services.ExecutionService;

public class ExecutionController {
    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    public void startExecution(WorkflowInstance workflowInstance) {
        executionService.execute(workflowInstance);
    }
}
