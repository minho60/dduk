$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot ".env"
$serviceStateFile = Join-Path (Join-Path $PSScriptRoot ".local-run") "current-services.json"

function Get-EnvFileValue {
    param([string]$FilePath, [string]$Key)

    if (-not (Test-Path $FilePath)) {
        return $null
    }

    foreach ($rawLine in Get-Content $FilePath) {
        $line = $rawLine.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            continue
        }

        $index = $line.IndexOf("=")
        $currentKey = $line.Substring(0, $index).Trim()
        if ($currentKey -ceq $Key) {
            $value = $line.Substring($index + 1).Trim()
            return ($value -replace "^['`"]|['`"]$")
        }
    }

    return $null
}

function Get-RecordedServiceState {
    param([string]$FilePath)

    if (-not (Test-Path $FilePath)) {
        return $null
    }

    try {
        return Get-Content $FilePath -Raw | ConvertFrom-Json
    } catch {
        Write-Host "Could not parse service state file: $FilePath" -ForegroundColor Yellow
        return $null
    }
}

function Get-RecordedService {
    param(
        $State,
        [int]$Port
    )

    if ($null -eq $State -or $null -eq $State.services) {
        return $null
    }

    return $State.services | Where-Object { $_.port -eq $Port } | Select-Object -First 1
}

$aiPortEnv = Get-EnvFileValue -FilePath $envFile -Key "AI_SERVER_PORT"
$aiPort = if ($aiPortEnv) { [int]$aiPortEnv } else { 5000 }

$rpaPortEnv = Get-EnvFileValue -FilePath $envFile -Key "RPA_SERVER_PORT"
$rpaPort = if ($rpaPortEnv) { [int]$rpaPortEnv } else { 5050 }

$portsToKill = @(8080, 5500, $aiPort, $rpaPort)
$processNames = @("Backend", "Frontend", "AI Server", "RPA Server")
$serviceState = Get-RecordedServiceState -FilePath $serviceStateFile

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Stopping DDUK ERP Background Services" -ForegroundColor Cyan
Write-Host "=========================================================="

if ($null -eq $serviceState) {
    Write-Host "No current-services.json found. Refusing to stop unknown processes by port only." -ForegroundColor Yellow
    Write-Host "Run ai-all-start.bat first or stop the process manually after confirming the PID." -ForegroundColor Yellow
    exit 1
}

for ($i = 0; $i -lt $portsToKill.Length; $i++) {
    $port = $portsToKill[$i]
    $name = $processNames[$i]
    $recordedService = Get-RecordedService -State $serviceState -Port $port

    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if ($null -eq $conn) {
        Write-Host "[$name] Port $port is already free (Not running)." -ForegroundColor DarkGray
        continue
    }

    $pidToKill = $conn.OwningProcess
    if ($null -eq $recordedService -or $null -eq $recordedService.pid) {
        Write-Host "[$name] Port $port is in use by PID $pidToKill, but there is no matching dduk service record. Skipping." -ForegroundColor Yellow
        continue
    }

    if ([int]$recordedService.pid -ne [int]$pidToKill) {
        Write-Host "[$name] Port $port is in use by PID $pidToKill, but recorded dduk PID is $($recordedService.pid). Skipping." -ForegroundColor Yellow
        continue
    }

    Write-Host "[$name] Port $port matches recorded dduk PID $pidToKill. Stopping process..."
    try {
        Stop-Process -Id $pidToKill -Force -ErrorAction Stop
        Write-Host "  -> [$name] Successfully stopped." -ForegroundColor Green
    } catch {
        Write-Host "  -> [$name] Failed to stop PID $pidToKill. You may need Administrator privileges." -ForegroundColor Red
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " All specified services stop attempt completed." -ForegroundColor Cyan
Write-Host "=========================================================="
