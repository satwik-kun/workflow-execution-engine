package workflow.controllers;

import java.util.concurrent.atomic.AtomicInteger;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;
import workflow.services.ValidationService;

public class WorkflowController {
    private static final AtomicInteger INSTANCE_SEQUENCE = new AtomicInteger(1);
    private final ValidationService validationService;

    public WorkflowController() {
        this.validationService = new ValidationService();
    }

    public Workflow createWorkflow(int workflowId, String workflowName) {
        return new Workflow(workflowId, workflowName);
    }

    public WorkflowInstance startWorkflow(Workflow workflow) {
        if (!validationService.validateWorkflow(workflow)) {
            throw new IllegalArgumentException("Workflow validation failed. Cannot start workflow.");
        }

        WorkflowInstance instance = new WorkflowInstance(INSTANCE_SEQUENCE.getAndIncrement(), workflow);
        instance.start();

        if (!workflow.getTasks().isEmpty()) {
            int firstTaskId = workflow.getTasks().get(0).getTaskId();
            instance.setCurrentTask(firstTaskId);
            instance.addHistory("Activated first task: " + firstTaskId);
        }

        instance.addHistory(
            "Workflow instance " + instance.getInstanceId() + " started in state " + instance.getState()
        );
        return instance;
    }

    public void viewWorkflowStatus(WorkflowInstance instance) {
        System.out.println("Workflow State: " + instance.getState());
        System.out.println("Current Task: " + instance.getCurrentTask());
        System.out.println("Execution History:");
        for (String event : instance.getHistory()) {
            System.out.println("- " + event);
        }
    }
}
