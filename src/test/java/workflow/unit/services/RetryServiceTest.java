package workflow.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;
import workflow.models.WorkflowStatus;
import workflow.services.ExecutionService;
import workflow.services.RetryService;

class RetryServiceTest {
    @Test
    void handleFailure_whenRetryEventuallySucceeds_keepsWorkflowRunning() {
        Workflow workflow = buildTwoTaskWorkflow();
        WorkflowInstance instance = new WorkflowInstance(1, workflow);
        instance.start();
        instance.setCurrentTask(1);

        ExecutionService executionService = new ExecutionService(new ScriptedRandom(false, true));
        RetryService retryService = new RetryService(executionService);

        retryService.handleFailure(instance);

        assertEquals(2, instance.getCurrentTask());
        assertEquals(WorkflowStatus.RUNNING.name(), instance.getState());
        assertEquals(0, instance.getRetryCount());
        assertTrue(instance.getHistory().stream().anyMatch(event -> event.contains("recovered successfully")));
    }

    @Test
    void handleFailure_whenRetriesExhausted_marksWorkflowFailed() {
        Workflow workflow = buildTwoTaskWorkflow();
        WorkflowInstance instance = new WorkflowInstance(1, workflow);
        instance.start();
        instance.setCurrentTask(1);

        ExecutionService executionService = new ExecutionService(new ScriptedRandom(false, false, false));
        RetryService retryService = new RetryService(executionService);

        retryService.handleFailure(instance);

        assertEquals(WorkflowStatus.FAILED.name(), instance.getState());
        assertTrue(instance.getHistory().stream().anyMatch(event -> event.contains("Retry limit reached")));
    }

    private Workflow buildTwoTaskWorkflow() {
        Workflow workflow = new Workflow(1001, "Retry Workflow");
        workflow.addTask(new Task(1, "Start", "EMPLOYEE", "PENDING"));
        workflow.addTask(new Task(2, "End", "OPS", "PENDING"));
        workflow.addTransition(1, 2);
        return workflow;
    }

    private static class ScriptedRandom extends Random {
        private final boolean[] outcomes;
        private int index = 0;

        private ScriptedRandom(boolean... outcomes) {
            this.outcomes = outcomes;
        }

        @Override
        public boolean nextBoolean() {
            boolean value = outcomes[Math.min(index, outcomes.length - 1)];
            index++;
            return value;
        }
    }
}
