package workflow.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinitionRecord, Integer> {
    @Query("select coalesce(max(w.id), 1000) from WorkflowDefinitionRecord w")
    Integer findMaxId();
}
