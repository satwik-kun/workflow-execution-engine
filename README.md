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

Maven is optional because this repository includes Maven Wrapper.

## Run

Compile (recommended via Maven Wrapper):

```bash
./mvnw compile
```

Run:

```bash
./mvnw exec:java -Dexec.mainClass="workflow.Main"
```

On Windows PowerShell, use:

```powershell
.\mvnw.cmd compile
.\mvnw.cmd exec:java "-Dexec.mainClass=workflow.Main"
```
