package workflow.unit.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import workflow.controllers.ApprovalController;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;
import workflow.models.WorkflowStatus;
import workflow.services.ExecutionService;

class ApprovalControllerTest {
    @Test
    void approveTask_shouldMarkApprovedAndMoveForward() {
        Workflow workflow = new Workflow(10, "Approval Workflow");
        workflow.addTask(new Task(1, "Submit", "EMPLOYEE", "PENDING"));
        workflow.addTask(new Task(2, "Approve", "MANAGER", "PENDING"));
        workflow.addTask(new Task(3, "Fulfill", "OPS", "PENDING"));
        workflow.addTransition(1, 2);
        workflow.addTransition(2, 3);

        WorkflowInstance instance = new WorkflowInstance(1, workflow);
        instance.start();
        instance.setCurrentTask(2);

        ApprovalController controller = new ApprovalController(new ExecutionService(true));
        controller.approveTask(instance);

        assertEquals("APPROVED", workflow.getTaskById(2).getStatus());
        assertEquals(3, instance.getCurrentTask());
        assertEquals(WorkflowStatus.RUNNING.name(), instance.getState());
        assertTrue(instance.getHistory().stream().anyMatch(h -> h.contains("approved")));
    }

    @Test
    void rejectTask_shouldMarkFailedAndStopFlow() {
        Workflow workflow = new Workflow(11, "Reject Workflow");
        workflow.addTask(new Task(1, "Submit", "EMPLOYEE", "PENDING"));
        workflow.addTask(new Task(2, "Approve", "MANAGER", "PENDING"));
        workflow.addTransition(1, 2);

        WorkflowInstance instance = new WorkflowInstance(1, workflow);
        instance.start();
        instance.setCurrentTask(2);

        ApprovalController controller = new ApprovalController(new ExecutionService(true));
        controller.rejectTask(instance);

        assertEquals("REJECTED", workflow.getTaskById(2).getStatus());
        assertEquals(WorkflowStatus.FAILED.name(), instance.getState());
        assertTrue(instance.getHistory().stream().anyMatch(h -> h.contains("rejected")));
    }
}
