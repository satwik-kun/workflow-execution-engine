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

const actorPresets = {
  employee: {
    label: "Employee",
    username: "employee",
    password: "employee123",
    role: "EMPLOYEE",
    description: "Creates and starts work"
  },
  manager: {
    label: "Manager",
    username: "manager",
    password: "manager123",
    role: "MANAGER",
    description: "Approves or rejects tasks"
  },
  operations: {
    label: "Operations",
    username: "operations",
    password: "operations123",
    role: "OPERATIONS",
    description: "Handles fulfillment steps"
  }
};

const initialDefinition = {
  workflowName: "UrbanThread Clothing Order Fulfillment",
  tasks: [
    { taskId: 1, taskName: "Capture Customer Order", assignedRole: "EMPLOYEE" },
    { taskId: 2, taskName: "Reserve Fabric And Inventory", assignedRole: "OPERATIONS" },
    { taskId: 3, taskName: "Manager Approval For Production Batch", assignedRole: "MANAGER" },
    { taskId: 4, taskName: "Cutting And Stitching", assignedRole: "OPERATIONS" },
    { taskId: 5, taskName: "Quality Control Inspection", assignedRole: "OPERATIONS" },
    { taskId: 6, taskName: "Packaging And Dispatch", assignedRole: "OPERATIONS" },
    { taskId: 7, taskName: "Send Customer Shipment Notification", assignedRole: "OPERATIONS" }
  ],
  transitions: [
    { fromTaskId: 1, toTaskId: 2 },
    { fromTaskId: 2, toTaskId: 3 },
    { fromTaskId: 3, toTaskId: 4 },
    { fromTaskId: 4, toTaskId: 5 },
    { fromTaskId: 5, toTaskId: 6 },
    { fromTaskId: 6, toTaskId: 7 }
  ]
};

const orderTiles = [
  { code: "UT-SS26-001", label: "Summer Drop Hoodie", channel: "Online" },
  { code: "UT-DNM-014", label: "Weekend Denim Jacket", channel: "Retail" },
  { code: "UT-ATH-022", label: "Athleisure Co-ord Set", channel: "Marketplace" }
];

const initialOrderQueue = orderTiles.map((order, idx) => ({
  ...order,
  queueStatus: idx === 0 ? "READY" : "LOCKED",
  workflowId: null,
  instanceId: null
}));

export default function App() {
  const [activeActor, setActiveActor] = useState("manager");
  const [username, setUsername] = useState(actorPresets.manager.username);
  const [password, setPassword] = useState(actorPresets.manager.password);
  const [definition, setDefinition] = useState(initialDefinition);
  const [workflowId, setWorkflowId] = useState(null);
  const [instance, setInstance] = useState(null);
  const [orderQueue, setOrderQueue] = useState(initialOrderQueue);
  const [activeOrderIndex, setActiveOrderIndex] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("Ready");
  const [error, setError] = useState("");

  const auth = useMemo(() => toBasicAuth(username, password), [username, password]);

  const selectActor = (actorKey) => {
    const preset = actorPresets[actorKey];
    if (!preset) return;

    setActiveActor(actorKey);
    setUsername(preset.username);
    setPassword(preset.password);
    setMessage(`${preset.label} profile loaded`);
    setError("");
  };

  const currentActor = actorPresets[activeActor] ?? actorPresets.manager;

  const canApprove = activeActor === "manager";
  const canRunDefinitionFlow = activeActor === "employee" || activeActor === "manager";
  const boardTasks = instance?.tasks || definition.tasks;
  const activeTask = instance?.tasks?.find((task) => task.taskId === instance.currentTaskId) ?? null;
  const actorRole = currentActor.role;
  const canExecuteForCurrentRole = !instance || !activeTask || activeTask.assignedRole === actorRole;
  const canRetryForCurrentRole = !instance || !activeTask || activeTask.assignedRole === actorRole;
  const canApproveForCurrentRole =
    canApprove && !!instance && !!activeTask && activeTask.assignedRole === "MANAGER";

  const nextReadyOrderIndex = orderQueue.findIndex((order) => order.queueStatus === "READY");
  const hasRunningQueueOrder = orderQueue.some((order) => order.queueStatus === "IN_PROGRESS");
  const queueComplete = orderQueue.every((order) => order.queueStatus === "COMPLETED");
  const firstNotCompletedOrderIndex = orderQueue.findIndex((order) => order.queueStatus !== "COMPLETED");
  const activeQueueOrder = activeOrderIndex !== null ? orderQueue[activeOrderIndex] : null;
  const completedCount = boardTasks.filter((task) => ["SUCCESS", "APPROVED", "COMPLETED"].includes(task.status || "")).length;
  const progress = boardTasks.length ? Math.round((completedCount / boardTasks.length) * 100) : 0;

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

  const markQueueProgress = (updatedInstance) => {
    if (!updatedInstance) {
      return;
    }

    // If there's an active order, update its status based on instance state or task completion
    if (activeOrderIndex !== null) {
      // Count completed tasks
      const totalTasks = updatedInstance.tasks?.length || 0;
      const completedTasks = updatedInstance.tasks?.filter(
        (task) => ["SUCCESS", "APPROVED", "COMPLETED"].includes(task.status)
      ).length || 0;
      
      // Check if order is complete: either instance is marked COMPLETED/FAILED, or all tasks are done
      const isComplete =
        updatedInstance.state === "COMPLETED" ||
        updatedInstance.state === "FAILED" ||
        (totalTasks > 0 && completedTasks === totalTasks);
      
      if (isComplete && orderQueue[activeOrderIndex]?.queueStatus === "IN_PROGRESS") {
        setOrderQueue((prev) => {
          const copy = [...prev];
          const current = { ...copy[activeOrderIndex] };
          
          // Determine status
          const isFailed = updatedInstance.state === "FAILED" || (completedTasks < totalTasks);
          current.queueStatus = isFailed ? "FAILED" : "COMPLETED";
          current.instanceId = updatedInstance.instanceId;
          copy[activeOrderIndex] = current;

          // Unlock next order if current one completed successfully
          if (!isFailed) {
            const nextIdx = activeOrderIndex + 1;
            if (nextIdx < copy.length && copy[nextIdx].queueStatus === "LOCKED") {
              copy[nextIdx] = { ...copy[nextIdx], queueStatus: "READY" };
            }
          }

          return copy;
        });

        if (updatedInstance.state !== "FAILED" && completedTasks === totalTasks) {
          const nextPosition = activeOrderIndex + 2;
          if (nextPosition <= orderQueue.length) {
            setMessage(`Order #${activeOrderIndex + 1} completed. Queue moved to #${nextPosition}.`);
          } else {
            setMessage("All live orders completed.");
          }
        } else {
          setMessage(`Order #${activeOrderIndex + 1} failed.`);
        }

        setActiveOrderIndex(null);
      }
    }
  };

  const startNextQueueOrder = async () => {
    if (firstNotCompletedOrderIndex < 0 || hasRunningQueueOrder) {
      return;
    }

    const targetIdx = firstNotCompletedOrderIndex;
    const order = orderQueue[targetIdx];

    if (order.queueStatus === "LOCKED") {
      setOrderQueue((prev) => {
        const copy = [...prev];
        copy[targetIdx] = { ...copy[targetIdx], queueStatus: "READY" };
        return copy;
      });
    }

    const queueDefinition = {
      ...definition,
      workflowName: `${order.code} - ${order.label}`
    };

    const createdWorkflow = await run(
      `Workflow created for ${order.code}`,
      () => createWorkflowRequest(queueDefinition, auth)
    );

    const startedInstance = await run(
      `Started queue order ${order.code}`,
      () => startWorkflowInstance(createdWorkflow.workflowId, auth)
    );

    setWorkflowId(createdWorkflow.workflowId);
    setInstance(startedInstance);
    setActiveOrderIndex(targetIdx);
    setOrderQueue((prev) => {
      const copy = [...prev];
      copy[targetIdx] = {
        ...copy[targetIdx],
        queueStatus: "IN_PROGRESS",
        workflowId: createdWorkflow.workflowId,
        instanceId: startedInstance.instanceId
      };
      return copy;
    });
  };

  const startInstance = async () => {
    if (!workflowId) return;
    const data = await run("Instance started", () => startWorkflowInstance(workflowId, auth));
    setInstance(data);
  };

  const refresh = async () => {
    if (!instance?.instanceId) return;
    const data = await run("Instance refreshed", () => getWorkflowInstance(instance.instanceId, auth));
    setInstance(data);
    
    // Only update queue if we're in queue mode (activeOrderIndex is set)
    if (activeOrderIndex !== null) {
      markQueueProgress(data);
    }
    
    // If workflow completed in normal mode, reset for next workflow
    if ((data.state === "COMPLETED" || data.state === "FAILED") && activeOrderIndex === null) {
      setTimeout(() => {
        setInstance(null);
        setWorkflowId(null);
        setMessage(`Workflow ${data.state.toLowerCase()}. Ready for new workflow.`);
      }, 1000);
    }
  };

  const unlockNextQueueOrder = () => {
    if (activeOrderIndex === null) return;
    
    const nextIdx = activeOrderIndex + 1;
    if (nextIdx < orderQueue.length && orderQueue[nextIdx].queueStatus === "LOCKED") {
      setOrderQueue((prev) => {
        const copy = [...prev];
        copy[nextIdx] = { ...copy[nextIdx], queueStatus: "READY" };
        return copy;
      });
      setActiveOrderIndex(null);
      setMessage(`Manually unlocked queue order #${nextIdx + 1}`);
    }
  };

  const callInstanceAction = async (requestFn, successLabel) => {
    if (!instance?.instanceId) return;
    const data = await run(successLabel, () => requestFn(instance.instanceId, auth));
    setInstance(data);
    
    // Only update queue if in queue mode
    if (activeOrderIndex !== null) {
      markQueueProgress(data);
    }
    
    // If workflow completed in normal mode, reset for next workflow
    if ((data.state === "COMPLETED" || data.state === "FAILED") && activeOrderIndex === null) {
      setTimeout(() => {
        setInstance(null);
        setWorkflowId(null);
        setMessage(`Workflow ${data.state.toLowerCase()}. Ready for new workflow.`);
      }, 1000);
    }
  };

  const queueTone = (queueStatus) => {
    if (queueStatus === "COMPLETED") return "good";
    if (queueStatus === "FAILED") return "bad";
    if (queueStatus === "IN_PROGRESS") return "active";
    return "neutral";
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
          <p className="kicker">UrbanThread Manufacturing Ops</p>
          <h1>Clothing Brand Workflow Console</h1>
          <p>
            Run configurable apparel order workflows in real time, from intake and inventory
            reservation to production, quality control, and shipment updates.
          </p>
          <div className="status-row">
            <span className="chip">{message}</span>
            {error && <span className="chip chip-error">{error}</span>}
          </div>
          <div className="hero-stats">
            <article>
              <h3>Workflow ID</h3>
              <p>{workflowId ?? "Not created"}</p>
            </article>
            <article>
              <h3>Instance</h3>
              <p>{instance?.instanceId ?? "No live instance"}</p>
            </article>
            <article>
              <h3>Progress</h3>
              <p>{progress}%</p>
            </article>
            <article>
              <h3>State</h3>
              <p>{instance?.state ?? "Idle"}</p>
            </article>
          </div>
        </section>

        <section className="card">
          <h2>Live Order Queue</h2>
          <p className="meta">
            Each order is run as its own workflow. Queue order #2 unlocks only after #1 completes.
          </p>
          <div className="actions">
            <button
              onClick={startNextQueueOrder}
              disabled={busy || !canRunDefinitionFlow || hasRunningQueueOrder || firstNotCompletedOrderIndex < 0}
            >
              Start Next Queue Order
            </button>
            <button onClick={refresh} disabled={busy || !instance}>Refresh Active Order</button>
            <button
              onClick={unlockNextQueueOrder}
              disabled={activeOrderIndex === null}
              style={{ opacity: 0.7 }}
              title="Manual recovery: unlock next order if current one is stuck"
            >
              Unlock Next Order (Recovery)
            </button>
          </div>
          {activeQueueOrder && (
            <p className="meta">
              Active queue order: <strong>{activeQueueOrder.code}</strong> · {activeQueueOrder.label}
            </p>
          )}
          {queueComplete && <p className="meta"><strong>Queue complete:</strong> all live orders are finished.</p>}
          <div className="order-grid">
            {orderQueue.map((order, idx) => (
              <article key={order.code} className="order-card">
                <small>{order.code}</small>
                <h3>{order.label}</h3>
                <p>{order.channel} Channel</p>
                <p className="order-meta">Queue #{idx + 1}</p>
                <span className={`queue-chip ${queueTone(order.queueStatus)}`}>{order.queueStatus}</span>
              </article>
            ))}
          </div>
        </section>

        <section className="card auth-card">
          <h2>Role Access</h2>
          <div className="actor-switcher" role="tablist" aria-label="Demo actors">
            {Object.entries(actorPresets).map(([key, preset]) => (
              <button
                key={key}
                type="button"
                className={key === activeActor ? "actor-pill active" : "actor-pill"}
                onClick={() => selectActor(key)}
              >
                <span>{preset.label}</span>
                <small>{preset.role}</small>
              </button>
            ))}
          </div>
          <p className="actor-summary">
            Active actor: <strong>{currentActor.label}</strong> · {currentActor.description}
          </p>
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
          <h2>Workflow Definition (Configurable)</h2>
          <p className="meta">
            Keep SRP and role boundaries intact: Employee starts flow, Manager handles gate approvals,
            Operations executes production and dispatch steps.
          </p>
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
            <button onClick={createWorkflow} disabled={busy || !canRunDefinitionFlow}>Create Workflow</button>
            <button onClick={startInstance} disabled={busy || !workflowId || !!instance}>Start Instance</button>
            <button onClick={refresh} disabled={busy || !instance}>Refresh State</button>
          </div>
          <p className="meta">Workflow ID: {workflowId ?? "Not created"}</p>
          <div className="progress-wrap">
            <div className="progress-bar" style={{ width: `${progress}%` }} />
          </div>
        </section>

        <section className="card">
          <h2>Instance Actions</h2>
          <div className="actions">
            <button
              onClick={() => callInstanceAction(executeCurrentTask, "Current task executed")}
              disabled={busy || !instance || !canExecuteForCurrentRole}
            >
              Execute
            </button>
            <button
              onClick={() => callInstanceAction(approveCurrentTask, "Task approved")}
              disabled={busy || !canApproveForCurrentRole}
            >
              Approve
            </button>
            <button
              onClick={() => callInstanceAction(retryCurrentTask, "Retry applied")}
              disabled={busy || !instance || !canRetryForCurrentRole}
            >
              Retry
            </button>
            <button
              className="danger"
              onClick={() => callInstanceAction(rejectCurrentTask, "Task rejected")}
              disabled={busy || !canApproveForCurrentRole}
            >
              Reject
            </button>
          </div>
          <p className="meta">
            Approval actions are enabled only for Manager. Use Execute for production stages and Retry when a stage fails.
          </p>
          {instance && activeTask && (
            <p className="meta">
              Current step owner: <strong>{activeTask.assignedRole}</strong>. Active actor role: <strong>{actorRole}</strong>.
            </p>
          )}
          <p className="meta">Instance ID: {instance?.instanceId ?? "No instance yet"}</p>
          <p className="meta">Current State: <strong>{instance?.state ?? "-"}</strong></p>
        </section>

        <section className="card">
          <h2>Role Coverage</h2>
          <div className="role-grid">
            <article className={activeActor === "employee" ? "role-card active" : "role-card"}>
              <h3>Employee</h3>
              <p>Captures order and initiates the workflow.</p>
            </article>
            <article className={activeActor === "manager" ? "role-card active" : "role-card"}>
              <h3>Manager</h3>
              <p>Approves production batch before manufacturing starts.</p>
            </article>
            <article className={activeActor === "operations" ? "role-card active" : "role-card"}>
              <h3>Operations</h3>
              <p>Runs inventory, stitching, quality, dispatch and shipment notification.</p>
            </article>
          </div>
        </section>

        <section className="card">
          <h2>Task Board</h2>
          <div className="task-list">
            {boardTasks.map((task) => (
              <article className={`task ${statusTone(task.status || "PENDING")}`} key={task.taskId}>
                <h3>{task.taskName}</h3>
                <p>
                  Task #{task.taskId} · {task.assignedRole || "UNASSIGNED"}
                </p>
                <span>{task.status || "PENDING"}</span>
              </article>
            ))}
          </div>
        </section>

        <section className="card timeline">
          <h2>Execution Timeline</h2>
          {instance?.history?.length ? (
            <ol>
              {instance.history.slice().reverse().map((item, idx) => (
                <li key={`${idx}-${item}`}>
                  <span className="timeline-index">#{instance.history.length - idx}</span>
                  <span>{item}</span>
                </li>
              ))}
            </ol>
          ) : (
            <p className="meta">No live events yet. Start an instance and execute actions to see audit history.</p>
          )}
        </section>
      </main>
    </div>
  );
}
