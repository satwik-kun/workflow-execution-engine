package workflow.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import workflow.models.Task;
import workflow.models.Workflow;
import workflow.models.WorkflowInstance;
import workflow.services.WorkflowRuntimeService;

@RestController
@RequestMapping("/api")
public class WorkflowApiController {
    private final WorkflowRuntimeService workflowRuntimeService;

    public WorkflowApiController(WorkflowRuntimeService workflowRuntimeService) {
        this.workflowRuntimeService = workflowRuntimeService;
    }

    @PostMapping("/workflows")
    public WorkflowCreatedResponse createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        List<Task> tasks = new ArrayList<>();
        for (TaskRequest taskRequest : request.tasks()) {
            tasks.add(new Task(taskRequest.taskId(), taskRequest.taskName(), taskRequest.assignedRole(), "PENDING"));
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
    public WorkflowInstanceResponse startInstance(@PathVariable int workflowId) {
        return toResponse(workflowRuntimeService.startInstance(workflowId));
    }

    @PostMapping("/instances/{instanceId}/execute")
    public WorkflowInstanceResponse executeTask(@PathVariable int instanceId) {
        return toResponse(workflowRuntimeService.executeCurrentTask(instanceId));
    }

    @PostMapping("/instances/{instanceId}/retry")
    public WorkflowInstanceResponse retryTask(@PathVariable int instanceId) {
        return toResponse(workflowRuntimeService.retryCurrentTask(instanceId));
    }

    @PostMapping("/instances/{instanceId}/approve")
    public WorkflowInstanceResponse approveTask(@PathVariable int instanceId) {
        return toResponse(workflowRuntimeService.approveCurrentTask(instanceId));
    }

    @PostMapping("/instances/{instanceId}/reject")
    public WorkflowInstanceResponse rejectTask(@PathVariable int instanceId) {
        return toResponse(workflowRuntimeService.rejectCurrentTask(instanceId));
    }

    @GetMapping("/instances/{instanceId}")
    public WorkflowInstanceResponse getInstance(@PathVariable int instanceId) {
        return toResponse(workflowRuntimeService.getInstance(instanceId));
    }

    private WorkflowInstanceResponse toResponse(WorkflowInstance instance) {
        List<TaskStatusResponse> tasks = new ArrayList<>();
        for (Task task : instance.getWorkflow().getTasks()) {
            tasks.add(new TaskStatusResponse(task.getTaskId(), task.getTaskName(), task.getStatus()));
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

    public record TaskStatusResponse(int taskId, String taskName, String status) {
    }
}
