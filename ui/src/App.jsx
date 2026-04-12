import { useMemo, useState } from "react";
import {
  approveCurrentTask,
  createWorkflow as createWorkflowRequest,
  executeCurrentTask,
  getWorkflowInstance,
  rejectCurrentTask,
  retryCurrentTask,
  startWorkflowInstance,
  toBasicAuth
} from "./services/workflowApi";

const initialDefinition = {
  workflowName: "Purchase Request Workflow",
  tasks: [
    { taskId: 1, taskName: "Submit Request", assignedRole: "EMPLOYEE" },
    { taskId: 2, taskName: "Manager Approval", assignedRole: "MANAGER" },
    { taskId: 3, taskName: "Fulfill Request", assignedRole: "OPERATIONS" }
  ],
  transitions: [
    { fromTaskId: 1, toTaskId: 2 },
    { fromTaskId: 2, toTaskId: 3 }
  ]
};

export default function App() {
  const [username, setUsername] = useState("manager");
  const [password, setPassword] = useState("manager123");
  const [definition, setDefinition] = useState(initialDefinition);
  const [workflowId, setWorkflowId] = useState(null);
  const [instance, setInstance] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("Ready");
  const [error, setError] = useState("");

  const auth = useMemo(() => toBasicAuth(username, password), [username, password]);

  const run = async (label, fn) => {
    setBusy(true);
    setError("");
    try {
      const result = await fn();
      setMessage(label);
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setBusy(false);
    }
  };

  const createWorkflow = async () => {
    const data = await run("Workflow definition created", () => createWorkflowRequest(definition, auth));
    setWorkflowId(data.workflowId);
  };

  const startInstance = async () => {
    if (!workflowId) return;
    const data = await run("Workflow instance started", () => startWorkflowInstance(workflowId, auth));
    setInstance(data);
  };

  const refresh = async () => {
    if (!instance?.instanceId) return;
    const data = await run("Instance refreshed", () => getWorkflowInstance(instance.instanceId, auth));
    setInstance(data);
  };

  const callInstanceAction = async (requestFn, successLabel) => {
    if (!instance?.instanceId) return;
    const data = await run(successLabel, () => requestFn(instance.instanceId, auth));
    setInstance(data);
  };

  const statusTone = (status) => {
    if (["SUCCESS", "APPROVED", "COMPLETED"].includes(status)) return "good";
    if (["FAILURE", "FAILED", "REJECTED"].includes(status)) return "bad";
    return "neutral";
  };

  return (
    <div className="page">
      <div className="orb orb-a" />
      <div className="orb orb-b" />
      <main className="layout">
        <section className="hero card">
          <p className="kicker">Workflow Control Deck</p>
          <h1>Execution Engine UI</h1>
          <p>
            Create a workflow, launch an instance, and drive execution through
            execute, approve, retry, and reject actions in one place.
          </p>
          <div className="status-row">
            <span className="chip">{message}</span>
            {error && <span className="chip chip-error">{error}</span>}
          </div>
        </section>

        <section className="card auth-card">
          <h2>Access</h2>
          <div className="grid two">
            <label>
              Username
              <input value={username} onChange={(e) => setUsername(e.target.value)} />
            </label>
            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </label>
          </div>
        </section>

        <section className="card">
          <h2>Workflow Definition</h2>
          <label>
            Workflow Name
            <input
              value={definition.workflowName}
              onChange={(e) =>
                setDefinition((prev) => ({ ...prev, workflowName: e.target.value }))
              }
            />
          </label>
          <div className="actions">
            <button onClick={createWorkflow} disabled={busy}>Create Workflow</button>
            <button onClick={startInstance} disabled={busy || !workflowId}>Start Instance</button>
            <button onClick={refresh} disabled={busy || !instance}>Refresh State</button>
          </div>
          <p className="meta">Workflow ID: {workflowId ?? "Not created"}</p>
        </section>

        <section className="card">
          <h2>Instance Actions</h2>
          <div className="actions">
            <button onClick={() => callInstanceAction(executeCurrentTask, "Current task executed")} disabled={busy || !instance}>Execute</button>
            <button onClick={() => callInstanceAction(approveCurrentTask, "Task approved")} disabled={busy || !instance}>Approve</button>
            <button onClick={() => callInstanceAction(retryCurrentTask, "Retry applied")} disabled={busy || !instance}>Retry</button>
            <button className="danger" onClick={() => callInstanceAction(rejectCurrentTask, "Task rejected")} disabled={busy || !instance}>Reject</button>
          </div>
          <p className="meta">Instance ID: {instance?.instanceId ?? "No instance yet"}</p>
          <p className="meta">Current State: <strong>{instance?.state ?? "-"}</strong></p>
        </section>

        <section className="card">
          <h2>Task Board</h2>
          <div className="task-list">
            {(instance?.tasks || definition.tasks).map((task) => (
              <article className={`task ${statusTone(task.status || "PENDING")}`} key={task.taskId}>
                <h3>{task.taskName}</h3>
                <p>Task #{task.taskId}</p>
                <span>{task.status || "PENDING"}</span>
              </article>
            ))}
          </div>
        </section>

        <section className="card timeline">
          <h2>Execution Timeline</h2>
          <ol>
            {(instance?.history || []).slice().reverse().map((item, idx) => (
              <li key={`${idx}-${item}`}>{item}</li>
            ))}
          </ol>
        </section>
      </main>
    </div>
  );
}
