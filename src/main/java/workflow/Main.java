package workflow;

import workflow.controllers.ExecutionController;
import workflow.controllers.WorkflowController;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;
import workflow.services.ExecutionService;
import workflow.services.ValidationService;

public class Main {
    public static void main(String[] args) {
        WorkflowController workflowController = new WorkflowController();
        ValidationService validationService = new ValidationService();
        ExecutionService executionService = new ExecutionService();
        ExecutionController executionController = new ExecutionController(executionService);

        Workflow workflow = workflowController.createWorkflow(1, "Sample Workflow");
        workflow.addTask(new Task(1, "Receive Request", "REQUESTER", "PENDING"));
        workflow.addTask(new Task(2, "Manager Approval", "MANAGER", "PENDING"));
        workflow.addTransition(1, 2);

        if (validationService.validateWorkflow(workflow)) {
            WorkflowInstance instance = new WorkflowInstance("inst-1", String.valueOf(workflow.getWorkflowId()), "PENDING");
            executionController.startExecution(instance);
            System.out.println("Workflow status: " + instance.getStatus());
        } else {
            System.out.println("Invalid workflow definition");
        }
    }
}
