$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080/api"
$line = "=" * 72
$username = "manager"
$password = "manager123"
$authToken = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$username`:$password"))
$headers = @{ Authorization = "Basic $authToken" }

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
        $null = Invoke-WebRequest -Uri "http://localhost:8080/h2-console" -UseBasicParsing -TimeoutSec 5
    } catch {
        Write-Host "Server is not reachable at http://localhost:8080" -ForegroundColor Red
        Write-Host "Start it first from workflow-execution-engine:" -ForegroundColor Red
        Write-Host "  .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
        exit 1
    }
}

function Invoke-ApiPost([string]$uri, [object]$body = $null) {
    if ($null -eq $body) {
        return Invoke-RestMethod -Method Post -Uri $uri -Headers $headers
    }
    return Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType "application/json" -Body $body
}

function Invoke-ApiGet([string]$uri) {
    return Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
}

Assert-ServerReady

$workflowBody = @{
    workflowName = "Purchase Request Workflow (Demo)"
    tasks = @(
        @{ taskId = 1; taskName = "Submit Request"; assignedRole = "EMPLOYEE" },
        @{ taskId = 2; taskName = "Manager Approval"; assignedRole = "MANAGER" },
        @{ taskId = 3; taskName = "Fulfill Request"; assignedRole = "OPERATIONS" }
    )
    transitions = @(
        @{ fromTaskId = 1; toTaskId = 2 },
        @{ fromTaskId = 2; toTaskId = 3 }
    )
} | ConvertTo-Json -Depth 6

Write-Section "Workflow Execution Engine Demo"
Write-Host ("API Base URL: {0}" -f $baseUrl)
Write-Host ("Auth User: {0}" -f $username)

Write-Step "Create workflow" "POST /workflows"
$workflow = Invoke-ApiPost "$baseUrl/workflows" $workflowBody
Write-Host ("[OK] workflowId={0}, name={1}" -f $workflow.workflowId, $workflow.workflowName) -ForegroundColor Green

Write-Step "Start instance" ("POST /workflows/{0}/instances" -f $workflow.workflowId)
$state = Invoke-ApiPost ("$baseUrl/workflows/{0}/instances" -f $workflow.workflowId)
Write-State $state

Write-Step "Execute first task" ("POST /instances/{0}/execute" -f $state.instanceId)
$state = Invoke-ApiPost ("$baseUrl/instances/{0}/execute" -f $state.instanceId)
Write-State $state

if ($state.state -eq "RUNNING" -and $state.currentTaskId -eq 2) {
    Write-Step "Approve manager task" ("POST /instances/{0}/approve" -f $state.instanceId)
    $state = Invoke-ApiPost ("$baseUrl/instances/{0}/approve" -f $state.instanceId)
    Write-State $state
} else {
    Write-Host "[INFO] Approval step skipped (instance not waiting at task 2)." -ForegroundColor DarkYellow
}

$loop = 0
while ($state.state -eq "RUNNING") {
    $loop++
    $currentTask = $state.tasks | Where-Object { $_.taskId -eq $state.currentTaskId } | Select-Object -First 1
    $hasFailedTask = ($null -ne $currentTask -and $currentTask.status -eq "FAILURE")

    if ($hasFailedTask) {
        Write-Step ("Retry failed task (attempt cycle {0})" -f $loop) ("POST /instances/{0}/retry" -f $state.instanceId)
        $state = Invoke-ApiPost ("$baseUrl/instances/{0}/retry" -f $state.instanceId)
    } else {
        Write-Step ("Execute current task (cycle {0})" -f $loop) ("POST /instances/{0}/execute" -f $state.instanceId)
        $state = Invoke-ApiPost ("$baseUrl/instances/{0}/execute" -f $state.instanceId)
    }

    Write-State $state
}

$final = Invoke-ApiGet ("$baseUrl/instances/{0}" -f $state.instanceId)

Write-Section "Final Result"
$result = [PSCustomObject]@{
    WorkflowId   = $final.workflowId
    InstanceId   = $final.instanceId
    FinalState   = $final.state
    CurrentTask  = $final.currentTaskId
    RetryCount   = $final.retryCount
    HistoryCount = $final.history.Count
}
$result | Format-Table -AutoSize | Out-String | Write-Host

Write-Host "Task Statuses" -ForegroundColor Cyan
$final.tasks |
    Select-Object taskId, taskName, status |
    Format-Table -AutoSize |
    Out-String |
    Write-Host

Write-Host "Recent Timeline (last 6 events)" -ForegroundColor Cyan
$eventNumber = [Math]::Max(1, $final.history.Count - 5)
foreach ($event in ($final.history | Select-Object -Last 6)) {
    Write-Host ("{0,2}. {1}" -f $eventNumber, $event)
    $eventNumber++
}

if ($final.state -eq "COMPLETED") {
    Write-Host "`nDemo verdict: SUCCESS (workflow reached COMPLETED)." -ForegroundColor Green
} elseif ($final.state -eq "FAILED") {
    Write-Host "`nDemo verdict: FAILED after retry limit (valid failure scenario)." -ForegroundColor DarkYellow
} else {
    Write-Host "`nDemo verdict: IN PROGRESS (workflow still RUNNING)." -ForegroundColor DarkYellow
}
