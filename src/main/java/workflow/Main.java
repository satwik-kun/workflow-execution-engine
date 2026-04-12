package workflow;

import java.util.List;
import java.util.Random;

import workflow.controllers.ApprovalController;
import workflow.controllers.ExecutionController;
import workflow.controllers.WorkflowController;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;
import workflow.models.WorkflowStatus;
import workflow.services.ExecutionService;
import workflow.services.RetryService;
import workflow.services.TaskFactory;
import workflow.services.ValidationService;

public class Main {
    private static final int APPROVAL_TASK_ID = 2;
    private static final String SECTION_SEPARATOR = "==================================================";

    public static void main(String[] args) {
        WorkflowController workflowController = new WorkflowController();
        ValidationService validationService = new ValidationService();
        ExecutionService executionService = new ExecutionService(createScriptedOutcomes());
        ExecutionController executionController = new ExecutionController(executionService);
        ApprovalController approvalController = new ApprovalController(executionService);
        RetryService retryService = new RetryService(executionService);
        TaskFactory taskFactory = new TaskFactory();

        printSection("Workflow Execution Demo");
        System.out.println("[1/8] Creating workflow definition...");
        Workflow workflow = workflowController.createWorkflow(1001, "Purchase Request Workflow");

        System.out.println("[2/8] Adding tasks...");
        workflow.addTask(taskFactory.createPendingTask(1, "Submit Request", "EMPLOYEE"));
        workflow.addTask(taskFactory.createPendingTask(APPROVAL_TASK_ID, "Manager Approval", "MANAGER"));
        workflow.addTask(taskFactory.createPendingTask(3, "Fulfill Request", "OPERATIONS"));

        System.out.println("[3/8] Defining transitions...");
        workflow.addTransition(1, APPROVAL_TASK_ID);
        workflow.addTransition(APPROVAL_TASK_ID, 3);

        System.out.println("[4/8] Validating workflow...");
        validationService.validateWorkflow(workflow);
        System.out.println("Validation result: PASS");

        System.out.println("[5/8] Starting workflow instance...");
        WorkflowInstance instance = workflowController.startWorkflow(workflow);
        executionController.startExecution(instance);

        System.out.println("[6/8] Executing first task...");
        boolean firstTaskSucceeded = executionService.executeTask(instance);
        if (!firstTaskSucceeded) {
            System.out.println("First attempt failed. Running retry logic...");
            retryService.handleFailure(instance);
        }

        System.out.println("[7/8] Processing approval step...");
        if (instance.getWorkflowStatus() == WorkflowStatus.RUNNING
            && instance.getCurrentTask() == APPROVAL_TASK_ID) {
            approvalController.approveTask(instance);
            System.out.println("Approval completed for task " + APPROVAL_TASK_ID + ".");
        } else {
            System.out.println("Approval skipped (workflow is not awaiting manager approval).");
        }

        if (instance.getWorkflowStatus() == WorkflowStatus.RUNNING) {
            System.out.println("[8/8] Executing remaining task(s)...");
            boolean finalTaskSucceeded = executionService.executeTask(instance);
            if (!finalTaskSucceeded) {
                System.out.println("Final task failed. Running retry logic...");
                retryService.handleFailure(instance);
            }
        } else {
            System.out.println("[8/8] Remaining execution skipped (workflow already finalized).");
        }

        printSection("Execution Summary");
        workflowController.viewWorkflowStatus(instance);
        printTaskStatuses(workflow.getTasks());
        System.out.println(SECTION_SEPARATOR);
    }

    private static void printTaskStatuses(List<Task> tasks) {
        System.out.println("Task Status Summary:");
        for (Task task : tasks) {
            System.out.println(
                "- Task " + task.getTaskId() + " (" + task.getTaskName() + ") -> " + task.getStatus()
            );
        }
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println(SECTION_SEPARATOR);
        System.out.println(title);
        System.out.println(SECTION_SEPARATOR);
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
