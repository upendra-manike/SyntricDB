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

# 6. Create syntricdb.ps1 Native PowerShell Script & syntricdb.bat CMD Wrapper
$ps1Content = @'
param(
    [string]$Command = "usage",
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$RestArgs
)

$InstallDir = $PSScriptRoot
if (-not $InstallDir) { $InstallDir = (Get-Location).Path }
$JarPath = Join-Path $InstallDir "syntricdb-engine.jar"

$ConfigDir = if ($env:APPDATA) { Join-Path $env:APPDATA "SyntricDB" } else { Join-Path $env:USERPROFILE ".syntricdb" }
if (-not (Test-Path $ConfigDir)) { New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null }
if (-not (Test-Path "$ConfigDir\data")) { New-Item -ItemType Directory -Force -Path "$ConfigDir\data" | Out-Null }
$LogFile = Join-Path $ConfigDir "syntricdb.log"

switch ($Command.ToLower()) {
    "start" {
        $java = Get-Command java -ErrorAction SilentlyContinue
        if (-not $java) {
            Write-Host "❌ Error: Java runtime is not installed or not in PATH!" -ForegroundColor Red
            Write-Host "Please install Java 21: winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Yellow
            exit 1
        }
        if (-not (Test-Path $JarPath)) {
            Write-Host "❌ Error: SyntricDB Engine JAR not found at $JarPath" -ForegroundColor Red
            exit 1
        }

        $existing = Get-WmiObject Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*syntricdb-engine.jar*' }
        if ($existing) {
            Write-Host "⚡ SyntricDB Server is already running (PID: $($existing.ProcessId))" -ForegroundColor Green
            Write-Host "🌐 Web Dashboard: http://localhost:8080/" -ForegroundColor Cyan
            exit 0
        }

        Write-Host "🚀 Starting SyntricDB AI-Native Engine on Port 8080..." -ForegroundColor Yellow
        Start-Process -FilePath "java.exe" -ArgumentList "-Xms512m", "-Xmx4g", "-jar", "`"$JarPath`"" -RedirectStandardOutput $LogFile -RedirectStandardError $LogFile -WindowStyle Hidden
        Start-Sleep -Seconds 2

        $proc = Get-WmiObject Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*syntricdb-engine.jar*' }
        if ($proc) {
            Write-Host "✅ SyntricDB Server launched in background (PID: $($proc.ProcessId))." -ForegroundColor Green
            Write-Host "📜 Log File     : $LogFile" -ForegroundColor Gray
            Write-Host "🌐 Web Dashboard: http://localhost:8080/" -ForegroundColor Cyan
            Write-Host "📡 REST API     : http://localhost:8080/api/sql" -ForegroundColor Cyan
        } else {
            Write-Host "⚠️ SyntricDB Server launch initiated. Check logs at: $LogFile" -ForegroundColor Yellow
        }
    }
    "server" {
        $java = Get-Command java -ErrorAction SilentlyContinue
        if (-not $java) {
            Write-Host "❌ Error: Java runtime is not installed or not in PATH!" -ForegroundColor Red
            exit 1
        }
        & java -Xms512m -Xmx4g -jar "$JarPath"
    }
    "stop" {
        Write-Host "🛑 Stopping SyntricDB Server..." -ForegroundColor Yellow
        $procs = Get-WmiObject Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*syntricdb-engine.jar*' }
        if ($procs) {
            foreach ($p in $procs) {
                Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
            }
            Write-Host "✅ SyntricDB Server stopped." -ForegroundColor Green
        } else {
            Write-Host "⚠️ SyntricDB Server is not currently running." -ForegroundColor Yellow
        }
    }
    "status" {
        $proc = Get-WmiObject Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*syntricdb-engine.jar*' }
        if ($proc) {
            Write-Host "🟢 SyntricDB Server is running (PID: $($proc.ProcessId))" -ForegroundColor Green
            Write-Host "🌐 Web Console: http://localhost:8080/" -ForegroundColor Cyan
        } else {
            Write-Host "🔴 SyntricDB Server is stopped." -ForegroundColor Red
        }
    }
    "cli" {
        $java = Get-Command java -ErrorAction SilentlyContinue
        if (-not $java) {
            Write-Host "❌ Error: Java runtime is not installed or not in PATH!" -ForegroundColor Red
            exit 1
        }
        & java -cp "$JarPath" com.syntricdb.cli.SyntricCLI $RestArgs
    }
    "logs" {
        if (Test-Path $LogFile) {
            Get-Content -Path $LogFile -Tail 50
        } else {
            Write-Host "No log file found at $LogFile" -ForegroundColor Yellow
        }
    }
    default {
        Write-Host "==========================================================" -ForegroundColor Cyan
        Write-Host "⚡ SyntricDB: Next-Generation AI-Native Unified Database ⚡" -ForegroundColor Cyan
        Write-Host "==========================================================" -ForegroundColor Cyan
        Write-Host "Usage: syntricdb {start|stop|status|cli|logs}"
        Write-Host "  syntricdb start   : Launch background server daemon"
        Write-Host "  syntricdb stop    : Shutdown background server daemon"
        Write-Host "  syntricdb status  : Check server status"
        Write-Host "  syntricdb cli     : Launch interactive SQL & Vector shell"
        Write-Host "  syntricdb logs    : Tail server stdout/stderr logs"
        Write-Host "==========================================================" -ForegroundColor Cyan
    }
}
'@

$batWrapperContent = @'
@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0syntricdb.ps1" %*
'@

Set-Content -Path "$InstallDir\syntricdb.ps1" -Value $ps1Content -Encoding UTF8
Set-Content -Path "$InstallDir\syntricdb.bat" -Value $batWrapperContent -Encoding UTF8
Set-Content -Path "$InstallDir\syntricdb.cmd" -Value $batWrapperContent -Encoding UTF8

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
