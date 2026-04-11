package workflow.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceRecord, Integer> {
    @Query("select coalesce(max(w.id), 0) from WorkflowInstanceRecord w")
    Integer findMaxId();
}
