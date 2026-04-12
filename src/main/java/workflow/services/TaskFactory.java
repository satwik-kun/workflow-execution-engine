package workflow.services;

import org.springframework.stereotype.Component;
import workflow.models.Task;
import workflow.models.TaskStatus;

@Component
public class TaskFactory {
    public Task createPendingTask(int taskId, String taskName, String assignedRole) {
        return new Task(taskId, taskName, assignedRole, TaskStatus.PENDING);
    }

    public Task createTask(int taskId, String taskName, String assignedRole, TaskStatus status) {
        return new Task(taskId, taskName, assignedRole, status);
    }
}