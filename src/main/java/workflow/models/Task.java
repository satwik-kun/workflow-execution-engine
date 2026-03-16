package workflow.models;

public class Task {
    private final String id;
    private final String name;
    private final boolean requiresApproval;

    public Task(String id, String name, boolean requiresApproval) {
        this.id = id;
        this.name = name;
        this.requiresApproval = requiresApproval;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }
}
