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
        System.out.println("Workflow: " + instance.getWorkflow().getWorkflowName() + " (ID: " + instance.getWorkflow().getWorkflowId() + ")");
        System.out.println("Instance ID: " + instance.getInstanceId());
        System.out.println("Current State: " + instance.getState());
        System.out.println("Current Task: " + formatCurrentTask(instance));
        System.out.println("Retry Count: " + instance.getRetryCount());
        if (!instance.getLastFailureDetails().isBlank()) {
            System.out.println("Last Failure: " + instance.getLastFailureDetails());
        }

        System.out.println("Execution History:");
        int eventNumber = 1;
        for (String event : instance.getHistory()) {
            System.out.println(eventNumber + ". " + event);
            eventNumber++;
        }
    }

    private String formatCurrentTask(WorkflowInstance instance) {
        int currentTaskId = instance.getCurrentTask();
        if (currentTaskId < 0) {
            return "N/A";
        }

        for (var task : instance.getWorkflow().getTasks()) {
            if (task.getTaskId() == currentTaskId) {
                return task.getTaskId() + " - " + task.getTaskName();
            }
        }

        return String.valueOf(currentTaskId);
    }
}
