# workflow-execution-engine

Workflow execution engine with:

- REST endpoints for workflow lifecycle operations
- persisted workflow instances and execution history (H2 database)
- unit tests for execution and retry logic
- request validation with structured API errors
- deterministic demo mode for stable presentations
- API integration tests for end-to-end flow

## Project Structure

src/main/java/workflow

- models: Domain entities like Workflow, Task, and WorkflowInstance
- controllers: Request/flow handlers
- services: Business logic (validation, execution, retry)
- api: REST controller + API exception handling
- persistence: JPA entities and repositories for instances/history
- Main.java: Demo entry point (console flow)
- WorkflowApiApplication.java: Spring Boot REST entry point

## Requirements

- Java 17+

Maven is optional because this repository includes Maven Wrapper.

## Run Demo (Console)

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

## Run REST API

Start the API server:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Base URL: `http://localhost:8080/api`

H2 console: `http://localhost:8080/h2-console`

JDBC URL: `jdbc:h2:file:./data/workflow-db`

Demo behavior is deterministic by default via `workflow.execution.demo-mode=true` in [src/main/resources/application.properties](src/main/resources/application.properties).

Set it to `false` to switch back to random execution behavior.

## API Endpoints

- `POST /api/workflows` create workflow definition
- `POST /api/workflows/{workflowId}/instances` start workflow instance
- `POST /api/instances/{instanceId}/execute` execute current task
- `POST /api/instances/{instanceId}/retry` retry current task
- `POST /api/instances/{instanceId}/approve` approve current task
- `POST /api/instances/{instanceId}/reject` reject current task
- `GET /api/instances/{instanceId}` fetch current state and history

Example payload for `POST /api/workflows`:

```json
{
	"workflowName": "Purchase Request Workflow",
	"tasks": [
		{ "taskId": 1, "taskName": "Submit Request", "assignedRole": "EMPLOYEE" },
		{ "taskId": 2, "taskName": "Manager Approval", "assignedRole": "MANAGER" },
		{ "taskId": 3, "taskName": "Fulfill Request", "assignedRole": "OPERATIONS" }
	],
	"transitions": [
		{ "fromTaskId": 1, "toTaskId": 2 },
		{ "fromTaskId": 2, "toTaskId": 3 }
	]
}
```

## Tests

Run unit + integration tests:

```bash
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```
