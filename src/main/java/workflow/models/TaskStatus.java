package workflow.models;

import java.util.Locale;

public enum TaskStatus {
    PENDING,
    SUCCESS,
    FAILURE,
    APPROVED,
    REJECTED;

    public static TaskStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Task status is required");
        }
        return TaskStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}