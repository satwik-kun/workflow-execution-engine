package workflow.services;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import workflow.models.Task;
import workflow.models.TaskStatus;
import workflow.models.WorkflowInstance;
import workflow.models.WorkflowStatus;

@Service
public class ExecutionService {
    private final Random random;
    private final boolean deterministicDemoMode;

    @Autowired
    public ExecutionService(@Value("${workflow.execution.demo-mode:false}") boolean deterministicDemoMode) {
        this(new Random(), deterministicDemoMode);
    }

    public ExecutionService(Random random) {
        this(random, false);
    }

    public ExecutionService(Random random, boolean deterministicDemoMode) {
        this.random = random;
        this.deterministicDemoMode = deterministicDemoMode;
    }

    public void execute(WorkflowInstance workflowInstance) {
        if (workflowInstance != null) {
            workflowInstance.start();
            workflowInstance.addHistory("Work order moved to RUNNING state by execution service");
        }
    }

    public boolean executeTask(WorkflowInstance instance) {
        if (instance == null || instance.getWorkflow() == null) {
            return false;
        }

        if (instance.getWorkflowStatus() == WorkflowStatus.COMPLETED) {
            instance.addHistory("Execution skipped: work order already COMPLETED");
            return false;
        }

        if (instance.getWorkflowStatus() == WorkflowStatus.FAILED) {
            instance.addHistory("Execution skipped: work order already FAILED");
            return false;
        }

        Task currentTask = identifyCurrentTask(instance);
        if (currentTask == null) {
            instance.addHistory("No executable production step found for instance " + instance.getInstanceId());
            return false;
        }

        boolean isSuccess = deterministicDemoMode
            ? evaluateDeterministicOutcome(instance, currentTask)
            : random.nextBoolean();
        TaskStatus executionStatus = isSuccess ? TaskStatus.SUCCESS : TaskStatus.FAILURE;
        updateTaskState(currentTask, executionStatus);

        instance.addHistory(
            "Team " + currentTask.getAssignedRole() + " processed step "
                + currentTask.getTaskId()
                + " ("
                + currentTask.getTaskName()
                + ") with outcome "
                + executionStatus
        );

        if (isSuccess) {
            instance.resetRetryCount();
            instance.setLastFailureDetails("");
            moveToNextTask(instance);
            return true;
        }

        instance.setStatus(WorkflowStatus.RUNNING);
        instance.setLastFailureDetails(
            "Task " + currentTask.getTaskId() + " (" + currentTask.getTaskName() + ") failed during execution"
        );
        instance.addHistory("Quality gate recorded a failure. Retry policy can continue processing.");

        return false;
    }

    public void updateTaskState(Task task, TaskStatus status) {
        if (task != null && status != null) {
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
            instance.setStatus(WorkflowStatus.COMPLETED);
            instance.addHistory("No downstream step after " + currentTaskId + ". Work order marked COMPLETED");
            return;
        }

        int nextTaskId = nextTaskIds.get(0);
        instance.setCurrentTask(nextTaskId);
        instance.addHistory("Pipeline advanced from step " + currentTaskId + " to step " + nextTaskId);
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
            instance.addHistory("Initialized first production step to " + firstTask.getTaskId());
            if (firstTask.getTaskStatus() == TaskStatus.PENDING) {
                updateTaskState(firstTask, TaskStatus.PENDING);
            }
            return firstTask;
        }

        for (Task task : tasks) {
            if (task.getTaskId() == currentTaskId) {
                if (task.getTaskStatus() == TaskStatus.PENDING) {
                    updateTaskState(task, TaskStatus.PENDING);
                }
                return task;
            }
        }

        return null;
    }

    private boolean evaluateDeterministicOutcome(WorkflowInstance instance, Task currentTask) {
        if (currentTask.getTaskId() == 1
            && currentTask.getTaskStatus() == TaskStatus.PENDING
            && instance.getRetryCount() == 0) {
            return false;
        }

        return true;
    }
}
