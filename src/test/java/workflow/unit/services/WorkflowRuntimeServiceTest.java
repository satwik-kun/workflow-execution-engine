package workflow.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import workflow.models.Task;
import workflow.services.WorkflowRuntimeService;
import workflow.services.WorkflowDefinitionPersistenceService;
import workflow.models.Workflow;

class WorkflowRuntimeServiceTest {
    @Test
    void createWorkflow_withDuplicateTaskIds_shouldThrow() {
        WorkflowRuntimeService service = new WorkflowRuntimeService(null, null, true);

        List<Task> tasks = List.of(
            new Task(1, "Submit", "EMPLOYEE", "PENDING"),
            new Task(1, "Approve", "MANAGER", "PENDING")
        );
        List<int[]> transitions = List.of(new int[] {1, 2});

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.createWorkflow("Duplicate Id Workflow", tasks, transitions)
        );

        assertTrue(ex.getMessage().contains("Duplicate taskId"));
    }

    @Test
    void createWorkflow_withSelfLoopTransition_shouldThrow() {
        WorkflowRuntimeService service = new WorkflowRuntimeService(null, null, true);

        List<Task> tasks = List.of(new Task(1, "Submit", "EMPLOYEE", "PENDING"));
        List<int[]> transitions = List.of(new int[] {1, 1});

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.createWorkflow("Self Loop Workflow", tasks, transitions)
        );

        assertTrue(ex.getMessage().contains("Self-loop transitions"));
    }

    @Test
    void createWorkflow_withValidInput_shouldCreateDefinition() {
        WorkflowRuntimeService service = new WorkflowRuntimeService(null, fakeDefinitionPersistence(), true);

        List<Task> tasks = List.of(
            new Task(1, "Submit", "EMPLOYEE", "PENDING"),
            new Task(2, "Approve", "MANAGER", "PENDING")
        );
        List<int[]> transitions = List.of(new int[] {1, 2});

        var workflow = service.createWorkflow("Valid Workflow", tasks, transitions);

        assertEquals("Valid Workflow", workflow.getWorkflowName());
        assertEquals(2, workflow.getTasks().size());
    }

    private WorkflowDefinitionPersistenceService fakeDefinitionPersistence() {
        return new WorkflowDefinitionPersistenceService(null, null) {
            @Override
            public synchronized int allocateWorkflowId() {
                return 1001;
            }

            @Override
            public void save(Workflow workflow) {
                // no-op for unit test
            }
        };
    }
}
