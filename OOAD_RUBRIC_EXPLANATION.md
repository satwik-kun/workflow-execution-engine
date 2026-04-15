# OOAD Rubric Explanation for Workflow Execution Engine

This document explains what you should present to the teacher based on the codebase. It is written to match the rubric directly and to help you explain the project clearly during review, demo, or viva.

## 1. What the Project Is

This project is a workflow execution engine. It lets a user:

- create a workflow definition
- start a workflow instance
- execute the current task
- approve or reject manager tasks
- retry failed tasks
- view workflow state and execution history
- persist workflow data and history in H2
- use the REST API or the React UI

The core domain objects are `Workflow`, `Task`, and `WorkflowInstance`. The runtime behavior is managed by services such as `WorkflowRuntimeService`, `ExecutionService`, `RetryService`, `ApprovalService`, and `ValidationService`.

## 2. What You Need To Explain From The Rubric

### A. Analysis and Design Models

You already have the UML diagrams, so your job is to explain them in relation to the code.

#### 1. Use Case Diagram

Explain the system actions and actors.

Actors in this project:

- employee
- manager
- operations user
- admin or API user, if you present the REST side

Main use cases:

- create workflow
- start workflow instance
- execute task
- approve task
- reject task
- retry failed task
- view workflow instance status

What to say:

- The workflow engine supports the full lifecycle of a workflow instance.
- Different roles have different permissions, especially for approval and rejection.
- The system validates input before execution.

#### 2. Class Diagram

Explain how the main classes are organized.

Important classes:

- `Workflow` stores tasks and transitions.
- `Task` stores task metadata and task status.
- `WorkflowInstance` stores runtime state, current task, retry count, and history.
- `WorkflowStatus` and `TaskStatus` define valid states.
- `WorkflowApiController` exposes REST endpoints.
- `WorkflowRuntimeService` coordinates workflow creation and instance operations.
- `ExecutionService` executes tasks and advances the workflow.
- `RetryService` handles retry logic.
- `ApprovalService` handles approve/reject decisions.
- `ValidationService` checks workflow correctness.
- `TaskFactory` creates task objects consistently.
- persistence repositories store workflow definitions, instances, and history.

What to say:

- The class diagram shows separation between domain, service, controller, and persistence layers.
- The system follows a layered architecture instead of putting all logic in one class.

#### 3. Activity Diagram

Explain the flow of work, not just the class structure.

Recommended flows to describe:

- create workflow
- start workflow instance
- execute task and move to the next task
- approve or reject manager task
- retry failed task until success or retry limit

What to say:

- The activity diagram should show decision points like success vs failure and approve vs reject.
- It should show validation before persistence and execution.
- It should show how the system records history at each step.

#### 4. State Diagram

Explain the lifecycle of the workflow instance and the task states.

Workflow instance states in code:

- CREATED
- RUNNING
- COMPLETED
- FAILED

Task states in code:

- PENDING
- SUCCESS
- FAILURE
- APPROVED
- REJECTED

What to say:

- A workflow instance starts in CREATED and moves to RUNNING when started.
- It becomes COMPLETED when the last task finishes successfully.
- It becomes FAILED when the retry limit is reached or rejection ends the workflow.
- Individual tasks also have their own state transitions.

### B. MVC Architecture Pattern

This project clearly uses MVC, and you should explain each part with examples.

#### Model

The model is the data and domain logic.

Examples:

- `Workflow`
- `Task`
- `WorkflowInstance`
- `WorkflowStatus`
- `TaskStatus`

What to say:

- These classes represent workflow data and state.
- `WorkflowInstance` stores runtime details like current task, retry count, and history.

#### View

The view is what the user sees.

Examples:

- the React UI in `ui/`
- JSON responses from `WorkflowApiController`

What to say:

- The frontend displays workflow state, task list, and history.
- For API testing or demo, the response payload itself is also part of the visible presentation layer.

#### Controller

The controller receives requests and delegates to services.

Examples:

- `workflow.api.WorkflowApiController`
- `workflow.controllers.WorkflowController`
- `workflow.controllers.ApprovalController`
- `workflow.controllers.ExecutionController`

What to say:

- Controllers do not contain heavy business logic.
- They pass requests to service classes and return results.

#### Service Layer

The services implement business rules.

Examples:

- `WorkflowRuntimeService`
- `ExecutionService`
- `RetryService`
- `ApprovalService`
- `ValidationService`

What to say:

- Business behavior is centralized in services.
- This keeps controllers thin and keeps the code easier to test.

#### Persistence Layer

The persistence layer stores data in the database.

Examples:

- `WorkflowDefinitionRepository`
- `WorkflowInstanceRepository`
- `WorkflowHistoryRepository`
- JPA record classes in `workflow.persistence`

What to say:

- Workflow definitions, instances, and history are stored separately.
- This makes execution state persistent rather than temporary.

### C. Design Principles and Patterns

The rubric says at least one principle or pattern per team member. You should be ready to explain several, then assign one or more to each teammate.

#### 1. Factory Pattern

Class: `TaskFactory`

What it does:

- creates `Task` objects in a consistent way
- provides helper methods such as `createPendingTask` and `createTask`

What to say:

- Object creation is centralized.
- If task creation rules change, the change is made in one place.

#### 2. Strategy Pattern

Class: `ApprovalService` with `TaskDecisionStrategy`, `ApproveTaskDecisionStrategy`, and `RejectTaskDecisionStrategy`

What it does:

- selects the approval or rejection behavior at runtime
- keeps approve and reject logic separate

What to say:

- Different decisions are handled by separate strategy classes.
- This makes it easy to add new decisions later without rewriting the service.

#### 3. Repository Pattern

Classes:

- `WorkflowDefinitionRepository`
- `WorkflowInstanceRepository`
- `WorkflowHistoryRepository`

What it does:

- isolates database operations from business logic

What to say:

- Services do not directly write SQL.
- Repositories abstract persistence and improve maintainability.

#### 4. Dependency Injection

Where it appears:

- constructors in `WorkflowRuntimeService`, `ApprovalService`, `ExecutionService`, and controllers

What to say:

- Dependencies are passed into classes instead of being created everywhere inside the code.
- This improves testability and reduces coupling.

#### 5. Single Responsibility Principle

How the code follows it:

- `ValidationService` validates workflows
- `ExecutionService` executes tasks
- `RetryService` handles retry behavior
- `ApprovalService` handles approval decisions
- `TaskFactory` handles task creation

What to say:

- Each class has a focused job.
- This prevents one large class from managing validation, execution, retry, and persistence all at once.

#### 6. Open/Closed Principle

How the code follows it:

- new decision behavior can be added through new `TaskDecisionStrategy` implementations
- existing service code does not need major changes for new decision types

What to say:

- The design is open for extension and closed for modification.
- The strategy pattern is the main reason this works well.

#### 7. Encapsulation

How the code follows it:

- fields are private inside model classes
- state changes happen through methods like `setCurrentTask`, `setStatus`, `incrementRetryCount`, and `addHistory`

What to say:

- The object controls how its state changes.
- External code does not manipulate fields directly.

#### 8. Separation of Concerns

How the code follows it:

- models hold data
- controllers handle requests
- services handle business logic
- repositories handle persistence
- UI handles presentation

What to say:

- Each layer has a clear responsibility.
- That makes the system easier to test and explain.

### D. State and Workflow Logic You Should Explain

This is one of the most important parts of the viva because it shows you understand the behavior of the system.

#### Workflow Instance Lifecycle

Explain this sequence:

1. A workflow definition is created.
2. A workflow instance is started.
3. The instance moves to RUNNING.
4. The current task is executed.
5. If execution succeeds, the system moves to the next task.
6. If the final task succeeds, the instance becomes COMPLETED.
7. If execution fails, retry logic runs.
8. If retry limit is exceeded, the instance becomes FAILED.

#### Task Handling

Explain this sequence:

- each task begins as PENDING
- execution changes it to SUCCESS or FAILURE
- approval changes it to APPROVED
- rejection changes it to REJECTED

#### Validation

The code validates workflows before execution.

Important rules in `ValidationService` and `WorkflowRuntimeService`:

- workflow cannot be null
- workflow must contain at least one task
- each task must have an assigned role
- transitions must reference valid task IDs
- dead tasks are rejected
- duplicate task IDs are rejected
- self-loop transitions are rejected

What to say:

- The system protects itself from invalid workflow definitions.
- Validation is done before runtime execution and before saving the workflow.

#### Retry Logic

`RetryService` retries failed tasks up to 3 times.

What to say:

- retry count is tracked in the workflow instance
- the service retries only when the current task is in FAILURE state
- if a retry succeeds, the workflow can continue
- if the retry limit is reached, the workflow is marked FAILED

#### Approval Logic

`ApprovalService` uses strategy classes to approve or reject a manager task.

What to say:

- only manager tasks can be approved or rejected
- a task must be PENDING before approval or rejection
- the strategy pattern handles the actual behavior

### E. What To Show In The Demo

If the teacher asks for a live demo, the cleanest walkthrough is this:

1. Create a workflow.
2. Start a workflow instance.
3. Execute the first task.
4. Approve or reject the manager task.
5. Retry a failed task if needed.
6. Fetch instance state and show history.
7. Show that the data is persisted.

Good evidence to mention:

- REST API endpoints in `WorkflowApiController`
- response state changing from RUNNING to COMPLETED or FAILED
- history entries being added after each step
- repository-backed persistence
- validation errors for bad input

### F. What To Say About Testing

The codebase already has three useful test levels.

- unit tests for service and model behavior
- integration tests for API behavior
- system tests for full persistence-backed flows

What to say:

- Unit tests prove small pieces like task creation, validation, retry, and enum handling.
- Integration tests prove the API endpoints and error handling.
- System tests prove the full workflow from request to persistence.

### G. What To Say About Non-Functional Features

These are not the main rubric focus, but they help you present the project as complete.

- Authentication and role-based protection are included.
- OpenAPI documentation is available.
- Actuator health endpoint is available.
- H2 database and Flyway migrations are used.
- The demo mode is deterministic for presentations.

## 3. Short Teacher-Facing Explanation You Can Memorize

You can say this in a viva:

"This project is a workflow execution engine built using the MVC pattern. The model layer contains Workflow, Task, and WorkflowInstance. The controller layer receives API requests and delegates to services. The service layer contains the main business rules for validation, execution, retry, and approval. The persistence layer stores workflow definitions, instances, and history.

For design patterns, we use Factory for task creation, Strategy for approval and rejection decisions, and Repository for database access. The system follows SOLID ideas like single responsibility, encapsulation, and separation of concerns. The workflow instance moves through states such as CREATED, RUNNING, COMPLETED, and FAILED, while tasks move through PENDING, SUCCESS, FAILURE, APPROVED, and REJECTED. The API, UI, persistence, and tests all support this lifecycle."

## 4. Team Split Suggestion

If the teacher expects each team member to explain one principle or pattern, split it like this:

- Member 1: Factory Pattern and Task creation
- Member 2: Strategy Pattern and approval/rejection flow
- Member 3: Repository Pattern and persistence
- Member 4: MVC and separation of layers
- Member 5: Validation, retry, and workflow state transitions

If your team size is smaller, combine two items per person.

## 5. Final Checklist Before Submission

- UML diagrams are ready and match the code
- every pattern you claim can be pointed to in code
- every principle you claim can be justified with an example class
- you can explain workflow lifecycle and state changes
- you can show validation, retry, approval, and persistence in the demo
- you can explain how MVC is implemented in this project

## 6. Best Files To Open During Viva

- `src/main/java/workflow/api/WorkflowApiController.java`
- `src/main/java/workflow/services/WorkflowRuntimeService.java`
- `src/main/java/workflow/services/ExecutionService.java`
- `src/main/java/workflow/services/RetryService.java`
- `src/main/java/workflow/services/ApprovalService.java`
- `src/main/java/workflow/services/ValidationService.java`
- `src/main/java/workflow/services/TaskFactory.java`
- `src/main/java/workflow/models/Workflow.java`
- `src/main/java/workflow/models/Task.java`
- `src/main/java/workflow/models/WorkflowInstance.java`
- `src/main/java/workflow/models/WorkflowStatus.java`
- `src/main/java/workflow/models/TaskStatus.java`

This is enough to explain the rubric in a way that is directly tied to the implementation.