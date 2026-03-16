package workflow.services;

import java.util.List;
import java.util.Random;

import workflow.models.Task;
import workflow.models.WorkflowInstance;

public class ExecutionService {
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_SUCCESS = "SUCCESS";
    private static final String TASK_STATUS_FAILURE = "FAILURE";

    private final Random random;

    public ExecutionService() {
        this.random = new Random();
    }

    public void execute(WorkflowInstance workflowInstance) {
        if (workflowInstance != null) {
            workflowInstance.start();
            workflowInstance.addHistory("Execution service marked instance as RUNNING");
        }
    }

    public boolean executeTask(WorkflowInstance instance) {
        if (instance == null || instance.getWorkflow() == null) {
            return false;
        }

        Task currentTask = identifyCurrentTask(instance);
        if (currentTask == null) {
            instance.addHistory("No executable task found for instance " + instance.getInstanceId());
            return false;
        }

        boolean isSuccess = random.nextBoolean();
        String executionStatus = isSuccess ? TASK_STATUS_SUCCESS : TASK_STATUS_FAILURE;
        updateTaskState(currentTask, executionStatus);

        instance.addHistory(
            "Task " + currentTask.getTaskId() + " (" + currentTask.getTaskName() + ") executed with status " + executionStatus
        );

        if (!isSuccess) {
            instance.setStatus(WorkflowInstance.STATE_FAILED);
            instance.addHistory("Workflow instance marked as FAILED after task failure");
        }

        return isSuccess;
    }

    public void updateTaskState(Task task, String status) {
        if (task != null && status != null && !status.isBlank()) {
            task.setStatus(status);
        }
    }

    private Task identifyCurrentTask(WorkflowInstance instance) {
        List<Task> tasks = instance.getWorkflow().getTasks();
        if (tasks.isEmpty()) {
            return null;
        }

        int currentTaskId = instance.getCurrentTask();
        if (currentTaskId < 0) {
            Task firstTask = tasks.get(0);
            instance.setCurrentTask(firstTask.getTaskId());
            instance.addHistory("Initialized current task to " + firstTask.getTaskId());
            if (TASK_STATUS_PENDING.equalsIgnoreCase(firstTask.getStatus())) {
                updateTaskState(firstTask, TASK_STATUS_PENDING);
            }
            return firstTask;
        }

        for (Task task : tasks) {
            if (task.getTaskId() == currentTaskId) {
                return task;
            }
        }

        return null;
    }
}
