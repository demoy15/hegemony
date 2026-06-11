param(
  [switch]$InstallMissing,
  [switch]$NoBrowser,
  [switch]$SkipFrontendInstall
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $RepoRoot "backend"
$FrontendDir = Join-Path $RepoRoot "frontend"
$SaveDir = Join-Path $RepoRoot "data\saves"

function Write-Step {
  param([string]$Message)
  Write-Host ""
  Write-Host "==> $Message" -ForegroundColor Cyan
}

function Test-Command {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Update-ProcessEnvironment {
  $machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
  $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
  $env:Path = (@($machinePath, $userPath) | Where-Object { $_ }) -join ";"

  $userJavaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
  $machineJavaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
  if ($userJavaHome) {
    $env:JAVA_HOME = $userJavaHome
  } elseif ($machineJavaHome) {
    $env:JAVA_HOME = $machineJavaHome
  }
}

function Get-JavaMajorVersion {
  if (-not (Test-Command "java")) {
    return $null
  }

  $versionLine = (& cmd.exe /c "java -version 2>&1" | Select-Object -First 1)
  if ($versionLine -match 'version "(\d+)') {
    return [int]$Matches[1]
  }

  return $null
}

function Get-NodeMajorVersion {
  if (-not (Test-Command "node")) {
    return $null
  }

  $versionLine = (& node --version)
  if ($versionLine -match '^v(\d+)') {
    return [int]$Matches[1]
  }

  return $null
}

function Install-WingetPackage {
  param(
    [string]$PackageId,
    [string]$DisplayName
  )

  if (-not (Test-Command "winget")) {
    throw "$DisplayName is missing. Install it manually, or install App Installer from Microsoft Store so winget is available."
  }

  Write-Step "Installing $DisplayName with winget"
  winget install --id $PackageId --exact --source winget --accept-package-agreements --accept-source-agreements
  if ($LASTEXITCODE -ne 0) {
    throw "winget could not install $DisplayName. Install it manually and run start-local.bat again."
  }
}

function Require-Java {
  $major = Get-JavaMajorVersion
  if ($major -ge 21) {
    Write-Host "Java $major found."
    return
  }

  if ($InstallMissing) {
    Install-WingetPackage -PackageId "EclipseAdoptium.Temurin.21.JDK" -DisplayName "Java 21 JDK"
    $major = Get-JavaMajorVersion
    if ($major -ge 21) {
      Write-Host "Java $major found."
      return
    }
    throw "Java 21 was installed, but this terminal cannot see it yet. Open a new terminal and run start-local.bat again."
  }

  throw "Java 21 JDK is required. Install Temurin 21 JDK, or run: start-local.bat -InstallMissing"
}

function Require-Node {
  $major = Get-NodeMajorVersion
  if (($major -ge 20) -and (Test-Command "npm.cmd")) {
    Write-Host "Node.js $major and npm found."
    return
  }

  if ($InstallMissing) {
    Install-WingetPackage -PackageId "OpenJS.NodeJS.LTS" -DisplayName "Node.js LTS"
    $major = Get-NodeMajorVersion
    if (($major -ge 20) -and (Test-Command "npm.cmd")) {
      Write-Host "Node.js $major and npm found."
      return
    }
    throw "Node.js was installed, but this terminal cannot see it yet. Open a new terminal and run start-local.bat again."
  }

  throw "Node.js 20+ with npm is required. Install Node.js LTS, or run: start-local.bat -InstallMissing"
}

function Assert-PortAvailable {
  param(
    [int]$Port,
    [string]$Name
  )

  $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
  if ($listeners) {
    throw "$Name port $Port is already in use. Stop the existing process and run start-local.bat again."
  }
}

function Invoke-FrontendInstallIfNeeded {
  if ($SkipFrontendInstall) {
    Write-Host "Skipping frontend dependency install."
    return
  }

  $nodeModules = Join-Path $FrontendDir "node_modules"
  $lockFile = Join-Path $FrontendDir "package-lock.json"
  $installedLock = Join-Path $nodeModules ".package-lock.json"
  $needsInstall = -not (Test-Path $nodeModules)

  if ((Test-Path $lockFile) -and (Test-Path $installedLock)) {
    $needsInstall = $needsInstall -or ((Get-Item $lockFile).LastWriteTimeUtc -gt (Get-Item $installedLock).LastWriteTimeUtc)
  } elseif (Test-Path $lockFile) {
    $needsInstall = $true
  }

  if (-not $needsInstall) {
    Write-Host "Frontend dependencies already installed."
    return
  }

  Write-Step "Installing frontend dependencies"
  Push-Location $FrontendDir
  try {
    npm.cmd ci
    if ($LASTEXITCODE -ne 0) {
      throw "npm ci failed."
    }
  } finally {
    Pop-Location
  }
}

function Start-DevWindow {
  param(
    [string]$Title,
    [string]$WorkingDirectory,
    [string]$Command
  )

  $safeTitle = $Title.Replace("'", "''")
  $safeWorkingDirectory = $WorkingDirectory.Replace("'", "''")
  $script = @"
`$Host.UI.RawUI.WindowTitle = '$safeTitle'
Set-Location -LiteralPath '$safeWorkingDirectory'
$Command
"@

  $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($script))
  Start-Process powershell.exe -WorkingDirectory $WorkingDirectory -ArgumentList @(
    "-NoExit",
    "-NoProfile",
    "-ExecutionPolicy",
    "Bypass",
    "-EncodedCommand",
    $encoded
  )
}

try {
  Update-ProcessEnvironment
  Set-Location $RepoRoot

  Write-Step "Checking Windows local prerequisites"
  Require-Java
  Require-Node
  Assert-PortAvailable -Port 8080 -Name "Backend"
  Assert-PortAvailable -Port 5173 -Name "Frontend"

  New-Item -ItemType Directory -Force -Path $SaveDir | Out-Null
  Invoke-FrontendInstallIfNeeded

  Write-Step "Starting backend and frontend"
$safeSaveDir = $SaveDir.Replace("'", "''")
$backendCommand = "`$env:HEGEMONY_SAVE_DIR = '$safeSaveDir'`r`n.\gradlew.bat bootRun"
$frontendCommand = "npm.cmd run dev -- --host 127.0.0.1"

  Start-DevWindow -Title "Hegemony Backend" -WorkingDirectory $BackendDir -Command $backendCommand
  Start-DevWindow -Title "Hegemony Frontend" -WorkingDirectory $FrontendDir -Command $frontendCommand

  Write-Host ""
  Write-Host "Local app is starting."
  Write-Host "Frontend: http://localhost:5173"
  Write-Host "Backend:  http://localhost:8080/api/game"
  Write-Host "Saves:    $SaveDir"
  Write-Host ""
  Write-Host "Stop it by pressing Ctrl+C in the backend/frontend windows, or close those windows."

  if (-not $NoBrowser) {
    Start-Sleep -Seconds 3
    Start-Process "http://localhost:5173"
  }
} catch {
  Write-Host ""
  Write-Host "Startup failed:" -ForegroundColor Red
  Write-Host $_.Exception.Message -ForegroundColor Red
  exit 1
}
