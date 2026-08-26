# ==============================================================================
# SyntricDB Windows Native PowerShell Installer (Windows 10/11 & Server)
# Usage (Online One-Liner):
# powershell -ExecutionPolicy Bypass -Command "iwr -useb https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/windows/install_windows.ps1 | iex"
# Usage (Local File):
# powershell -ExecutionPolicy Bypass -File install_windows.ps1
# ==============================================================================

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "⚡ SyntricDB Windows Native Database Installer ⚡" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

# Function to check if a JAR file is valid and non-corrupt (> 1MB & PK ZIP header)
function Test-ValidJar ($filePath) {
    if (-not (Test-Path $filePath)) { return $false }
    $item = Get-Item $filePath
    if ($item.Length -lt 1000000) { return $false }
    try {
        $stream = [System.IO.File]::OpenRead($filePath)
        $b1 = $stream.ReadByte()
        $b2 = $stream.ReadByte()
        $stream.Close()
        if ($b1 -eq 0x50 -and $b2 -eq 0x4B) { return $true }
    } catch {}
    return $false
}

# 1. Detect Privileges & Set Install Directory
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if ($isAdmin) {
    Write-Host "🛡️  Running as Administrator (System-wide installation)" -ForegroundColor Green
    $InstallDir = "$env:ProgramFiles\SyntricDB"
} else {
    Write-Host "👤 Running as Normal User (User-level installation - No Admin needed)" -ForegroundColor Yellow
    $InstallDir = "$env:LOCALAPPDATA\Programs\SyntricDB"
}

$ConfigDir = "$env:APPDATA\SyntricDB"
$UserHomeConfigDir = "$env:USERPROFILE\.syntricdb"
$ConfFile = "$ConfigDir\syntricdb.conf"

# 2. Create Target Directories
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null
New-Item -ItemType Directory -Force -Path "$ConfigDir\data" | Out-Null
New-Item -ItemType Directory -Force -Path "$ConfigDir\wal" | Out-Null
New-Item -ItemType Directory -Force -Path "$ConfigDir\snapshots" | Out-Null
New-Item -ItemType Directory -Force -Path $UserHomeConfigDir | Out-Null
New-Item -ItemType Directory -Force -Path "$UserHomeConfigDir\data" | Out-Null

# 3. Verify / Install Java 21
try {
    $javaVer = java -version 2>&1
    Write-Host "✅ Detected Java Runtime Environment." -ForegroundColor Green
} catch {
    Write-Host "📦 Installing OpenJDK 21 via winget..." -ForegroundColor Yellow
    try {
        winget install EclipseAdoptium.Temurin.21.JDK --silent --accept-package-agreements --accept-source-agreements
    } catch {
        Write-Host "⚠️ winget install skipped or failed. Ensure Java 21 is installed manually if needed." -ForegroundColor Yellow
    }
}

# 4. Setup Configuration File (Fail-safe for iex / non-interactive execution)
$AdminUser = "admin"
$AdminPass = "syntricdb_secret_pass"

if ($env:SYNTRICDB_NON_INTERACTIVE -ne "true") {
    try {
        Write-Host "🔐 Setting up Database Administrator Credentials:" -ForegroundColor Yellow
        $inputUser = Read-Host "   • Admin Username [default: admin]"
        if (-not [string]::IsNullOrWhiteSpace($inputUser)) { $AdminUser = $inputUser }

        $inputPass = Read-Host "   • Admin Password [default: syntricdb_secret_pass]" -AsSecureString
        if ($inputPass) {
            $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($inputPass)
            $PlainPass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
            if (-not [string]::IsNullOrWhiteSpace($PlainPass)) { $AdminPass = $PlainPass }
        }
    } catch {
        # Fallback to default credentials if running non-interactively via iex pipeline
    }
}

$configContent = @"
bind_address=0.0.0.0
port=8080
auth_enabled=true
admin_user=$AdminUser
admin_password=$AdminPass
data_dir=$ConfigDir\data
wal_dir=$ConfigDir\wal
snapshot_dir=$ConfigDir\snapshots
firewall_enabled=true
rate_limit_per_sec=1000
dlp_masking_enabled=true
"@

Set-Content -Path $ConfFile -Value $configContent -Encoding UTF8
Set-Content -Path "$UserHomeConfigDir\syntricdb.conf" -Value $configContent -Encoding UTF8
Write-Host "✅ Configuration saved to $ConfFile" -ForegroundColor Green

# 5. Locate or Download Engine JAR File
$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$jarCandidates = @(
    "syntricdb-engine.jar",
    "syntricdb-engine-1.0.0-SNAPSHOT.jar",
    "target\syntricdb-engine-1.0.0-SNAPSHOT.jar",
    "..\target\syntricdb-engine-1.0.0-SNAPSHOT.jar",
    "$scriptDir\syntricdb-engine.jar",
    "$scriptDir\deploy\windows\syntricdb-engine.jar",
    "$scriptDir\target\syntricdb-engine-1.0.0-SNAPSHOT.jar"
)

$targetJar = "$InstallDir\syntricdb-engine.jar"
$jarFound = $false

foreach ($candidate in $jarCandidates) {
    if (Test-ValidJar $candidate) {
        Copy-Item $candidate $targetJar -Force
        Write-Host "✅ Installed engine JAR from local path ($candidate)" -ForegroundColor Green
        $jarFound = $true
        break
    }
}

if (-not $jarFound) {
    $foundFile = Get-ChildItem -Path . -Filter "syntricdb-engine*.jar" -Recurse -ErrorAction SilentlyContinue | Where-Object { Test-ValidJar $_.FullName } | Select-Object -First 1
    if ($foundFile) {
        Copy-Item $foundFile.FullName $targetJar -Force
        Write-Host "✅ Installed engine JAR from search ($($foundFile.FullName))" -ForegroundColor Green
        $jarFound = $true
    }
}

# Download directly from raw GitHub if not found locally (for iwr | iex online execution)
if (-not $jarFound) {
    Write-Host "🌐 Engine JAR not found locally. Downloading production JAR from GitHub repository..." -ForegroundColor Yellow
    $downloadUrls = @(
        "https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/windows/syntricdb-engine.jar",
        "https://github.com/upendra-manike/SyntricDB/raw/main/deploy/windows/syntricdb-engine.jar",
        "https://github.com/upendra-manike/SyntricDB/releases/download/v1.0.0/syntricdb-engine.jar"
    )

    foreach ($url in $downloadUrls) {
        try {
            Write-Host "   Downloading: $url" -ForegroundColor Gray
            Invoke-WebRequest -Uri $url -OutFile $targetJar -UseBasicParsing -ErrorAction Stop
            if (Test-ValidJar $targetJar) {
                Write-Host "✅ Engine JAR downloaded and validated successfully!" -ForegroundColor Green
                $jarFound = $true
                break
            } else {
                Remove-Item $targetJar -ErrorAction SilentlyContinue
            }
        } catch {
            Remove-Item $targetJar -ErrorAction SilentlyContinue
        }
    }
}

if (-not $jarFound) {
    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        Write-Host "🔨 Compiling SyntricDB JAR via Maven..." -ForegroundColor Yellow
        mvn clean package -DskipTests
        if (Test-ValidJar "target\syntricdb-engine-1.0.0-SNAPSHOT.jar") {
            Copy-Item "target\syntricdb-engine-1.0.0-SNAPSHOT.jar" $targetJar -Force
            $jarFound = $true
        }
    }
}

if (-not (Test-ValidJar $targetJar)) {
    Write-Host "❌ Error: Could not locate or download a valid syntricdb-engine.jar file." -ForegroundColor Red
    exit 1
}

# 6. Create syntricdb.bat CMD & PowerShell Launcher with Fixed Quoting Syntax
$batContent = @'
@echo off
setlocal enableextensions
SET "INSTALL_DIR=%~dp0"
IF "%INSTALL_DIR:~-1%"=="\" SET "INSTALL_DIR=%INSTALL_DIR:~0,-1%"
SET "JAR_PATH=%INSTALL_DIR%\syntricdb-engine.jar"

IF "%APPDATA%"=="" (
    SET "CONF_DIR=%USERPROFILE%\.syntricdb"
) ELSE (
    SET "CONF_DIR=%APPDATA%\SyntricDB"
)

IF NOT EXIST "%CONF_DIR%" mkdir "%CONF_DIR%"
IF NOT EXIST "%CONF_DIR%\data" mkdir "%CONF_DIR%\data"
IF NOT EXIST "%CONF_DIR%\wal" mkdir "%CONF_DIR%\wal"
IF NOT EXIST "%CONF_DIR%\snapshots" mkdir "%CONF_DIR%\snapshots"

SET "LOG_FILE=%CONF_DIR%\syntricdb.log"

IF "%1"=="start" GOTO start
IF "%1"=="server" GOTO start
IF "%1"=="stop" GOTO stop
IF "%1"=="status" GOTO status
IF "%1"=="cli" GOTO cli
IF "%1"=="logs" GOTO logs
GOTO usage

:start
WHERE java >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
    echo ❌ Error: Java runtime is not installed or not in PATH!
    echo Please install Java 21: winget install EclipseAdoptium.Temurin.21.JDK
    EXIT /B 1
)
IF NOT EXIST "%JAR_PATH%" (
    echo ❌ Error: SyntricDB Engine JAR not found at %JAR_PATH%
    EXIT /B 1
)
echo Starting SyntricDB AI-Native Engine on Port 8080...
start "SyntricDB Server" /B java -Xms512m -Xmx4g -jar "%JAR_PATH%" > "%LOG_FILE%" 2>&1
echo SyntricDB Server launched in background.
echo Log File     : %LOG_FILE%
echo Web Dashboard: http://localhost:8080/
echo REST API     : http://localhost:8080/api/sql
EXIT /B 0

:stop
echo Stopping SyntricDB Server...
powershell -Command "Get-WmiObject Win32_Process | Where-Object { $_.CommandLine -like '*syntricdb-engine.jar*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }" >nul 2>nul
echo SyntricDB Server stopped.
EXIT /B 0

:status
powershell -Command "$p = Get-WmiObject Win32_Process | Where-Object { $_.CommandLine -like '*syntricdb-engine.jar*' }; if ($p) { Write-Host 'SyntricDB Server is running (PID: ' $p.ProcessId ')' -ForegroundColor Green; Write-Host 'Web Console: http://localhost:8080/' -ForegroundColor Cyan } else { Write-Host 'SyntricDB Server is stopped.' -ForegroundColor Red }"
EXIT /B 0

:cli
WHERE java >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
    echo ❌ Error: Java is not installed or not in PATH!
    EXIT /B 1
)
shift
java -cp "%JAR_PATH%" com.syntricdb.cli.SyntricCLI %1 %2 %3 %4 %5 %6 %7 %8 %9
EXIT /B 0

:logs
powershell -Command "if (Test-Path '%LOG_FILE%') { Get-Content '%LOG_FILE%' -Tail 50 } else { Write-Host 'No log file found at %LOG_FILE%' -ForegroundColor Yellow }"
EXIT /B 0

:usage
echo ==========================================================
echo SyntricDB: Next-Generation AI-Native Unified Database
echo ==========================================================
echo Usage: syntricdb {start|stop|status|cli|logs}
echo   syntricdb start   : Launch background server daemon
echo   syntricdb stop    : Shutdown background server daemon
echo   syntricdb status  : Check server status
echo   syntricdb cli     : Launch interactive SQL & Vector shell
echo   syntricdb logs    : Tail server stdout/stderr logs
echo ==========================================================
EXIT /B 0
'@

Set-Content -Path "$InstallDir\syntricdb.bat" -Value $batContent -Encoding UTF8
Set-Content -Path "$InstallDir\syntricdb.cmd" -Value $batContent -Encoding UTF8

# 7. Update PATH Environment Variable (Registry + Active Process Session)
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($UserPath -notlike "*$InstallDir*") {
    [Environment]::SetEnvironmentVariable("Path", "$UserPath;$InstallDir", "User")
    Write-Host "✅ Added $InstallDir to User PATH environment variable." -ForegroundColor Green
}

if ($isAdmin) {
    $MachinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($MachinePath -notlike "*$InstallDir*") {
        [Environment]::SetEnvironmentVariable("Path", "$MachinePath;$InstallDir", "Machine")
        Write-Host "✅ Added $InstallDir to System PATH environment variable." -ForegroundColor Green
    }
}

# Immediately make syntricdb available in current active terminal
if ($env:Path -notlike "*$InstallDir*") {
    $env:Path = "$InstallDir;$env:Path"
}

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "🎉 SyntricDB Windows Installation Complete!" -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "🚀 Start Server   : syntricdb start" -ForegroundColor Yellow
Write-Host "🛑 Stop Server    : syntricdb stop" -ForegroundColor Yellow
Write-Host "📊 Check Status   : syntricdb status" -ForegroundColor Yellow
Write-Host "💻 Launch CLI      : syntricdb cli" -ForegroundColor Yellow
Write-Host "🌐 Web Dashboard  : http://localhost:8080/" -ForegroundColor Yellow
Write-Host "==========================================================================" -ForegroundColor Cyan
