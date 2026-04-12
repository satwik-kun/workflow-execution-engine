package workflow.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb-it;DB_CLOSE_DELAY=-1;MODE=LEGACY",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "workflow.execution.demo-mode=true"
    }
)
class WorkflowApiIT {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private TestRestTemplate asManager() {
        return restTemplate.withBasicAuth("manager", "manager123");
    }

    private TestRestTemplate asEmployee() {
        return restTemplate.withBasicAuth("employee", "employee123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fullLifecycleFlow_shouldReachCompleted() {
        String baseUrl = "http://localhost:" + port + "/api";

        Map<String, Object> createRequest = Map.of(
            "workflowName", "Integration Workflow",
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

        ResponseEntity<Map> createResponse = post(baseUrl + "/workflows", createRequest, Map.class);
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        Integer workflowId = (Integer) createResponse.getBody().get("workflowId");
        assertNotNull(workflowId);

        ResponseEntity<Map> startResponse = post(
            baseUrl + "/workflows/" + workflowId + "/instances",
            null,
            Map.class
        );
        assertEquals(HttpStatus.OK, startResponse.getStatusCode());
        Integer instanceId = (Integer) startResponse.getBody().get("instanceId");
        assertNotNull(instanceId);

        ResponseEntity<Map> stateResponse = post(baseUrl + "/instances/" + instanceId + "/execute", null, Map.class);
        assertEquals(HttpStatus.OK, stateResponse.getStatusCode());

        int safetyCounter = 0;
        while ("RUNNING".equals((String) stateResponse.getBody().get("state")) && safetyCounter < 10) {
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

            safetyCounter++;
        }

        ResponseEntity<Map> finalResponse = asManager().getForEntity(
            baseUrl + "/instances/" + instanceId,
            Map.class
        );

        assertEquals(HttpStatus.OK, finalResponse.getStatusCode());
        assertEquals("COMPLETED", finalResponse.getBody().get("state"));
        assertTrue(((List<?>) finalResponse.getBody().get("history")).size() >= 5);
    }

    @Test
    void createWorkflow_withInvalidRequest_shouldReturnBadRequest() {
        String baseUrl = "http://localhost:" + port + "/api";

        Map<String, Object> invalidRequest = Map.of(
            "workflowName", "",
            "tasks", List.of(),
            "transitions", List.of()
        );

        ResponseEntity<Map> response = post(baseUrl + "/workflows", invalidRequest, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().get("code"));
        assertNotNull(response.getBody().get("message"));
    }

    @Test
    void approveEndpoint_withNonManagerUser_shouldReturnForbidden() {
        String baseUrl = "http://localhost:" + port + "/api";

        Map<String, Object> createRequest = Map.of(
            "workflowName", "Auth Workflow",
            "tasks", List.of(
                Map.of("taskId", 1, "taskName", "Submit", "assignedRole", "EMPLOYEE"),
                Map.of("taskId", 2, "taskName", "Approve", "assignedRole", "MANAGER")
            ),
            "transitions", List.of(
                Map.of("fromTaskId", 1, "toTaskId", 2)
            )
        );

        Integer workflowId = (Integer) post(baseUrl + "/workflows", createRequest, Map.class).getBody().get("workflowId");
        Integer instanceId = (Integer) post(baseUrl + "/workflows/" + workflowId + "/instances", null, Map.class).getBody().get("instanceId");
        post(baseUrl + "/instances/" + instanceId + "/execute", null, Map.class);

        ResponseEntity<Map> forbidden = postAs(
            asEmployee(),
            baseUrl + "/instances/" + instanceId + "/approve",
            null,
            Map.class
        );

        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void createWorkflow_withDuplicateTaskIds_shouldReturnBadRequest() {
        String baseUrl = "http://localhost:" + port + "/api";

        Map<String, Object> duplicateTaskRequest = Map.of(
            "workflowName", "Duplicate Task Workflow",
            "tasks", List.of(
                Map.of("taskId", 1, "taskName", "Submit", "assignedRole", "EMPLOYEE"),
                Map.of("taskId", 1, "taskName", "Approve", "assignedRole", "MANAGER")
            ),
            "transitions", List.of(
                Map.of("fromTaskId", 1, "toTaskId", 2)
            )
        );

        ResponseEntity<Map> response = post(baseUrl + "/workflows", duplicateTaskRequest, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().get("code"));
        assertTrue(((String) response.getBody().get("message")).contains("Duplicate taskId"));
    }

    @Test
    void createWorkflow_withSelfLoopTransition_shouldReturnBadRequest() {
        String baseUrl = "http://localhost:" + port + "/api";

        Map<String, Object> selfLoopRequest = Map.of(
            "workflowName", "Self Loop Workflow",
            "tasks", List.of(
                Map.of("taskId", 1, "taskName", "Submit", "assignedRole", "EMPLOYEE")
            ),
            "transitions", List.of(
                Map.of("fromTaskId", 1, "toTaskId", 1)
            )
        );

        ResponseEntity<Map> response = post(baseUrl + "/workflows", selfLoopRequest, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().get("code"));
        assertTrue(((String) response.getBody().get("message")).contains("Self-loop transitions"));
    }

    @Test
    void rejectTask_shouldMoveInstanceToFailedState() {
        String baseUrl = "http://localhost:" + port + "/api";

        Map<String, Object> createRequest = Map.of(
            "workflowName", "Reject Path Workflow",
            "tasks", List.of(
                Map.of("taskId", 1, "taskName", "Submit", "assignedRole", "EMPLOYEE"),
                Map.of("taskId", 2, "taskName", "Approve", "assignedRole", "MANAGER")
            ),
            "transitions", List.of(
                Map.of("fromTaskId", 1, "toTaskId", 2)
            )
        );

        Integer workflowId = (Integer) post(baseUrl + "/workflows", createRequest, Map.class).getBody().get("workflowId");
        Integer instanceId = (Integer) post(baseUrl + "/workflows/" + workflowId + "/instances", null, Map.class).getBody().get("instanceId");

        post(baseUrl + "/instances/" + instanceId + "/execute", null, Map.class);
        post(baseUrl + "/instances/" + instanceId + "/retry", null, Map.class);
        ResponseEntity<Map> rejectResponse = post(baseUrl + "/instances/" + instanceId + "/reject", null, Map.class);

        assertEquals(HttpStatus.OK, rejectResponse.getStatusCode());
        assertEquals("FAILED", rejectResponse.getBody().get("state"));
    }

    private <T> ResponseEntity<T> post(String url, Object body, Class<T> responseType) {
        return postAs(asManager(), url, body, responseType);
    }

    private <T> ResponseEntity<T> postAs(
        TestRestTemplate client,
        String url,
        Object body,
        Class<T> responseType
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return client.exchange(url, HttpMethod.POST, entity, responseType);
    }
}
