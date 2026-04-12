package workflow.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import workflow.models.Task;
import workflow.models.TaskStatus;
import workflow.services.TaskFactory;

class TaskFactoryTest {
    @Test
    void createPendingTask_shouldCreateTaskWithPendingStatus() {
        TaskFactory factory = new TaskFactory();

        Task task = factory.createPendingTask(1, "Submit", "EMPLOYEE");

        assertEquals(1, task.getTaskId());
        assertEquals("Submit", task.getTaskName());
        assertEquals("EMPLOYEE", task.getAssignedRole());
        assertEquals(TaskStatus.PENDING, task.getTaskStatus());
    }

    @Test
    void createTask_shouldUseProvidedStatus() {
        TaskFactory factory = new TaskFactory();

        Task task = factory.createTask(2, "Approve", "MANAGER", TaskStatus.APPROVED);

        assertEquals(TaskStatus.APPROVED, task.getTaskStatus());
    }
}
