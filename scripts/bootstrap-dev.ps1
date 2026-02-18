<#
    bootstrap-dev.ps1

    Purpose: developer bootstrap script to create a deterministic dev environment
    and run the fast-path described in README-dev.md.

    This is a stub. It will later:
      - pin the JDK version and optionally install or warn if mismatched
      - verify the Gradle wrapper and/or prompt to install it
      - run `:core:test` and `:cli:run` as the recommended fast path

    Current contents are placeholders to be implemented during Phase 1.
#>

Write-Host "[bootstrap] developer bootstrap script placeholder"

## TODO: JDK pinning
# e.g. verify `java -version` matches pinned version

## TODO: Gradle wrapper verification
# e.g. verify `gradlew` exists and is executable

## TODO: run core tests and CLI
# ./gradlew :core:test
# ./gradlew :cli:run

# -----------------------------
# Expanded bootstrap script
# -----------------------------

# Pinned/toolchain variables (update per Master Constitution)
$PinnedJavaMajor = 17
$PinnedGradleWrapperVersion = "8.2.1"  # adjust to pinned version in repo
$PinnedKotlinVersion = "1.9.10"

# Resolve project root relative to script location so the script works when invoked from anywhere
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..")
Write-Host "[bootstrap] Project root: $ProjectRoot"

function Check-KotlinPlugin {
  Write-Host "[bootstrap] Verifying Kotlin plugin pin..."
  $settingsPath = Join-Path -Path $ProjectRoot -ChildPath "settings.gradle.kts"
  if (Test-Path $settingsPath) {
    $settings = Get-Content $settingsPath -Raw -ErrorAction SilentlyContinue
    $m = [regex]::Match($settings, 'org\.jetbrains\.kotlin\.jvm"\)\s*version\s*"([0-9.\.]+)')
    if ($m.Success) {
      $found = $m.Groups[1].Value
      Write-Host "[bootstrap] settings.gradle.kts pins Kotlin version: $found"
      if ($found -eq $PinnedKotlinVersion) {
        Write-Host "[bootstrap] Kotlin version matches pinned version: $PinnedKotlinVersion"
        return @{ Found = $true; Match = $true; Version = $found }
      } else {
        Write-Host "[bootstrap][fatal] Kotlin pinned in settings.gradle.kts ($found) does not match required $PinnedKotlinVersion" -ForegroundColor Red
        return @{ Found = $true; Match = $false; Version = $found }
      }
    } else {
      Write-Host "[bootstrap][warn] settings.gradle.kts does not explicitly pin Kotlin in pluginManagement." -ForegroundColor Yellow
      # As fallback, scan subproject build files for explicit Kotlin plugin versions
      $mismatches = @()
      Get-ChildItem -Path $ProjectRoot -Include build.gradle.kts -Recurse -ErrorAction SilentlyContinue | ForEach-Object {
        $content = Get-Content $_.FullName -ErrorAction SilentlyContinue
        if ($content -match 'kotlin\("jvm"\)\s*version\s*"([0-9\.]+)"') {
          $ver = $Matches[1]
          $mismatches += "$($_.FullName) -> $ver"
        }
      }
      if ($mismatches.Count -gt 0) {
        Write-Host "[bootstrap][fatal] Found explicit Kotlin plugin versions in subprojects:" -ForegroundColor Red
        $mismatches | ForEach-Object { Write-Host "  $_" }
        return @{ Found = $true; Match = $false; Details = $mismatches }
      }
      return @{ Found = $true; Match = $null }
    }
  } else {
    Write-Host "[bootstrap][warn] settings.gradle.kts not found; cannot verify Kotlin pin." -ForegroundColor Yellow
    return @{ Found = $false }
  }
}

function Check-JavaVersion {
  Write-Host "[bootstrap] Checking Java version..."
  try {
    $verOut = & java -XshowSettings:properties -version 2>&1
  } catch {
    Write-Host "[bootstrap][error] 'java' not found in PATH." -ForegroundColor Red
    return @{ Found = $false }
  }
  # Attempt to parse major version from output lines
  # Attempt to parse major version from java properties output
  $propsLine = $verOut | Select-String "java.class.version" -SimpleMatch | Select-Object -First 1
  if ($propsLine) {
    $pv = [regex]::Match($propsLine.ToString(), "(\d+(?:\.\d+)?)")
    if ($pv.Success) {
      $classVersion = [int]$pv.Groups[1].Value
      # class file version to Java major approximation: JavaMajor = classVersion - 44
      $major = $classVersion - 44
      Write-Host "[bootstrap] Detected java.class.version: $classVersion => Java major approx: $major"
      return @{ Found = $true; Major = $major; Raw = $propsLine.ToString() }
    }
  }

  # Fallback: parse typical 'java -version' output like 'openjdk version "17.0.1"'
  $firstLine = $verOut | Select-String "version" -SimpleMatch | Select-Object -First 1
  if (-not $firstLine) {
    $firstLine = $verOut | Select-String "openjdk" -SimpleMatch | Select-Object -First 1
  }
  $versionString = if ($firstLine) { $firstLine.ToString() } else { $verOut -join " `n " }
  $m = [regex]::Match($versionString, "(\d+)(?:\.\d+)?")
  if ($m.Success) {
    $major = [int]$m.Groups[1].Value
    Write-Host "[bootstrap] Detected Java major version: $major"
    return @{ Found = $true; Major = $major; Raw = $versionString }
  } else {
    Write-Host "[bootstrap][warn] Could not parse Java version output:" -ForegroundColor Yellow
    Write-Host $versionString
    return @{ Found = $true; Major = 0; Raw = $versionString }
  }
}

function Check-GradleWrapper {
  Write-Host "[bootstrap] Checking Gradle wrapper..."
  $wrapperScript = Get-ChildItem -Path $ProjectRoot -Filter "gradlew*" -File -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($null -eq $wrapperScript) {
    Write-Host "[bootstrap][warn] Gradle wrapper not found in project root." -ForegroundColor Yellow
    return @{ Found = $false }
  }
  Write-Host "[bootstrap] Found wrapper: $($wrapperScript.Name)"
  $propsPath = Join-Path -Path $ProjectRoot -ChildPath "gradle/wrapper/gradle-wrapper.properties"
  if (Test-Path $propsPath) {
    $props = Get-Content $propsPath -ErrorAction SilentlyContinue
    $distLine = $props | Select-String "distributionUrl" | Select-Object -First 1
    if ($distLine) {
      $dist = ($distLine -split "=")[1].Trim()
      Write-Host "[bootstrap] gradle-wrapper distributionUrl: $dist"
      if ($dist -match "$PinnedGradleWrapperVersion") {
        Write-Host "[bootstrap] Gradle wrapper version appears to match pinned version: $PinnedGradleWrapperVersion"
        return @{ Found = $true; VersionMatch = $true; Distribution = $dist }
      } else {
        Write-Host "[bootstrap][warn] Gradle wrapper distribution does not match pinned version ($PinnedGradleWrapperVersion)." -ForegroundColor Yellow
        return @{ Found = $true; VersionMatch = $false; Distribution = $dist }
      }
    } else {
      Write-Host "[bootstrap][warn] gradle-wrapper.properties missing distributionUrl line." -ForegroundColor Yellow
      return @{ Found = $true; VersionMatch = $false }
    }
  } else {
    Write-Host "[bootstrap][warn] gradle/wrapper/gradle-wrapper.properties not found." -ForegroundColor Yellow
    return @{ Found = $true; VersionMatch = $false }
  }
}

function Run-FastPath {
  param(
    [string]$GradleCmd
  )
  Write-Host "[bootstrap] Running fast-path: core tests then cli run"
  $results = @{ CoreTest = $null; CliRun = $null }
  $coreCmd = "$GradleCmd :core:test"
  Write-Host "[bootstrap] Executing: $coreCmd"
  # Ensure the gradle command or wrapper exists before invoking
  $canRun = $false
  if (Test-Path $GradleCmd) { $canRun = $true }
  else { if (Get-Command $GradleCmd -ErrorAction SilentlyContinue) { $canRun = $true } }
  if (-not $canRun) {
    Write-Host "[bootstrap][error] Gradle command not found: $GradleCmd" -ForegroundColor Red
    $results.CoreTest = 127
    return $results
  }

  & $GradleCmd ":core:test"
  $coreCode = $LASTEXITCODE
  $results.CoreTest = $coreCode

  if ($coreCode -ne 0) {
    Write-Host "[bootstrap][error] :core:test failed with exit code $coreCode" -ForegroundColor Red
    return $results
  }

  Write-Host "[bootstrap] :core:test succeeded. Running :cli:run"
  & $GradleCmd ":cli:run"
  $cliCode = $LASTEXITCODE
  $results.CliRun = $cliCode
  if ($cliCode -ne 0) {
    Write-Host "[bootstrap][error] :cli:run failed with exit code $cliCode" -ForegroundColor Red
  } else {
    Write-Host "[bootstrap] :cli:run succeeded." -ForegroundColor Green
  }
  return $results
}

# -----------------------------
# Main script flow
# -----------------------------

Write-Host "[bootstrap] Starting RemoteDexter-v2 developer bootstrap"

Write-Host "[bootstrap] Enforcing pinned toolchain requirements per Master Constitution"

$javaCheck = Check-JavaVersion
if (-not $javaCheck.Found) {
  Write-Host "[bootstrap][fatal] Java not found. Please install JDK $PinnedJavaMajor or set JAVA_HOME to a JDK $PinnedJavaMajor installation." -ForegroundColor Red
  exit 2
}
if ($javaCheck.Major -ne 0 -and $javaCheck.Major -ne $PinnedJavaMajor) {
  Write-Host "[bootstrap][fatal] Java major version mismatch. Pinned=$PinnedJavaMajor, found=$($javaCheck.Major)." -ForegroundColor Red
  Write-Host "[bootstrap] Java raw output:`n$($javaCheck.Raw)"
  Write-Host "[bootstrap] Install the pinned JDK or update the Master Constitution if a different JDK is required." -ForegroundColor Red
  exit 3
}

# Verify Kotlin plugin pin
$kotlinCheck = Check-KotlinPlugin
if (-not $kotlinCheck.Found) {
    Write-Host "[bootstrap][fatal] Could not verify Kotlin plugin pin in settings.gradle.kts." -ForegroundColor Red
    exit 6
}
if ($kotlinCheck.Match -eq $false) {
    Write-Host "[bootstrap][fatal] Kotlin plugin pin mismatch. Fix settings.gradle.kts or update Master Constitution." -ForegroundColor Red
    exit 7
}

$gradleCheck = Check-GradleWrapper
if (-not $gradleCheck.Found) {
  Write-Host "[bootstrap][fatal] Gradle wrapper not found. The repository must include the Gradle wrapper per the Master Constitution." -ForegroundColor Red
  exit 4
}
if (-not $gradleCheck.VersionMatch) {
  Write-Host "[bootstrap][fatal] Gradle wrapper distribution does not match pinned version ($PinnedGradleWrapperVersion)." -ForegroundColor Red
  Write-Host "[bootstrap] Found distribution: $($gradleCheck.Distribution)"
  Write-Host "[bootstrap] Update gradle/wrapper/gradle-wrapper.properties to pin to $PinnedGradleWrapperVersion or update the Master Constitution." -ForegroundColor Red
  exit 5
}

# prefer wrapper script if present in project root
$gradlewPath = Join-Path -Path $ProjectRoot -ChildPath "gradlew"
$gradlewBatPath = Join-Path -Path $ProjectRoot -ChildPath "gradlew.bat"
if (Test-Path $gradlewPath) { $gradleCmd = $gradlewPath }
elseif (Test-Path $gradlewBatPath) { $gradleCmd = $gradlewBatPath }
else { $gradleCmd = "gradle" }

Write-Host "[bootstrap] Gradle wrapper verified; using Gradle command: $gradleCmd"

Write-Host "[bootstrap] Using Gradle command: $gradleCmd"

# Environment preparation placeholders
Write-Host "[bootstrap] (placeholder) environment preparation steps can be added here: local repo, env vars, caching"

# Run the fast-path
$res = Run-FastPath -GradleCmd $gradleCmd

Write-Host "[bootstrap] Fast-path result summary:" 
if ($res.CoreTest -eq 0) { Write-Host " - core tests: SUCCESS" -ForegroundColor Green } else { Write-Host " - core tests: FAILED (exit $($res.CoreTest))" -ForegroundColor Red }
if ($res.CliRun -eq 0) { Write-Host " - cli run: SUCCESS" -ForegroundColor Green } elseif ($res.CliRun -ne $null) { Write-Host " - cli run: FAILED (exit $($res.CliRun))" -ForegroundColor Red } else { Write-Host " - cli run: SKIPPED" -ForegroundColor Yellow }

Write-Host "[bootstrap] Done. Extend this script to strictly enforce pinned toolchains per the Master Constitution."

