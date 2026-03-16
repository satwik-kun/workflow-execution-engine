package workflow.services;

import workflow.models.Task;
import workflow.models.WorkflowInstance;

public class RetryService {
    private static final int MAX_RETRIES = 3;

    private final ExecutionService executionService;

    public RetryService() {
        this(new ExecutionService());
    }

    public RetryService(ExecutionService executionService) {
        this.executionService = executionService;
    }

    public void handleFailure(WorkflowInstance instance) {
        if (instance == null || instance.getWorkflow() == null) {
            return;
        }

        Task currentTask = instance.getWorkflow().getTaskById(instance.getCurrentTask());
        if (currentTask == null) {
            instance.setStatus(WorkflowInstance.STATE_FAILED);
            instance.addHistory("Failure handling aborted because there is no active task.");
            return;
        }

        while (instance.getRetryCount() < MAX_RETRIES) {
            instance.incrementRetryCount();
            int attempt = instance.getRetryCount();
            instance.setStatus(WorkflowInstance.STATE_RUNNING);
            instance.addHistory(
                "Retry attempt " + attempt + " of " + MAX_RETRIES + " for task " + currentTask.getTaskId()
            );

            boolean retrySucceeded = executionService.executeTask(instance);
            if (retrySucceeded) {
                instance.addHistory(
                    "Task " + currentTask.getTaskId() + " recovered successfully on retry attempt " + attempt
                );
                return;
            }
        }

        instance.setStatus(WorkflowInstance.STATE_FAILED);
        instance.addHistory(
            "Retry limit reached for task " + currentTask.getTaskId() + ". Workflow marked FAILED"
        );
        instance.addHistory("Failure details: " + instance.getLastFailureDetails());
    }
}
