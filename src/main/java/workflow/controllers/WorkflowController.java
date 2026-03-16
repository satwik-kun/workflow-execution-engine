package workflow.controllers;

import workflow.models.Workflow;

public class WorkflowController {
    public Workflow createWorkflow(String id, String name) {
        return new Workflow(id, name);
    }
}
