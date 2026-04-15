$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080/api"
$line = "=" * 88
$credentialsByRole = @{
    EMPLOYEE = @{ username = "employee"; password = "employee123" }
    MANAGER = @{ username = "manager"; password = "manager123" }
    OPERATIONS = @{ username = "operations"; password = "operations123" }
}

function Get-HeadersForRole([string]$role) {
    $normalizedRole = if ([string]::IsNullOrWhiteSpace($role)) { "EMPLOYEE" } else { $role.Trim().ToUpperInvariant() }
    if (-not $credentialsByRole.ContainsKey($normalizedRole)) {
        throw "No credentials configured for role '$normalizedRole'"
    }

    $creds = $credentialsByRole[$normalizedRole]
    $token = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$($creds.username)`:$($creds.password)"))
    return @{ Authorization = "Basic $token" }
}

function Get-RoleForCurrentTask([object]$state) {
    if ($null -eq $state -or $null -eq $state.tasks) {
        return "EMPLOYEE"
    }

    $currentTask = $state.tasks | Where-Object { $_.taskId -eq $state.currentTaskId } | Select-Object -First 1
    if ($null -eq $currentTask -or [string]::IsNullOrWhiteSpace($currentTask.assignedRole)) {
        return "EMPLOYEE"
    }

    return $currentTask.assignedRole
}

function Write-Section([string]$title) {
    Write-Host "`n$line" -ForegroundColor DarkCyan
    Write-Host $title -ForegroundColor Cyan
    Write-Host $line -ForegroundColor DarkCyan
}

function Write-Step([string]$label, [string]$detail) {
    Write-Host ("[STEP] {0}" -f $label) -ForegroundColor Yellow
    if ($detail) {
        Write-Host ("       {0}" -f $detail) -ForegroundColor Gray
    }
}

function Write-State([object]$state) {
    $currentTask = $state.tasks | Where-Object { $_.taskId -eq $state.currentTaskId } | Select-Object -First 1
    $taskLabel = if ($null -ne $currentTask) {
        "{0} ({1})" -f $currentTask.taskId, $currentTask.taskName
    } else {
        [string]$state.currentTaskId
    }

    $summary = [PSCustomObject]@{
        InstanceId     = $state.instanceId
        Workflow       = $state.workflowName
        State          = $state.state
        CurrentTask    = $taskLabel
        RetryCount     = $state.retryCount
        LastFailure    = if ([string]::IsNullOrWhiteSpace($state.lastFailureDetails)) { "-" } else { $state.lastFailureDetails }
    }

    $summary | Format-List | Out-String | Write-Host
}

function Assert-ServerReady {
    try {
        $null = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
    } catch {
        Write-Host "Server is not reachable at http://localhost:8080" -ForegroundColor Red
        Write-Host "Start it first from workflow-execution-engine:" -ForegroundColor Red
        Write-Host "  .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
        exit 1
    }
}

function Invoke-ApiPost([string]$uri, [object]$body = $null, [string]$role = "EMPLOYEE") {
    $headers = Get-HeadersForRole $role
    if ($null -eq $body) {
        return Invoke-RestMethod -Method Post -Uri $uri -Headers $headers
    }
    return Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType "application/json" -Body $body
}

function Invoke-ApiGet([string]$uri, [string]$role = "EMPLOYEE") {
    $headers = Get-HeadersForRole $role
    return Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
}

function Execute-To-End([object]$state, [string]$orderTag) {
    $loop = 0
    while ($state.state -eq "RUNNING") {
        $loop++
        $currentTask = $state.tasks | Where-Object { $_.taskId -eq $state.currentTaskId } | Select-Object -First 1
        $taskName = if ($null -ne $currentTask) { $currentTask.taskName } else { "Unknown Task" }
        $hasFailedTask = ($null -ne $currentTask -and $currentTask.status -eq "FAILURE")
        $taskRole = Get-RoleForCurrentTask $state

        if ($hasFailedTask) {
            Write-Step ("{0}: Retry failed task (cycle {1})" -f $orderTag, $loop) ("POST /instances/{0}/retry as {1}" -f $state.instanceId, $taskRole)
            $state = Invoke-ApiPost ("$baseUrl/instances/{0}/retry" -f $state.instanceId) $null $taskRole
        } elseif ($state.currentTaskId -eq 3) {
            Write-Step ("{0}: Approve manager gate for {1}" -f $orderTag, $taskName) ("POST /instances/{0}/approve as MANAGER" -f $state.instanceId)
            $state = Invoke-ApiPost ("$baseUrl/instances/{0}/approve" -f $state.instanceId) $null "MANAGER"
        } else {
            Write-Step ("{0}: Execute {1}" -f $orderTag, $taskName) ("POST /instances/{0}/execute as {1}" -f $state.instanceId, $taskRole)
            $state = Invoke-ApiPost ("$baseUrl/instances/{0}/execute" -f $state.instanceId) $null $taskRole
        }

        Write-State $state
        $snapshot = Invoke-ApiGet ("$baseUrl/instances/{0}" -f $state.instanceId) "MANAGER"
        Write-Host ("[LIVE] {0}: history events so far = {1}" -f $orderTag, $snapshot.history.Count) -ForegroundColor DarkGray
    }

    return $state
}

Assert-ServerReady

$workflowBody = @{
    workflowName = "UrbanThread Clothing Order Fulfillment"
    tasks = @(
        @{ taskId = 1; taskName = "Capture Customer Order"; assignedRole = "EMPLOYEE" },
        @{ taskId = 2; taskName = "Reserve Fabric And Inventory"; assignedRole = "OPERATIONS" },
        @{ taskId = 3; taskName = "Manager Approval For Production Batch"; assignedRole = "MANAGER" },
        @{ taskId = 4; taskName = "Cutting And Stitching"; assignedRole = "OPERATIONS" },
        @{ taskId = 5; taskName = "Quality Control Inspection"; assignedRole = "OPERATIONS" },
        @{ taskId = 6; taskName = "Packaging And Dispatch"; assignedRole = "OPERATIONS" },
        @{ taskId = 7; taskName = "Send Customer Shipment Notification"; assignedRole = "OPERATIONS" }
    )
    transitions = @(
        @{ fromTaskId = 1; toTaskId = 2 },
        @{ fromTaskId = 2; toTaskId = 3 },
        @{ fromTaskId = 3; toTaskId = 4 },
        @{ fromTaskId = 4; toTaskId = 5 },
        @{ fromTaskId = 5; toTaskId = 6 },
        @{ fromTaskId = 6; toTaskId = 7 }
    )
} | ConvertTo-Json -Depth 8

Write-Section "UrbanThread Clothing Brand - Real Workflow Demo"
Write-Host ("API Base URL: {0}" -f $baseUrl)
Write-Host "Auth Mode: role-based switching (EMPLOYEE / OPERATIONS / MANAGER)"

Write-Step "Create configurable workflow definition" "POST /workflows"
$workflow = Invoke-ApiPost "$baseUrl/workflows" $workflowBody "EMPLOYEE"
Write-Host ("[OK] workflowId={0}, name={1}" -f $workflow.workflowId, $workflow.workflowName) -ForegroundColor Green

Write-Section "Order Instance A - Summer Drop Hoodie"
Write-Step "Start workflow instance" ("POST /workflows/{0}/instances" -f $workflow.workflowId)
$stateA = Invoke-ApiPost ("$baseUrl/workflows/{0}/instances" -f $workflow.workflowId) $null "EMPLOYEE"
Write-State $stateA
$stateA = Execute-To-End $stateA "Order A"

Write-Section "Order Instance B - Weekend Denim Jacket"
Write-Step "Start workflow instance" ("POST /workflows/{0}/instances" -f $workflow.workflowId)
$stateB = Invoke-ApiPost ("$baseUrl/workflows/{0}/instances" -f $workflow.workflowId) $null "EMPLOYEE"
Write-State $stateB
$stateB = Execute-To-End $stateB "Order B"

$finalA = Invoke-ApiGet ("$baseUrl/instances/{0}" -f $stateA.instanceId) "MANAGER"
$finalB = Invoke-ApiGet ("$baseUrl/instances/{0}" -f $stateB.instanceId) "MANAGER"

Write-Section "Final Company Dashboard View"
$result = @(
    [PSCustomObject]@{
        OrderLabel   = "Order A"
        WorkflowId   = $finalA.workflowId
        InstanceId   = $finalA.instanceId
        FinalState   = $finalA.state
        CurrentTask  = $finalA.currentTaskId
        RetryCount   = $finalA.retryCount
        HistoryCount = $finalA.history.Count
    },
    [PSCustomObject]@{
        OrderLabel   = "Order B"
        WorkflowId   = $finalB.workflowId
        InstanceId   = $finalB.instanceId
        FinalState   = $finalB.state
        CurrentTask  = $finalB.currentTaskId
        RetryCount   = $finalB.retryCount
        HistoryCount = $finalB.history.Count
    }
)

$result | Format-Table -AutoSize | Out-String | Write-Host

Write-Host "Order A - task statuses" -ForegroundColor Cyan
$finalA.tasks |
    Select-Object taskId, taskName, status |
    Format-Table -AutoSize |
    Out-String |
    Write-Host

Write-Host "Order B - task statuses" -ForegroundColor Cyan
$finalB.tasks |
    Select-Object taskId, taskName, status |
    Format-Table -AutoSize |
    Out-String |
    Write-Host

Write-Host "Order A - recent timeline" -ForegroundColor Cyan
$eventNumberA = [Math]::Max(1, $finalA.history.Count - 5)
foreach ($event in ($finalA.history | Select-Object -Last 6)) {
    Write-Host ("{0,2}. {1}" -f $eventNumberA, $event)
    $eventNumberA++
}

Write-Host "Order B - recent timeline" -ForegroundColor Cyan
$eventNumberB = [Math]::Max(1, $finalB.history.Count - 5)
foreach ($event in ($finalB.history | Select-Object -Last 6)) {
    Write-Host ("{0,2}. {1}" -f $eventNumberB, $event)
    $eventNumberB++
}

if ($finalA.state -eq "COMPLETED" -and $finalB.state -eq "COMPLETED") {
    Write-Host "`nDemo verdict: SUCCESS (both clothing orders reached COMPLETED)." -ForegroundColor Green
} elseif ($finalA.state -eq "FAILED" -or $finalB.state -eq "FAILED") {
    Write-Host "`nDemo verdict: MIXED (at least one order failed after retry policy)." -ForegroundColor DarkYellow
} else {
    Write-Host "`nDemo verdict: IN PROGRESS (at least one order still RUNNING)." -ForegroundColor DarkYellow
}