# workflow-execution-engine

Workflow execution engine with:

- REST endpoints for workflow lifecycle operations
- persisted workflow instances and execution history (H2 database)
- unit tests for execution and retry logic
- request validation with structured API errors
- deterministic demo mode for stable presentations
- API integration tests for end-to-end flow
- OpenAPI docs and health endpoint
- Flyway-managed schema migrations
- HTTP Basic auth with role-based endpoint protection

## Project Structure

src/main/java/workflow

- models: Domain entities like Workflow, Task, and WorkflowInstance
- controllers: Request/flow handlers
- services: Business logic (validation, execution, retry)
- api: REST controller + API exception handling
- persistence: JPA entities and repositories for instances/history
- Main.java: Demo entry point (console flow)
- WorkflowApiApplication.java: Spring Boot REST entry point

src/test/java/workflow

- unit: fast isolated unit tests
- integration: API integration tests
- system: end-to-end persistence-backed system tests

scripts

- demo-run.ps1: automated demo runner script
- check-requirements.ps1: validates local tooling requirements only
- team-demo.ps1: team launcher (checks requirements, starts backend/UI, runs demo)

ui

- React + Vite frontend for workflow control and monitoring

## Team Quick Start

Read [REQUIREMENTS.md](REQUIREMENTS.md) and run:

```powershell
.\scripts\check-requirements.ps1
```

Then run:

```powershell
.\scripts\team-demo.ps1
```

This will verify dependencies, start backend/UI if needed, and run a full API walkthrough.

## Requirements

- Java 25+

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

Run automated API demo script:

```powershell
.\scripts\demo-run.ps1
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

## Run UI

In a separate terminal:

```bash
cd ui
npm install
npm run dev
```

Open `http://localhost:5173`.

Base URL: `http://localhost:8080/api`

H2 console: `http://localhost:8080/h2-console`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Health endpoint: `http://localhost:8080/actuator/health`

JDBC URL: `jdbc:h2:file:./data/workflow-db`

Demo behavior is deterministic by default via `workflow.execution.demo-mode=true` in [src/main/resources/application.properties](src/main/resources/application.properties).

Set it to `false` to switch back to random execution behavior.

Default API credentials:

- manager / manager123
- employee / employee123
- operations / operations123

## API Endpoints

All `/api/**` endpoints require authentication.

Manager-only endpoints:

- `POST /api/instances/{instanceId}/approve`
- `POST /api/instances/{instanceId}/reject`

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

Run unit tests only:

```bash
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Run full test pyramid (unit + integration + system):

```bash
./mvnw verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd verify
```
