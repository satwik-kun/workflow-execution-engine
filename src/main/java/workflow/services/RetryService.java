package workflow.services;

import workflow.models.WorkflowInstance;

public class RetryService {
    public void retry(WorkflowInstance workflowInstance) {
        if (workflowInstance != null) {
            workflowInstance.setStatus(WorkflowInstance.STATE_FAILED);
            workflowInstance.addHistory("Retry requested after failure");
        }
    }
}
