package workflow.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;

class ExecutionServiceTest {
    @Test
    void executeTask_whenSuccessful_movesToNextTask() {
        Workflow workflow = buildTwoTaskWorkflow();
        WorkflowInstance instance = new WorkflowInstance(1, workflow);
        instance.start();
        instance.setCurrentTask(1);

        ExecutionService executionService = new ExecutionService(new ScriptedRandom(true));

        boolean result = executionService.executeTask(instance);

        assertTrue(result);
        assertEquals(2, instance.getCurrentTask());
        assertEquals("SUCCESS", workflow.getTaskById(1).getStatus());
        assertEquals(WorkflowInstance.STATE_RUNNING, instance.getState());
    }

    @Test
    void executeTask_whenFailure_recordsFailureDetails() {
        Workflow workflow = buildTwoTaskWorkflow();
        WorkflowInstance instance = new WorkflowInstance(1, workflow);
        instance.start();
        instance.setCurrentTask(1);

        ExecutionService executionService = new ExecutionService(new ScriptedRandom(false));

        boolean result = executionService.executeTask(instance);

        assertFalse(result);
        assertEquals("FAILURE", workflow.getTaskById(1).getStatus());
        assertEquals(WorkflowInstance.STATE_RUNNING, instance.getState());
        assertTrue(instance.getLastFailureDetails().contains("failed during execution"));
    }

    private Workflow buildTwoTaskWorkflow() {
        Workflow workflow = new Workflow(1001, "Test Workflow");
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
