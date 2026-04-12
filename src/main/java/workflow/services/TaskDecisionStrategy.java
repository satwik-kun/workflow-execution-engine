package workflow.services;

import workflow.models.Task;
import workflow.models.WorkflowInstance;

public interface TaskDecisionStrategy {
    String decision();

    void apply(WorkflowInstance instance, Task currentTask, ExecutionService executionService);
}