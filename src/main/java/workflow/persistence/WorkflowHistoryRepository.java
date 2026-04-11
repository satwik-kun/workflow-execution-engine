package workflow.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistoryRecord, Long> {
    List<WorkflowHistoryRecord> findByInstanceIdOrderBySequenceNoAsc(Integer instanceId);

    void deleteByInstanceId(Integer instanceId);
}
