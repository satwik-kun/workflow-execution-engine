package workflow.services;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import workflow.models.Task;
import workflow.models.Workflow;

public class ValidationService {
    public boolean validateWorkflow(Workflow workflow) {
        if (workflow == null) {
            System.out.println("Validation error: workflow cannot be null.");
            return false;
        }

        List<Task> tasks = workflow.getTasks();
        if (tasks.isEmpty()) {
            System.out.println("Validation error: workflow must contain at least one task.");
            return false;
        }

        Set<Integer> validTaskIds = new HashSet<>();
        int lastTaskId = Integer.MIN_VALUE;
        for (Task task : tasks) {
            validTaskIds.add(task.getTaskId());
            if (task.getTaskId() > lastTaskId) {
                lastTaskId = task.getTaskId();
            }

            if (task.getAssignedRole() == null || task.getAssignedRole().isBlank()) {
                System.out.println(
                    "Validation error: task " + task.getTaskId() + " has no assigned role."
                );
                return false;
            }
        }

        Map<Integer, List<Integer>> transitions = workflow.getTransitions();
        for (Map.Entry<Integer, List<Integer>> entry : transitions.entrySet()) {
            Integer fromTaskId = entry.getKey();
            if (!validTaskIds.contains(fromTaskId)) {
                System.out.println(
                    "Validation error: transition source taskId " + fromTaskId + " is invalid."
                );
                return false;
            }

            for (Integer toTaskId : entry.getValue()) {
                if (!validTaskIds.contains(toTaskId)) {
                    System.out.println(
                        "Validation error: transition target taskId " + toTaskId + " is invalid."
                    );
                    return false;
                }
            }
        }

        for (Task task : tasks) {
            int taskId = task.getTaskId();
            boolean hasOutgoing = transitions.containsKey(taskId)
                && !transitions.get(taskId).isEmpty();
            if (!hasOutgoing && taskId != lastTaskId) {
                System.out.println(
                    "Validation error: dead task detected for taskId " + taskId + "."
                );
                return false;
            }
        }

        return true;
    }
}
