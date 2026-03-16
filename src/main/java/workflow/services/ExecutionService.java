package workflow.services;

import workflow.models.WorkflowInstance;

public class ExecutionService {
    public void execute(WorkflowInstance workflowInstance) {
        if (workflowInstance != null) {
            workflowInstance.start();
            workflowInstance.addHistory("Execution service marked instance as RUNNING");
        }
    }
}
