package workflow.database;

public class DatabaseManager {
    public boolean connect(String connectionString) {
        return connectionString != null && !connectionString.isBlank();
    }
}
