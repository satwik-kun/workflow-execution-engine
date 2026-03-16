# workflow-execution-engine

Java-based workflow execution engine scaffold with layered structure for models, controllers, services, and database access.

## Project Structure

src/main/java/workflow

- models: Domain entities like Workflow, Task, and WorkflowInstance
- controllers: Request/flow handlers
- services: Business logic (validation, execution, retry)
- database: Data access manager
- Main.java: Entry point

## Requirements

- Java 17+
- Maven 3.8+

## Run

Compile:

```bash
mvn compile
```

Run:

```bash
mvn exec:java -Dexec.mainClass="workflow.Main"
```
