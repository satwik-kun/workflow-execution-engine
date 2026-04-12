package workflow.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import workflow.persistence.WorkflowHistoryRepository;
import workflow.persistence.WorkflowInstanceRepository;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-st;DB_CLOSE_DELAY=-1;MODE=LEGACY",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "workflow.execution.demo-mode=true"
    }
)
class WorkflowSystemST {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private TestRestTemplate asManager() {
        return restTemplate.withBasicAuth("manager", "manager123");
    }

    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    private WorkflowHistoryRepository workflowHistoryRepository;

    @Test
    @SuppressWarnings("unchecked")
    void systemFlow_shouldPersistInstanceAndHistory() {
        String baseUrl = "http://localhost:" + port + "/api";

        Map<String, Object> request = Map.of(
            "workflowName", "System Test Workflow",
            "tasks", List.of(
                Map.of("taskId", 1, "taskName", "Submit", "assignedRole", "EMPLOYEE"),
                Map.of("taskId", 2, "taskName", "Approve", "assignedRole", "MANAGER"),
                Map.of("taskId", 3, "taskName", "Fulfill", "assignedRole", "OPS")
            ),
            "transitions", List.of(
                Map.of("fromTaskId", 1, "toTaskId", 2),
                Map.of("fromTaskId", 2, "toTaskId", 3)
            )
        );

        Integer workflowId = (Integer) post(baseUrl + "/workflows", request, Map.class).getBody().get("workflowId");
        Integer instanceId = (Integer) post(
            baseUrl + "/workflows/" + workflowId + "/instances",
            null,
            Map.class
        ).getBody().get("instanceId");

        ResponseEntity<Map> stateResponse = post(baseUrl + "/instances/" + instanceId + "/execute", null, Map.class);

        int loops = 0;
        while ("RUNNING".equals((String) stateResponse.getBody().get("state")) && loops < 10) {
            Integer currentTaskId = (Integer) stateResponse.getBody().get("currentTaskId");
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) stateResponse.getBody().get("tasks");
            String currentTaskStatus = tasks.stream()
                .filter(task -> ((Integer) task.get("taskId")).equals(currentTaskId))
                .findFirst()
                .map(task -> (String) task.get("status"))
                .orElse("PENDING");

            if (currentTaskId == 2) {
                stateResponse = post(baseUrl + "/instances/" + instanceId + "/approve", null, Map.class);
            } else if ("FAILURE".equals(currentTaskStatus)) {
                stateResponse = post(baseUrl + "/instances/" + instanceId + "/retry", null, Map.class);
            } else {
                stateResponse = post(baseUrl + "/instances/" + instanceId + "/execute", null, Map.class);
            }
            loops++;
        }

        ResponseEntity<Map> finalResponse = asManager().getForEntity(
            baseUrl + "/instances/" + instanceId,
            Map.class
        );
        assertEquals(HttpStatus.OK, finalResponse.getStatusCode());
        assertEquals("COMPLETED", finalResponse.getBody().get("state"));

        var persistedInstance = workflowInstanceRepository.findById(instanceId);
        assertTrue(persistedInstance.isPresent());
        assertEquals("COMPLETED", persistedInstance.get().getState());

        var persistedHistory = workflowHistoryRepository.findByInstanceIdOrderBySequenceNoAsc(instanceId);
        assertFalse(persistedHistory.isEmpty());
        assertTrue(persistedHistory.size() >= 5);
        assertNotNull(persistedHistory.get(0).getEventText());
    }

    @Test
    void rejectPath_shouldPersistFailedStateAndRejectionHistory() {
        String baseUrl = "http://localhost:" + port + "/api";

        Map<String, Object> request = Map.of(
            "workflowName", "System Reject Workflow",
            "tasks", List.of(
                Map.of("taskId", 1, "taskName", "Submit", "assignedRole", "EMPLOYEE"),
                Map.of("taskId", 2, "taskName", "Approve", "assignedRole", "MANAGER")
            ),
            "transitions", List.of(
                Map.of("fromTaskId", 1, "toTaskId", 2)
            )
        );

        Integer workflowId = (Integer) post(baseUrl + "/workflows", request, Map.class).getBody().get("workflowId");
        Integer instanceId = (Integer) post(baseUrl + "/workflows/" + workflowId + "/instances", null, Map.class).getBody().get("instanceId");

        post(baseUrl + "/instances/" + instanceId + "/execute", null, Map.class);
        post(baseUrl + "/instances/" + instanceId + "/retry", null, Map.class);
        ResponseEntity<Map> rejected = post(baseUrl + "/instances/" + instanceId + "/reject", null, Map.class);

        assertEquals(HttpStatus.OK, rejected.getStatusCode());
        assertEquals("FAILED", rejected.getBody().get("state"));

        var persistedInstance = workflowInstanceRepository.findById(instanceId);
        assertTrue(persistedInstance.isPresent());
        assertEquals("FAILED", persistedInstance.get().getState());

        var persistedHistory = workflowHistoryRepository.findByInstanceIdOrderBySequenceNoAsc(instanceId);
        assertTrue(persistedHistory.stream().anyMatch(event -> event.getEventText().contains("rejected")));
    }

    private <T> ResponseEntity<T> post(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return asManager().exchange(url, HttpMethod.POST, entity, responseType);
    }
}
