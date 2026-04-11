package workflow.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinitionRecord {
    @Id
    private Integer id;

    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    @Lob
    @Column(name = "workflow_snapshot", nullable = false)
    private String workflowSnapshot;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getWorkflowSnapshot() {
        return workflowSnapshot;
    }

    public void setWorkflowSnapshot(String workflowSnapshot) {
        this.workflowSnapshot = workflowSnapshot;
    }
}
