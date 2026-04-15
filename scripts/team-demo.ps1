$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$uiRoot = Join-Path $repoRoot "ui"
$backendHealthUrl = "http://localhost:8080/actuator/health"
$uiUrl = "http://localhost:5173"

function Write-Info([string]$message) {
    Write-Host "[INFO] $message" -ForegroundColor Cyan
}

function Write-Ok([string]$message) {
    Write-Host "[OK]   $message" -ForegroundColor Green
}

function Write-Warn([string]$message) {
    Write-Host "[WARN] $message" -ForegroundColor Yellow
}

function Require-Command([string]$name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Required command '$name' is not available on PATH."
    }
}

function Get-JavaCommand {
    if ($env:JAVA_HOME) {
        $javaFromHome = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $javaFromHome) {
            return $javaFromHome
        }
    }

    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        return $javaCmd.Source
    }

    throw "Java is not installed or not available on PATH/JAVA_HOME."
}

function Get-JavaMajorVersion {
    $javaCommand = Get-JavaCommand
    $versionOutput = (& $javaCommand -version 2>&1 | Select-Object -First 1)
    if ($versionOutput -match '"(?<v>\d+)(\.\d+)?') {
        return [int]$Matches['v']
    }
    throw "Unable to parse Java version from output: $versionOutput"
}

function Get-NodeMajorVersion {
    $nodeVersion = (& node -v).Trim()
    if ($nodeVersion -match '^v(?<v>\d+)') {
        return [int]$Matches['v']
    }
    throw "Unable to parse Node version from output: $nodeVersion"
}

function Test-Endpoint([string]$url) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2
        return ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500)
    } catch {
        return $false
    }
}

function Wait-ForBackend {
    param(
        [int]$TimeoutSeconds = 120
    )

    $startedAt = Get-Date
    while (((Get-Date) - $startedAt).TotalSeconds -lt $TimeoutSeconds) {
        if (Test-Endpoint $backendHealthUrl) {
            Write-Ok "Backend is reachable at $backendHealthUrl"
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "Backend did not become ready within $TimeoutSeconds seconds."
}

Write-Info "Checking local requirements"
Require-Command node
Require-Command npm

$javaMajor = Get-JavaMajorVersion
if ($javaMajor -lt 25) {
    throw "Java 25+ is required, found Java $javaMajor"
}
Write-Ok "Java version is compatible ($javaMajor+)"

$nodeMajor = Get-NodeMajorVersion
if ($nodeMajor -lt 20) {
    throw "Node 20+ is required, found Node $nodeMajor"
}
Write-Ok "Node version is compatible ($nodeMajor+)"

Write-Info "Ensuring backend is running"
if (-not (Test-Endpoint $backendHealthUrl)) {
    Write-Info "Starting backend in a new PowerShell window"
    Start-Process -FilePath "pwsh" -WorkingDirectory $repoRoot -ArgumentList @(
        "-NoExit",
        "-Command",
        ".\\mvnw.cmd spring-boot:run"
    ) | Out-Null

    Wait-ForBackend -TimeoutSeconds 120
} else {
    Write-Ok "Backend already running"
}

Write-Info "Ensuring UI dependencies"
if (-not (Test-Path (Join-Path $uiRoot "node_modules"))) {
    Push-Location $uiRoot
    try {
        npm install | Out-Host
    } finally {
        Pop-Location
    }
}
Write-Ok "UI dependencies ready"

Write-Info "Ensuring UI dev server is running"
if (-not (Test-Endpoint $uiUrl)) {
    Write-Info "Starting UI in a new PowerShell window"
    Start-Process -FilePath "pwsh" -WorkingDirectory $uiRoot -ArgumentList @(
        "-NoExit",
        "-Command",
        "npm run dev"
    ) | Out-Null
} else {
    Write-Ok "UI already running"
}

Write-Info "Running API demo scenario (clothing brand workflow)"
& (Join-Path $PSScriptRoot "demo-run-clothing-brand.ps1")

Write-Host ""
Write-Host "================ TEAM DEMO READY ================" -ForegroundColor DarkCyan
Write-Host "UI        : http://localhost:5173" -ForegroundColor White
Write-Host "API       : http://localhost:8080/api" -ForegroundColor White
Write-Host "Swagger   : http://localhost:8080/swagger-ui/index.html" -ForegroundColor White
Write-Host "Health    : http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host "Credentials: manager / manager123" -ForegroundColor White
Write-Host "=================================================" -ForegroundColor DarkCyan
