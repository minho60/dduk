$ErrorActionPreference = "Stop"

$workspaceRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $workspaceRoot "backend"
$frontendDir = Join-Path $workspaceRoot "frontend"
$tmpRootDir = Join-Path $workspaceRoot ".local-run"
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$tmpDir = Join-Path $tmpRootDir $runId
$backendLog = Join-Path $tmpDir "backend.out.log"
$backendErrLog = Join-Path $tmpDir "backend.err.log"
$frontendLog = Join-Path $tmpDir "frontend.out.log"
$frontendErrLog = Join-Path $tmpDir "frontend.err.log"
$backendUrl = "http://localhost:8080/"
$frontendUrl = "http://localhost:5500/"

function Test-PortListening {
    param([int]$Port)

    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $conn
}

function Test-HttpReady {
    param([string]$Url)

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

function Wait-HttpReady {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpReady -Url $Url) {
            return $true
        }
        Start-Sleep -Seconds 2
    }

    return $false
}

function Get-PythonCommand {
    if (Get-Command python -ErrorAction SilentlyContinue) {
        return "python"
    }

    if (Get-Command py -ErrorAction SilentlyContinue) {
        return "py"
    }

    throw "Python executable not found. Install python or py first."
}

function Start-DetachedCommand {
    param(
        [string]$WorkingDirectory,
        [string]$CommandLine
    )

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "cmd.exe"
    $psi.Arguments = "/c $CommandLine"
    $psi.WorkingDirectory = $WorkingDirectory
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    $process.Start() | Out-Null

    return $process
}

try {
    if (-not (Test-Path $backendDir)) {
        throw "backend directory not found: $backendDir"
    }

    if (-not (Test-Path $frontendDir)) {
        throw "frontend directory not found: $frontendDir"
    }

    if (-not (Test-Path $tmpRootDir)) {
        New-Item -ItemType Directory -Path $tmpRootDir | Out-Null
    }

    if (-not (Test-Path $tmpDir)) {
        New-Item -ItemType Directory -Path $tmpDir | Out-Null
    }

    New-Item -ItemType File -Path $backendLog -Force | Out-Null
    New-Item -ItemType File -Path $backendErrLog -Force | Out-Null
    New-Item -ItemType File -Path $frontendLog -Force | Out-Null
    New-Item -ItemType File -Path $frontendErrLog -Force | Out-Null

    $pythonCommand = Get-PythonCommand

    $backendAlreadyRunning = (Test-PortListening -Port 8080) -and (Test-HttpReady -Url $backendUrl)
    $frontendAlreadyRunning = (Test-PortListening -Port 5500) -and (Test-HttpReady -Url $frontendUrl)

    $backendProcess = $null
    $frontendProcess = $null

    if (-not $backendAlreadyRunning) {
        if (Test-PortListening -Port 8080) {
            throw "Port 8080 is busy but backend is not responding. Stop the old process first."
        }

        $backendCommand = "cd /d ""$backendDir"" && call gradlew.bat bootRun 1>""$backendLog"" 2>""$backendErrLog"""
        $backendProcess = Start-DetachedCommand -WorkingDirectory $backendDir -CommandLine $backendCommand
    }

    if (-not $frontendAlreadyRunning) {
        if (Test-PortListening -Port 5500) {
            throw "Port 5500 is busy but frontend is not responding. Stop the old process first."
        }

        $frontendCommand = "cd /d ""$frontendDir"" && ""$pythonCommand"" -m http.server 5500 1>""$frontendLog"" 2>""$frontendErrLog"""
        $frontendProcess = Start-DetachedCommand -WorkingDirectory $frontendDir -CommandLine $frontendCommand
    }

    $backendReady = if ($backendAlreadyRunning) { $true } else { Wait-HttpReady -Url $backendUrl -TimeoutSeconds 90 }
    $frontendReady = if ($frontendAlreadyRunning) { $true } else { Wait-HttpReady -Url $frontendUrl -TimeoutSeconds 30 }

    if (-not $backendReady) {
        throw "Backend did not become ready. Check logs: $backendLog / $backendErrLog"
    }

    if (-not $frontendReady) {
        throw "Frontend did not become ready. Check logs: $frontendLog / $frontendErrLog"
    }

    if ($backendAlreadyRunning) {
        $backendStatus = "reused existing server"
    } else {
        $backendStatus = "started new process (PID: $($backendProcess.Id))"
    }

    if ($frontendAlreadyRunning) {
        $frontendStatus = "reused existing server"
    } else {
        $frontendStatus = "started new process (PID: $($frontendProcess.Id))"
    }

    cmd.exe /c start "" $frontendUrl | Out-Null

    Write-Host ""
    Write-Host "Backend: $backendUrl"
    Write-Host "Frontend: $frontendUrl"
    Write-Host "Backend status: $backendStatus"
    Write-Host "Frontend status: $frontendStatus"
    Write-Host "Logs:"
    Write-Host "  $backendLog"
    Write-Host "  $backendErrLog"
    Write-Host "  $frontendLog"
    Write-Host "  $frontendErrLog"
    exit 0
} catch {
    Write-Host ""
    Write-Host "start-local failed" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "Log folder: $tmpDir"

    if (Test-Path $backendErrLog) {
        Write-Host ""
        Write-Host "[backend.err.log]"
        Get-Content $backendErrLog -Tail 20 -ErrorAction SilentlyContinue
    }

    if (Test-Path $frontendErrLog) {
        Write-Host ""
        Write-Host "[frontend.err.log]"
        Get-Content $frontendErrLog -Tail 20 -ErrorAction SilentlyContinue
    }

    exit 1
}
