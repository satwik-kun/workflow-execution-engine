package workflow;

import workflow.controllers.WorkflowController;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;

public class Main {
    public static void main(String[] args) {
        WorkflowController workflowController = new WorkflowController();

        Workflow workflow = workflowController.createWorkflow(1, "Sample Workflow");
        workflow.addTask(new Task(1, "Receive Request", "REQUESTER", "PENDING"));
        workflow.addTask(new Task(2, "Manager Approval", "MANAGER", "PENDING"));
        workflow.addTransition(1, 2);

        WorkflowInstance instance = workflowController.startWorkflow(workflow);
        workflowController.viewWorkflowStatus(instance);
    }
}
