package workflow.services;

import org.springframework.stereotype.Component;
import workflow.models.Task;
import workflow.models.TaskStatus;
import workflow.models.WorkflowInstance;
import workflow.models.WorkflowStatus;

@Component
public class RejectTaskDecisionStrategy implements TaskDecisionStrategy {
    @Override
    public String decision() {
        return "REJECT";
    }

    @Override
    public void apply(WorkflowInstance instance, Task currentTask, ExecutionService executionService) {
        currentTask.setStatus(TaskStatus.REJECTED);
        instance.setStatus(WorkflowStatus.FAILED);
        instance.addHistory("Approval decision recorded: task " + currentTask.getTaskId() + " rejected");
        instance.addHistory("Workflow stopped after rejection.");
    }
}