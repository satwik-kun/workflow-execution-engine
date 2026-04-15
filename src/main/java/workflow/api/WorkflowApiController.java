package workflow.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;
import workflow.services.TaskFactory;
import workflow.services.WorkflowRuntimeService;

@RestController
@RequestMapping("/api")
public class WorkflowApiController {
    private final WorkflowRuntimeService workflowRuntimeService;
    private final TaskFactory taskFactory;

    public WorkflowApiController(WorkflowRuntimeService workflowRuntimeService, TaskFactory taskFactory) {
        this.workflowRuntimeService = workflowRuntimeService;
        this.taskFactory = taskFactory;
    }

    @PostMapping("/workflows")
    public WorkflowCreatedResponse createWorkflow(@Valid @RequestBody CreateWorkflowRequest request, Authentication authentication) {
        ensureHasAnyRole(authentication, "EMPLOYEE", "MANAGER");

        List<Task> tasks = new ArrayList<>();
        for (TaskRequest taskRequest : request.tasks()) {
            tasks.add(taskFactory.createPendingTask(taskRequest.taskId(), taskRequest.taskName(), taskRequest.assignedRole()));
        }

        List<WorkflowRuntimeService.TransitionEdge> edges = new ArrayList<>();
        for (TransitionRequest transitionRequest : request.transitions()) {
            edges.add(new WorkflowRuntimeService.TransitionEdge(
                transitionRequest.fromTaskId(),
                transitionRequest.toTaskId()
            ));
        }

        Workflow workflow = workflowRuntimeService.createWorkflow(
            request.workflowName(),
            tasks,
            WorkflowRuntimeService.toTransitions(edges)
        );

        return new WorkflowCreatedResponse(workflow.getWorkflowId(), workflow.getWorkflowName());
    }

    @PostMapping("/workflows/{workflowId}/instances")
    public WorkflowInstanceResponse startInstance(@PathVariable int workflowId, Authentication authentication) {
        ensureHasAnyRole(authentication, "EMPLOYEE", "MANAGER");
        return toResponse(workflowRuntimeService.startInstance(workflowId));
    }

    @PostMapping("/instances/{instanceId}/execute")
    public WorkflowInstanceResponse executeTask(@PathVariable int instanceId, Authentication authentication) {
        ensureActorMatchesCurrentTask(authentication, instanceId, "execute");
        return toResponse(workflowRuntimeService.executeCurrentTask(instanceId));
    }

    @PostMapping("/instances/{instanceId}/retry")
    public WorkflowInstanceResponse retryTask(@PathVariable int instanceId, Authentication authentication) {
        ensureActorMatchesCurrentTask(authentication, instanceId, "retry");
        return toResponse(workflowRuntimeService.retryCurrentTask(instanceId));
    }

    @PostMapping("/instances/{instanceId}/approve")
    public WorkflowInstanceResponse approveTask(@PathVariable int instanceId, Authentication authentication) {
        ensureHasAnyRole(authentication, "MANAGER");
        return toResponse(workflowRuntimeService.approveCurrentTask(instanceId));
    }

    @PostMapping("/instances/{instanceId}/reject")
    public WorkflowInstanceResponse rejectTask(@PathVariable int instanceId, Authentication authentication) {
        ensureHasAnyRole(authentication, "MANAGER");
        return toResponse(workflowRuntimeService.rejectCurrentTask(instanceId));
    }

    @GetMapping("/instances/{instanceId}")
    public WorkflowInstanceResponse getInstance(@PathVariable int instanceId) {
        return toResponse(workflowRuntimeService.getInstance(instanceId));
    }

    private WorkflowInstanceResponse toResponse(WorkflowInstance instance) {
        List<TaskStatusResponse> tasks = new ArrayList<>();
        for (Task task : instance.getWorkflow().getTasks()) {
            tasks.add(new TaskStatusResponse(task.getTaskId(), task.getTaskName(), task.getAssignedRole(), task.getStatus()));
        }

        return new WorkflowInstanceResponse(
            instance.getInstanceId(),
            instance.getWorkflow().getWorkflowId(),
            instance.getWorkflow().getWorkflowName(),
            instance.getState(),
            instance.getCurrentTask(),
            instance.getRetryCount(),
            instance.getLastFailureDetails(),
            tasks,
            instance.getHistory()
        );
    }

    private void ensureHasAnyRole(Authentication authentication, String... allowedRoles) {
        Set<String> actorRoles = extractActorRoles(authentication);
        for (String allowedRole : allowedRoles) {
            if (actorRoles.contains(normalizeRole(allowedRole))) {
                return;
            }
        }

        throw new IllegalArgumentException(
            "Access denied for this action. Required roles: " + String.join(", ", allowedRoles)
        );
    }

    private void ensureActorMatchesCurrentTask(Authentication authentication, int instanceId, String operation) {
        WorkflowInstance instance = workflowRuntimeService.getInstance(instanceId);
        Task currentTask = instance.getWorkflow().getTaskById(instance.getCurrentTask());
        if (currentTask == null) {
            throw new IllegalArgumentException("No active task found to " + operation + " for instance " + instanceId);
        }

        String requiredRole = normalizeRole(currentTask.getAssignedRole());
        Set<String> actorRoles = extractActorRoles(authentication);
        if (actorRoles.contains(requiredRole)) {
            return;
        }

        throw new IllegalArgumentException(
            "Role mismatch for task execution. Task "
                + currentTask.getTaskId()
                + " requires "
                + requiredRole
                + " role."
        );
    }

    private Set<String> extractActorRoles(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        return authentication.getAuthorities()
            .stream()
            .map(authority -> normalizeRole(authority.getAuthority().replace("ROLE_", "")))
            .collect(Collectors.toSet());
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }

    public record CreateWorkflowRequest(
        @NotBlank(message = "workflowName is required") String workflowName,
        @NotEmpty(message = "tasks cannot be empty") List<@Valid TaskRequest> tasks,
        @NotEmpty(message = "transitions cannot be empty") List<@Valid TransitionRequest> transitions
    ) {
    }

    public record TaskRequest(
        @Min(value = 1, message = "taskId must be >= 1") int taskId,
        @NotBlank(message = "taskName is required") String taskName,
        @NotBlank(message = "assignedRole is required") String assignedRole
    ) {
    }

    public record TransitionRequest(
        @Min(value = 1, message = "fromTaskId must be >= 1") int fromTaskId,
        @Min(value = 1, message = "toTaskId must be >= 1") int toTaskId
    ) {
    }

    public record WorkflowCreatedResponse(int workflowId, String workflowName) {
    }

    public record WorkflowInstanceResponse(
        int instanceId,
        int workflowId,
        String workflowName,
        String state,
        int currentTaskId,
        int retryCount,
        String lastFailureDetails,
        List<TaskStatusResponse> tasks,
        List<String> history
    ) {
    }

    public record TaskStatusResponse(int taskId, String taskName, String assignedRole, String status) {
    }
}
