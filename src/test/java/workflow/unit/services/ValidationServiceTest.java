package workflow.unit.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.services.ValidationException;
import workflow.services.ValidationService;

class ValidationServiceTest {
    private final ValidationService validationService = new ValidationService();

    @Test
    void validateWorkflow_withValidDefinition_returnsTrue() {
        Workflow workflow = new Workflow(1, "Valid Workflow");
        workflow.addTask(new Task(1, "Submit", "EMPLOYEE", "PENDING"));
        workflow.addTask(new Task(2, "Approve", "MANAGER", "PENDING"));
        workflow.addTransition(1, 2);

        boolean result = validationService.validateWorkflow(workflow);

        assertTrue(result);
    }

    @Test
    void validateWorkflow_withDeadTask_throwsValidationException() {
        Workflow workflow = new Workflow(1, "Dead Task Workflow");
        workflow.addTask(new Task(1, "Submit", "EMPLOYEE", "PENDING"));
        workflow.addTask(new Task(2, "Approve", "MANAGER", "PENDING"));
        workflow.addTask(new Task(3, "Fulfill", "OPS", "PENDING"));
        workflow.addTransition(2, 3);

        assertThrows(ValidationException.class, () -> validationService.validateWorkflow(workflow));
    }

    @Test
    void validateWorkflow_withInvalidTransitionTarget_throwsValidationException() {
        Workflow workflow = new Workflow(1, "Invalid Transition Workflow");
        workflow.addTask(new Task(1, "Submit", "EMPLOYEE", "PENDING"));
        workflow.addTask(new Task(2, "Approve", "MANAGER", "PENDING"));
        workflow.addTransition(1, 99);

        assertThrows(ValidationException.class, () -> validationService.validateWorkflow(workflow));
    }
}
