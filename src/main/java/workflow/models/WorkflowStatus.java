package workflow.models;

import java.util.Locale;

public enum WorkflowStatus {
    CREATED,
    RUNNING,
    COMPLETED,
    FAILED;

    public static WorkflowStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Workflow status is required");
        }
        return WorkflowStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}