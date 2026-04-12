package workflow.services;

import org.springframework.stereotype.Component;
import workflow.models.Task;
import workflow.models.TaskStatus;
import workflow.models.WorkflowInstance;

@Component
public class ApproveTaskDecisionStrategy implements TaskDecisionStrategy {
    @Override
    public String decision() {
        return "APPROVE";
    }

    @Override
    public void apply(WorkflowInstance instance, Task currentTask, ExecutionService executionService) {
        currentTask.setStatus(TaskStatus.APPROVED);
        instance.addHistory("Approval decision recorded: task " + currentTask.getTaskId() + " approved");
        executionService.moveToNextTask(instance);
    }
}