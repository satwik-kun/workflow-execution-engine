package workflow.services;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import workflow.models.Task;
import workflow.models.Workflow;

@Service
public class ValidationService {
    private static final Set<String> ALLOWED_ROLES = Set.of("EMPLOYEE", "MANAGER", "OPERATIONS");

    public boolean validateWorkflow(Workflow workflow) {
        if (workflow == null) {
            throw new ValidationException("workflow cannot be null");
        }

        List<Task> tasks = workflow.getTasks();
        if (tasks.isEmpty()) {
            throw new ValidationException("workflow must contain at least one task");
        }

        Set<Integer> validTaskIds = new HashSet<>();
        int lastTaskId = Integer.MIN_VALUE;
        for (Task task : tasks) {
            validTaskIds.add(task.getTaskId());
            if (task.getTaskId() > lastTaskId) {
                lastTaskId = task.getTaskId();
            }

            if (task.getAssignedRole() == null || task.getAssignedRole().isBlank()) {
                throw new ValidationException("task " + task.getTaskId() + " has no assigned role");
            }

            String normalizedRole = task.getAssignedRole().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_ROLES.contains(normalizedRole)) {
                throw new ValidationException(
                    "task " + task.getTaskId() + " has unsupported role " + task.getAssignedRole()
                        + ". Allowed roles: " + ALLOWED_ROLES
                );
            }
        }

        Map<Integer, List<Integer>> transitions = workflow.getTransitions();
        for (Map.Entry<Integer, List<Integer>> entry : transitions.entrySet()) {
            Integer fromTaskId = entry.getKey();
            if (!validTaskIds.contains(fromTaskId)) {
                throw new ValidationException("transition source taskId " + fromTaskId + " is invalid");
            }

            for (Integer toTaskId : entry.getValue()) {
                if (!validTaskIds.contains(toTaskId)) {
                    throw new ValidationException("transition target taskId " + toTaskId + " is invalid");
                }
            }
        }

        for (Task task : tasks) {
            int taskId = task.getTaskId();
            boolean hasOutgoing = transitions.containsKey(taskId)
                && !transitions.get(taskId).isEmpty();
            if (!hasOutgoing && taskId != lastTaskId) {
                throw new ValidationException("dead task detected for taskId " + taskId);
            }
        }

        return true;
    }
}
