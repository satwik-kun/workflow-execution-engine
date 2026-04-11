# Local Setup Requirements

Use this checklist before running the project on a new machine.

## Required Software

- Java JDK 17 or newer
- Node.js 20 or newer (includes npm)
- Git
- PowerShell 7+ recommended on Windows

## Verify Installations

Run these commands in a terminal:

```powershell
java -version
node -v
npm -v
git --version
```

Expected:

- Java version should be 17+
- Node should be 20+

## Clone And Start

From a fresh clone:

```powershell
cd workflow-execution-engine
.\scripts\team-demo.ps1
```

What this script does:

- checks required tools
- starts backend if not already running
- starts UI if not already running
- runs an end-to-end API demo flow
- prints team demo URLs and credentials

## Manual Run (Optional)

Backend:

```powershell
.\mvnw.cmd spring-boot:run
```

UI (second terminal):

```powershell
cd ui
npm install
npm run dev
```

## Team Demo URLs

- UI: http://localhost:5173
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health

## Default Credentials

- manager / manager123
- employee / employee123
- operations / operations123

## Common Local Issues

If backend startup fails due to DB lock:

```powershell
Get-CimInstance Win32_Process -Filter "name = 'java.exe'" |
Where-Object { $_.CommandLine -like '*workflow-execution-engine*' } |
ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

Then start again:

```powershell
.\mvnw.cmd spring-boot:run
```
