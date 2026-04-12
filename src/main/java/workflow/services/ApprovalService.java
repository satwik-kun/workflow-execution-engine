package workflow.services;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import workflow.models.Task;
import workflow.models.WorkflowInstance;

@Service
public class ApprovalService {
    private final ExecutionService executionService;
    private final Map<String, TaskDecisionStrategy> strategies;

    @Autowired
    public ApprovalService(ExecutionService executionService, List<TaskDecisionStrategy> strategyList) {
        this.executionService = executionService;
        this.strategies = new HashMap<>();
        for (TaskDecisionStrategy strategy : strategyList) {
            this.strategies.put(normalizeDecision(strategy.decision()), strategy);
        }
    }

    public ApprovalService(List<TaskDecisionStrategy> strategyList) {
        this(new ExecutionService(false), strategyList);
    }

    public void approveTask(WorkflowInstance instance) {
        applyDecision("APPROVE", instance);
    }

    public void rejectTask(WorkflowInstance instance) {
        applyDecision("REJECT", instance);
    }

    public void applyDecision(String decision, WorkflowInstance instance) {
        Task currentTask = getCurrentTask(instance);
        if (currentTask == null) {
            return;
        }

        TaskDecisionStrategy strategy = strategies.get(normalizeDecision(decision));
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported decision: " + decision);
        }

        strategy.apply(instance, currentTask, executionService);
    }

    private Task getCurrentTask(WorkflowInstance instance) {
        if (instance == null || instance.getWorkflow() == null) {
            return null;
        }

        Task currentTask = instance.getWorkflow().getTaskById(instance.getCurrentTask());
        if (currentTask == null) {
            instance.addHistory("Approval requested with no active task.");
        }

        return currentTask;
    }

    private String normalizeDecision(String decision) {
        return decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
    }
}