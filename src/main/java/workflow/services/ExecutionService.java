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
        this(new Random());
    }

    public ExecutionService(Random random) {
        this.random = random;
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

        if (WorkflowInstance.STATE_COMPLETED.equals(instance.getState())) {
            instance.addHistory("Execution skipped because workflow is already COMPLETED");
            return false;
        }

        if (WorkflowInstance.STATE_FAILED.equals(instance.getState())) {
            instance.addHistory("Execution skipped because workflow is already FAILED");
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

        if (isSuccess) {
            instance.resetRetryCount();
            instance.setLastFailureDetails("");
            moveToNextTask(instance);
            return true;
        }

        instance.setStatus(WorkflowInstance.STATE_RUNNING);
        instance.setLastFailureDetails(
            "Task " + currentTask.getTaskId() + " (" + currentTask.getTaskName() + ") failed during execution"
        );
        instance.addHistory("Task failure recorded. Retry handling may continue execution.");

        return false;
    }

    public void updateTaskState(Task task, String status) {
        if (task != null && status != null && !status.isBlank()) {
            task.setStatus(status);
        }
    }

    public void moveToNextTask(WorkflowInstance instance) {
        if (instance == null || instance.getWorkflow() == null) {
            return;
        }

        int currentTaskId = instance.getCurrentTask();
        List<Integer> nextTaskIds = instance.getWorkflow().getNextTasks(currentTaskId);

        if (nextTaskIds.isEmpty()) {
            instance.setStatus(WorkflowInstance.STATE_COMPLETED);
            instance.addHistory("No next task from " + currentTaskId + ". Workflow marked COMPLETED");
            return;
        }

        int nextTaskId = nextTaskIds.get(0);
        instance.setCurrentTask(nextTaskId);
        instance.addHistory("Transitioned from task " + currentTaskId + " to task " + nextTaskId);
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
                if (TASK_STATUS_PENDING.equalsIgnoreCase(task.getStatus())) {
                    updateTaskState(task, TASK_STATUS_PENDING);
                }
                return task;
            }
        }

        return null;
    }
}
