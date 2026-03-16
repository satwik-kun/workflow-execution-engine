package workflow.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Workflow {
    private final int workflowId;
    private final String workflowName;
    private final List<Task> tasks;
    private final Map<Integer, List<Integer>> transitions;

    public Workflow(int workflowId, String workflowName) {
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.tasks = new ArrayList<>();
        this.transitions = new HashMap<>();
    }

    public int getWorkflowId() {
        return workflowId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void addTransition(int fromTaskId, int toTaskId) {
        transitions.computeIfAbsent(fromTaskId, key -> new ArrayList<>()).add(toTaskId);
    }

    public List<Task> getTasks() {
        return new ArrayList<>(tasks);
    }

    public Task getTaskById(int taskId) {
        for (Task task : tasks) {
            if (task.getTaskId() == taskId) {
                return task;
            }
        }

        return null;
    }

    public List<Integer> getNextTasks(int taskId) {
        return new ArrayList<>(transitions.getOrDefault(taskId, new ArrayList<>()));
    }

    public Map<Integer, List<Integer>> getTransitions() {
        return new HashMap<>(transitions);
    }

    public void printWorkflow() {
        System.out.println("Workflow ID: " + workflowId + ", Name: " + workflowName);
        for (Task task : tasks) {
            System.out.println(
                "Task ID: " + task.getTaskId()
                    + ", Name: " + task.getTaskName()
                    + ", Role: " + task.getAssignedRole()
                    + ", Status: " + task.getStatus()
            );
        }
        for (Map.Entry<Integer, List<Integer>> entry : transitions.entrySet()) {
            System.out.println("Transition from Task " + entry.getKey() + " to " + entry.getValue());
        }
    }
}
