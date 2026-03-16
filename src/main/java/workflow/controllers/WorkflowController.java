package workflow.controllers;

import workflow.models.Workflow;

public class WorkflowController {
    public Workflow createWorkflow(int workflowId, String workflowName) {
        return new Workflow(workflowId, workflowName);
    }
}
