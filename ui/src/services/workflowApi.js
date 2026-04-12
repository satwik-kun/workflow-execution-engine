export function toBasicAuth(username, password) {
  return `Basic ${btoa(`${username}:${password}`)}`;
}

async function request(path, { method = "GET", body, auth }) {
  const res = await fetch(path, {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: auth
    },
    body: body ? JSON.stringify(body) : undefined
  });

  const contentType = res.headers.get("content-type") || "";
  const payload = contentType.includes("application/json") ? await res.json() : await res.text();

  if (!res.ok) {
    const message = typeof payload === "object" ? payload.message || payload.error : payload;
    throw new Error(message || `Request failed (${res.status})`);
  }

  return payload;
}

export function createWorkflow(definition, auth) {
  return request("/api/workflows", {
    method: "POST",
    body: definition,
    auth
  });
}

export function startWorkflowInstance(workflowId, auth) {
  return request(`/api/workflows/${workflowId}/instances`, {
    method: "POST",
    auth
  });
}

export function getWorkflowInstance(instanceId, auth) {
  return request(`/api/instances/${instanceId}`, {
    auth
  });
}

export function executeCurrentTask(instanceId, auth) {
  return request(`/api/instances/${instanceId}/execute`, {
    method: "POST",
    auth
  });
}

export function approveCurrentTask(instanceId, auth) {
  return request(`/api/instances/${instanceId}/approve`, {
    method: "POST",
    auth
  });
}

export function retryCurrentTask(instanceId, auth) {
  return request(`/api/instances/${instanceId}/retry`, {
    method: "POST",
    auth
  });
}

export function rejectCurrentTask(instanceId, auth) {
  return request(`/api/instances/${instanceId}/reject`, {
    method: "POST",
    auth
  });
}
