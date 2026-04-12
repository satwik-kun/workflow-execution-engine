package workflow.unit.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import workflow.models.TaskStatus;
import workflow.models.WorkflowStatus;

class StatusEnumTest {
    @Test
    void taskStatus_from_shouldParseCaseInsensitiveValues() {
        assertEquals(TaskStatus.PENDING, TaskStatus.from("pending"));
        assertEquals(TaskStatus.APPROVED, TaskStatus.from("APPROVED"));
    }

    @Test
    void workflowStatus_from_shouldRejectBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> WorkflowStatus.from(" "));
    }
}
