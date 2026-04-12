$ErrorActionPreference = "Stop"

$script:hasErrors = $false

function Write-Info([string]$message) {
    Write-Host "[INFO] $message" -ForegroundColor Cyan
}

function Write-Ok([string]$message) {
    Write-Host "[OK]   $message" -ForegroundColor Green
}

function Write-Fail([string]$message) {
    Write-Host "[FAIL] $message" -ForegroundColor Red
    $script:hasErrors = $true
}

function Get-MajorVersionFromSemVer([string]$value) {
    if ($value -match '^(?<major>\d+)(\.\d+)?(\.\d+)?') {
        return [int]$Matches['major']
    }
    throw "Unable to parse semantic version from '$value'"
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

    return $null
}

function Get-JavaMajorVersion {
    $javaCommand = Get-JavaCommand
    if (-not $javaCommand) {
        throw "Java is not installed or not available on PATH/JAVA_HOME. Required: Java JDK 25+"
    }

    $versionOutput = (& $javaCommand -version 2>&1 | Select-Object -First 1)
    if ($versionOutput -match '"(?<major>\d+)(\.\d+)?') {
        return [int]$Matches['major']
    }
    throw "Unable to parse Java version from output: $versionOutput"
}

function Test-CommandExists([string]$name) {
    return [bool](Get-Command $name -ErrorAction SilentlyContinue)
}

Write-Info "Checking required tools for workflow-execution-engine"

try {
    $javaMajor = Get-JavaMajorVersion
    if ($javaMajor -lt 25) {
        Write-Fail "Java 25+ is required, found Java $javaMajor"
    } else {
        Write-Ok "Java version is compatible ($javaMajor+)"
    }
} catch {
    Write-Fail $_.Exception.Message
}

if (-not (Test-CommandExists "node")) {
    Write-Fail "Node.js is not installed or not available on PATH. Required: Node 20+"
} else {
    try {
        $nodeVersionRaw = (& node -v).Trim().TrimStart('v')
        $nodeMajor = Get-MajorVersionFromSemVer $nodeVersionRaw
        if ($nodeMajor -lt 20) {
            Write-Fail "Node 20+ is required, found Node $nodeMajor"
        } else {
            Write-Ok "Node version is compatible ($nodeMajor+)"
        }
    } catch {
        Write-Fail $_.Exception.Message
    }
}

if (-not (Test-CommandExists "npm")) {
    Write-Fail "npm is not installed or not available on PATH. Install Node.js 20+ to include npm."
} else {
    try {
        $npmVersionRaw = (& npm -v).Trim()
        $npmMajor = Get-MajorVersionFromSemVer $npmVersionRaw
        Write-Ok "npm detected ($npmMajor.x)"
    } catch {
        Write-Fail $_.Exception.Message
    }
}

if (-not (Test-CommandExists "git")) {
    Write-Fail "Git is not installed or not available on PATH."
} else {
    Write-Ok "Git detected"
}

if ($PSVersionTable.PSVersion.Major -lt 7) {
    Write-Fail "PowerShell 7+ is recommended; found PowerShell $($PSVersionTable.PSVersion)"
} else {
    Write-Ok "PowerShell version is compatible ($($PSVersionTable.PSVersion))"
}

Write-Host ""
if ($script:hasErrors) {
    Write-Host "Requirements check failed. Install/update missing tools, then run this script again." -ForegroundColor Red
    exit 1
}

Write-Host "All requirements satisfied. You're ready to run the project." -ForegroundColor Green
exit 0
