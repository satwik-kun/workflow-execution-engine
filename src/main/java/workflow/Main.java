package workflow;

import java.util.List;
import java.util.Random;

import workflow.controllers.ApprovalController;
import workflow.controllers.ExecutionController;
import workflow.controllers.WorkflowController;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;
import workflow.services.ExecutionService;
import workflow.services.RetryService;
import workflow.services.ValidationService;

public class Main {
    private static final int APPROVAL_TASK_ID = 2;

    public static void main(String[] args) {
        WorkflowController workflowController = new WorkflowController();
        ValidationService validationService = new ValidationService();
        ExecutionService executionService = new ExecutionService(createScriptedOutcomes());
        ExecutionController executionController = new ExecutionController(executionService);
        ApprovalController approvalController = new ApprovalController(executionService);
        RetryService retryService = new RetryService(executionService);

        System.out.println("Step 1: create workflow");
        Workflow workflow = workflowController.createWorkflow(1001, "Purchase Request Workflow");

        System.out.println("Step 2: add tasks");
        workflow.addTask(new Task(1, "Submit Request", "EMPLOYEE", "PENDING"));
        workflow.addTask(new Task(APPROVAL_TASK_ID, "Manager Approval", "MANAGER", "PENDING"));
        workflow.addTask(new Task(3, "Fulfill Request", "OPERATIONS", "PENDING"));

        System.out.println("Step 3: define transitions");
        workflow.addTransition(1, APPROVAL_TASK_ID);
        workflow.addTransition(APPROVAL_TASK_ID, 3);

        System.out.println("Step 4: validate workflow");
        boolean isValid = validationService.validateWorkflow(workflow);
        System.out.println("Workflow valid: " + isValid);
        if (!isValid) {
            return;
        }

        System.out.println("Step 5: start workflow instance");
        WorkflowInstance instance = workflowController.startWorkflow(workflow);
        executionController.startExecution(instance);

        System.out.println("Step 6: execute tasks");
        boolean firstTaskSucceeded = executionService.executeTask(instance);
        if (!firstTaskSucceeded) {
            System.out.println("Initial task execution failed. Invoking retry logic.");
            retryService.handleFailure(instance);
        }

        System.out.println("Step 7: simulate approval");
        if (WorkflowInstance.STATE_RUNNING.equals(instance.getState())
            && instance.getCurrentTask() == APPROVAL_TASK_ID) {
            approvalController.approveTask(instance);
        } else {
            System.out.println("Approval step skipped because workflow is no longer awaiting approval.");
        }

        if (WorkflowInstance.STATE_RUNNING.equals(instance.getState())) {
            boolean finalTaskSucceeded = executionService.executeTask(instance);
            if (!finalTaskSucceeded) {
                System.out.println("Final task failed. Invoking retry logic.");
                retryService.handleFailure(instance);
            }
        }

        System.out.println("Step 8: print workflow status");
        workflowController.viewWorkflowStatus(instance);
        printTaskStatuses(workflow.getTasks());
    }

    private static void printTaskStatuses(List<Task> tasks) {
        System.out.println("Task Status Summary:");
        for (Task task : tasks) {
            System.out.println(
                "- Task " + task.getTaskId() + " (" + task.getTaskName() + ") -> " + task.getStatus()
            );
        }
    }

    private static Random createScriptedOutcomes() {
        return new Random() {
            private final boolean[] outcomes = {false, true, true};
            private int index = 0;

            @Override
            public boolean nextBoolean() {
                boolean result = outcomes[Math.min(index, outcomes.length - 1)];
                index++;
                return result;
            }
        };
    }
}
