param(
    [string]$LogDir = "./logs/mobile",
    [string]$PackageName = "com.remotedexter.mobile",
    [string]$MetricsFile = "./logs/mobile/block_h_metrics.json",
    [string]$ResultFile = "./logs/mobile/block_h_result.json",
    [switch]$StartLogcat,
    [switch]$StopLogcat,
    [switch]$EvaluateOnly
)

$ErrorActionPreference = "Stop"

$thresholds = [ordered]@{
    max_reconnect_attempts = 3
    max_frame_drop_percent = 5
    max_session_setup_ms = 4000
}

function Ensure-Directory {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        New-Item -Path $Path -ItemType Directory -Force | Out-Null
    }
}

function Resolve-PathSafe {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return (Join-Path (Get-Location) $Path)
}

function Test-AdbAvailable {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    return $null -ne $adb
}

function Start-LogcatCapture {
    param(
        [string]$OutDir,
        [string]$OutPackage
    )

    Ensure-Directory -Path $OutDir

    if (-not (Test-AdbAvailable)) {
        throw "adb not found in PATH"
    }

    $logFile = Join-Path $OutDir ("block_h_logcat_" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".txt")
    $pidFile = Join-Path $OutDir "block_h_logcat.pid"

    adb logcat -c | Out-Null
    $process = Start-Process -FilePath "adb" -ArgumentList @("logcat", "-v", "time") -RedirectStandardOutput $logFile -NoNewWindow -PassThru
    Set-Content -Path $pidFile -Value $process.Id

    Write-Host "Started logcat capture"
    Write-Host "PID: $($process.Id)"
    Write-Host "Log: $logFile"
}

function Stop-LogcatCapture {
    param([string]$OutDir)

    $pidFile = Join-Path $OutDir "block_h_logcat.pid"
    if (-not (Test-Path $pidFile)) {
        Write-Host "No active logcat PID file found"
        return
    }

    $pid = Get-Content -Path $pidFile | Select-Object -First 1
    if ($pid) {
        try {
            Stop-Process -Id ([int]$pid) -Force -ErrorAction Stop
            Write-Host "Stopped logcat PID: $pid"
        } catch {
            Write-Host "Could not stop PID $pid (may already be stopped)"
        }
    }

    Remove-Item -Path $pidFile -Force -ErrorAction SilentlyContinue
}

function Load-Metrics {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Metrics file not found: $Path"
    }

    return Get-Content -Path $Path -Raw | ConvertFrom-Json
}

function Evaluate-Run {
    param(
        $Metrics,
        [hashtable]$Limits
    )

    $failures = New-Object System.Collections.Generic.List[string]

    if ([int]$Metrics.session_setup_ms -gt [int]$Limits.max_session_setup_ms) {
        $failures.Add("session_setup_ms exceeded: $($Metrics.session_setup_ms) > $($Limits.max_session_setup_ms)")
    }

    if ([int]$Metrics.reconnect_attempts -gt [int]$Limits.max_reconnect_attempts) {
        $failures.Add("reconnect_attempts exceeded: $($Metrics.reconnect_attempts) > $($Limits.max_reconnect_attempts)")
    }

    if ([double]$Metrics.frame_drop_percent -gt [double]$Limits.max_frame_drop_percent) {
        $failures.Add("frame_drop_percent exceeded: $($Metrics.frame_drop_percent) > $($Limits.max_frame_drop_percent)")
    }

    $requiredBools = @(
        "manual_reconnect_pass",
        "automatic_reconnect_pass",
        "clipboard_bidirectional_pass",
        "multimonitor_pass",
        "network_variability_pass",
        "teardown_clean_pass",
        "loop_safety_pass"
    )

    foreach ($name in $requiredBools) {
        if (-not $Metrics.PSObject.Properties.Name.Contains($name)) {
            $failures.Add("missing metric: $name")
            continue
        }
        if (-not [bool]$Metrics.$name) {
            $failures.Add("required check failed: $name")
        }
    }

    return [ordered]@{
        pass = ($failures.Count -eq 0)
        failures = $failures
        thresholds = $Limits
        evaluated_at_utc = (Get-Date).ToUniversalTime().ToString("o")
    }
}

$resolvedLogDir = Resolve-PathSafe -Path $LogDir
$resolvedMetrics = Resolve-PathSafe -Path $MetricsFile
$resolvedResult = Resolve-PathSafe -Path $ResultFile

Ensure-Directory -Path $resolvedLogDir

if ($StartLogcat) {
    Start-LogcatCapture -OutDir $resolvedLogDir -OutPackage $PackageName
}

if ($StopLogcat) {
    Stop-LogcatCapture -OutDir $resolvedLogDir
}

if ($EvaluateOnly -or (-not $StartLogcat -and -not $StopLogcat)) {
    $metrics = Load-Metrics -Path $resolvedMetrics
    $result = Evaluate-Run -Metrics $metrics -Limits $thresholds
    $json = $result | ConvertTo-Json -Depth 6
    $resultDir = Split-Path -Parent $resolvedResult
    Ensure-Directory -Path $resultDir
    Set-Content -Path $resolvedResult -Value $json -Encoding UTF8

    if (-not $result.pass) {
        Write-Host "Block H evaluation: FAIL"
        $result.failures | ForEach-Object { Write-Host " - $_" }
        exit 2
    }

    Write-Host "Block H evaluation: PASS"
}
