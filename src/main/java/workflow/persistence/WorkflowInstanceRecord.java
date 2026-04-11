package workflow.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_instances")
public class WorkflowInstanceRecord {
    @Id
    private Integer id;

    @Column(name = "workflow_id", nullable = false)
    private Integer workflowId;

    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    @Column(name = "current_task_id", nullable = false)
    private Integer currentTaskId;

    @Column(nullable = false)
    private String state;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_failure_details", nullable = false)
    private String lastFailureDetails;

    @Lob
    @Column(name = "workflow_snapshot", nullable = false)
    private String workflowSnapshot;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Integer workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public Integer getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(Integer currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastFailureDetails() {
        return lastFailureDetails;
    }

    public void setLastFailureDetails(String lastFailureDetails) {
        this.lastFailureDetails = lastFailureDetails;
    }

    public String getWorkflowSnapshot() {
        return workflowSnapshot;
    }

    public void setWorkflowSnapshot(String workflowSnapshot) {
        this.workflowSnapshot = workflowSnapshot;
    }
}
