package workflow.services;

import org.springframework.stereotype.Service;
import workflow.models.Task;
import workflow.models.WorkflowInstance;
import workflow.models.WorkflowStatus;

@Service
public class RetryService {
    private static final int MAX_RETRIES = 3;

    private final ExecutionService executionService;

    public RetryService() {
        this(new ExecutionService(false));
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
            instance.setStatus(WorkflowStatus.FAILED);
            instance.addHistory("Retry handling aborted: no active production step found.");
            return;
        }

        while (instance.getRetryCount() < MAX_RETRIES) {
            instance.incrementRetryCount();
            int attempt = instance.getRetryCount();
            instance.setStatus(WorkflowStatus.RUNNING);
            instance.addHistory(
                "Incident recovery attempt " + attempt + " of " + MAX_RETRIES + " for step " + currentTask.getTaskId()
            );

            boolean retrySucceeded = executionService.executeTask(instance);
            if (retrySucceeded) {
                instance.addHistory(
                    "Step " + currentTask.getTaskId() + " recovered successfully on attempt " + attempt
                );
                return;
            }
        }

        instance.setStatus(WorkflowStatus.FAILED);
        instance.addHistory(
            "Retry limit reached for step " + currentTask.getTaskId() + ". Work order marked FAILED"
        );
        instance.addHistory("Failure report: " + instance.getLastFailureDetails());
    }
}
