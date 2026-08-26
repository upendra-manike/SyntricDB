# ==============================================================================
# SyntricDB Windows Native PowerShell Installer (Windows 10/11 & Server)
# Usage (Online One-Liner):
# powershell -ExecutionPolicy Bypass -Command "iwr -useb 'https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/windows/install_windows.ps1?v=1.0.3' | iex"
# Usage (Local File):
# powershell -ExecutionPolicy Bypass -File install_windows.ps1
# ==============================================================================

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "SyntricDB Windows Native Database Installer v1.0.3" -ForegroundColor Cyan
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

# Function to find Java runtime on Windows
function Find-JavaExecutable {
    # 1. Active PATH
    try {
        $cmd = Get-Command java -ErrorAction SilentlyContinue
        if ($cmd -and (Test-Path $cmd.Source)) {
            return $cmd.Source
        }
    } catch {}

    # 2. JAVA_HOME environment variable
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        return "$env:JAVA_HOME\bin\java.exe"
    }

    # 3. Machine / User PATH from Registry
    $regMachine = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $regUser = [Environment]::GetEnvironmentVariable("Path", "User")
    $allRegPaths = "$regMachine;$regUser" -split ';'
    foreach ($p in $allRegPaths) {
        if ($p -and (Test-Path "$p\java.exe")) {
            return "$p\java.exe"
        }
    }

    # 4. Standard JDK Install Locations
    $searchLocations = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Amazon Corretto",
        "C:\Program Files\Zulu",
        "C:\Program Files\Semeru",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium",
        "$env:USERPROFILE\.jdks"
    )

    foreach ($loc in $searchLocations) {
        if (Test-Path $loc) {
            $found = Get-ChildItem -Path $loc -Filter "java.exe" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($found) {
                return $found.FullName
            }
        }
    }

    return $null
}

# 1. Detect Privileges & Set Install Directory
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if ($isAdmin) {
    Write-Host "[+] Running as Administrator (System-wide installation)" -ForegroundColor Green
    $InstallDir = "$env:ProgramFiles\SyntricDB"
} else {
    Write-Host "[*] Running as Normal User (User-level installation - No Admin needed)" -ForegroundColor Yellow
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

# Clean old stale launcher files if present
Remove-Item "$InstallDir\syntricdb.bat" -Force -ErrorAction SilentlyContinue
Remove-Item "$InstallDir\syntricdb.cmd" -Force -ErrorAction SilentlyContinue
Remove-Item "$InstallDir\syntricdb.ps1" -Force -ErrorAction SilentlyContinue

# 3. Verify / Install Java 21
$javaPath = Find-JavaExecutable

if (-not $javaPath) {
    Write-Host "[*] Java not detected. Attempting to install OpenJDK 21 via winget..." -ForegroundColor Yellow
    try {
        winget install EclipseAdoptium.Temurin.21.JDK --silent --accept-package-agreements --accept-source-agreements
        $javaPath = Find-JavaExecutable
    } catch {
        Write-Host "[!] winget install skipped or failed." -ForegroundColor Yellow
    }
}

if ($javaPath) {
    $javaBinDir = Split-Path $javaPath -Parent
    if ($env:Path -notlike "*$javaBinDir*") {
        $env:Path = "$javaBinDir;$env:Path"
    }
    if (-not $env:JAVA_HOME) {
        $env:JAVA_HOME = Split-Path $javaBinDir -Parent
    }
    # Ensure Java is persistently in User PATH if not already in machine PATH
    $regMachine = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $regUser = [Environment]::GetEnvironmentVariable("Path", "User")
    if (($regMachine -notlike "*$javaBinDir*") -and ($regUser -notlike "*$javaBinDir*")) {
        [Environment]::SetEnvironmentVariable("Path", "$regUser;$javaBinDir", "User")
    }
    Write-Host "[OK] Detected Java Runtime Environment at: $javaPath" -ForegroundColor Green
} else {
    Write-Host "[!] Warning: Java 21 was not found automatically." -ForegroundColor Yellow
    Write-Host "    Please install OpenJDK 21 from: https://adoptium.net/temurin/releases/?version=21" -ForegroundColor Yellow
}

# 4. Setup Configuration File (Fail-safe for iex / non-interactive execution)
$AdminUser = "admin"
$AdminPass = "syntricdb_secret_pass"

if ($env:SYNTRICDB_NON_INTERACTIVE -ne "true") {
    try {
        Write-Host "Setting up Database Administrator Credentials:" -ForegroundColor Yellow
        $inputUser = Read-Host "   - Admin Username [default: admin]"
        if (-not [string]::IsNullOrWhiteSpace($inputUser)) { $AdminUser = $inputUser }

        $inputPass = Read-Host "   - Admin Password [default: syntricdb_secret_pass]" -AsSecureString
        if ($inputPass) {
            $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($inputPass)
            $PlainPass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
            if (-not [string]::IsNullOrWhiteSpace($PlainPass)) { $AdminPass = $PlainPass }
        }
    } catch {
        # Fallback to default credentials if running non-interactively via iex pipeline
    }
}

$dataDirPosix = $ConfigDir.Replace('\', '/') + '/data'
$walDirPosix = $ConfigDir.Replace('\', '/') + '/wal'
$snapDirPosix = $ConfigDir.Replace('\', '/') + '/snapshots'

$configContent = @"
bind_address=0.0.0.0
port=8080
auth_enabled=true
admin_user=$AdminUser
admin_password=$AdminPass
data_dir=$dataDirPosix
wal_dir=$walDirPosix
snapshot_dir=$snapDirPosix
firewall_enabled=true
rate_limit_per_sec=1000
dlp_masking_enabled=true
"@

Set-Content -Path $ConfFile -Value $configContent -Encoding UTF8
Set-Content -Path "$UserHomeConfigDir\syntricdb.conf" -Value $configContent -Encoding UTF8
Write-Host "[OK] Configuration saved to $ConfFile" -ForegroundColor Green

# 5. Locate or Download Engine JAR File
$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$jarCandidates = @(
    "syntricdb-engine.jar",
    "deploy\windows\syntricdb-engine.jar",
    "$scriptDir\syntricdb-engine.jar",
    "$scriptDir\deploy\windows\syntricdb-engine.jar",
    "$scriptDir\..\..\deploy\windows\syntricdb-engine.jar",
    "target\syntricdb-engine-1.0.0-SNAPSHOT.jar",
    "..\target\syntricdb-engine-1.0.0-SNAPSHOT.jar",
    "$scriptDir\target\syntricdb-engine-1.0.0-SNAPSHOT.jar"
)

$targetJar = "$InstallDir\syntricdb-engine.jar"
$jarFound = $false

foreach ($candidate in $jarCandidates) {
    if (Test-ValidJar $candidate) {
        Copy-Item $candidate $targetJar -Force
        Write-Host "[OK] Installed engine JAR from local path ($candidate)" -ForegroundColor Green
        $jarFound = $true
        break
    }
}

if (-not $jarFound) {
    $foundFile = Get-ChildItem -Path . -Filter "syntricdb-engine*.jar" -Recurse -ErrorAction SilentlyContinue | Where-Object { Test-ValidJar $_.FullName } | Select-Object -First 1
    if ($foundFile) {
        Copy-Item $foundFile.FullName $targetJar -Force
        Write-Host "[OK] Installed engine JAR from search ($($foundFile.FullName))" -ForegroundColor Green
        $jarFound = $true
    }
}

# Download directly from raw GitHub with cache buster if not found locally
if (-not $jarFound) {
    Write-Host "[*] Engine JAR not found locally. Downloading production JAR from GitHub repository..." -ForegroundColor Yellow
    $cacheBuster = Get-Date -Format "yyyyMMddHHmmss"
    $downloadUrls = @(
        "https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/windows/syntricdb-engine.jar?v=$cacheBuster",
        "https://github.com/upendra-manike/SyntricDB/raw/main/deploy/windows/syntricdb-engine.jar?v=$cacheBuster",
        "https://github.com/upendra-manike/SyntricDB/releases/download/v1.0.0/syntricdb-engine.jar"
    )

    $noCacheHeaders = @{
        "Cache-Control" = "no-cache, no-store, must-revalidate"
        "Pragma"        = "no-cache"
        "Expires"       = "0"
    }

    foreach ($url in $downloadUrls) {
        try {
            Write-Host "   Downloading: $url" -ForegroundColor Gray
            Invoke-WebRequest -Uri $url -OutFile $targetJar -Headers $noCacheHeaders -UseBasicParsing -ErrorAction Stop
            if (Test-ValidJar $targetJar) {
                Write-Host "[OK] Engine JAR downloaded and validated successfully!" -ForegroundColor Green
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

if (-not (Test-ValidJar $targetJar)) {
    Write-Host "[x] Error: Could not locate or download a valid syntricdb-engine.jar file." -ForegroundColor Red
    exit 1
}

# 6. Create syntricdb.ps1 Native PowerShell Script & syntricdb.bat CMD Wrapper
$ps1Content = @'
param(
    [string]$Command = "usage",
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$RestArgs
)

function Find-JavaRuntime {
    try {
        $cmd = Get-Command java -ErrorAction SilentlyContinue
        if ($cmd -and (Test-Path $cmd.Source)) { return $cmd.Source }
    } catch {}

    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        return "$env:JAVA_HOME\bin\java.exe"
    }

    $regMachine = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $regUser = [Environment]::GetEnvironmentVariable("Path", "User")
    $allRegPaths = "$regMachine;$regUser" -split ';'
    foreach ($p in $allRegPaths) {
        if ($p -and (Test-Path "$p\java.exe")) {
            return "$p\java.exe"
        }
    }

    $searchLocations = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Amazon Corretto",
        "C:\Program Files\Zulu",
        "C:\Program Files\Semeru",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium",
        "$env:USERPROFILE\.jdks"
    )

    foreach ($loc in $searchLocations) {
        if (Test-Path $loc) {
            $found = Get-ChildItem -Path $loc -Filter "java.exe" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($found) { return $found.FullName }
        }
    }
    return "java"
}

$InstallDir = $PSScriptRoot
if (-not $InstallDir) { $InstallDir = (Get-Location).Path }
$JarPath = Join-Path $InstallDir "syntricdb-engine.jar"

$ConfigDir = if ($env:APPDATA) { Join-Path $env:APPDATA "SyntricDB" } else { Join-Path $env:USERPROFILE ".syntricdb" }
if (-not (Test-Path $ConfigDir)) { New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null }
if (-not (Test-Path "$ConfigDir\data")) { New-Item -ItemType Directory -Force -Path "$ConfigDir\data" | Out-Null }
$LogFile = Join-Path $ConfigDir "syntricdb.log"
$ErrLogFile = Join-Path $ConfigDir "syntricdb.err.log"
$PidFilePath = Join-Path $ConfigDir "syntricdb.pid"

function Get-SyntricProcesses {
    $found = [System.Collections.Generic.List[PSObject]]::new()
    try {
        $cimProcs = Get-CimInstance Win32_Process -Filter "CommandLine LIKE '%syntricdb-engine.jar%'" -ErrorAction SilentlyContinue
        if ($cimProcs) {
            foreach ($cp in $cimProcs) {
                $found.Add([PSCustomObject]@{ ProcessId = $cp.ProcessId; ProcessName = $cp.Name })
            }
        }
    } catch {}
    return $found.ToArray()
}

$javaExe = Find-JavaRuntime

switch ($Command.ToLower()) {
    "start" {
        if (-not (Test-Path $JarPath)) {
            Write-Host "[x] Error: SyntricDB Engine JAR not found at $JarPath" -ForegroundColor Red
            exit 1
        }

        $existing = Get-SyntricProcesses
        if ($existing.Length -gt 0) {
            $currentServerPid = $existing[0].ProcessId
            Write-Host "[!] SyntricDB Server is already running (PID: $currentServerPid)" -ForegroundColor Green
            Write-Host "    Web Dashboard: http://localhost:8080/" -ForegroundColor Cyan
            exit 0
        }

        Write-Host "Starting SyntricDB AI-Native Engine on Port 8080..." -ForegroundColor Yellow
        $cmdLine = "`"$javaExe`" -Xms512m -Xmx4g -jar `"$JarPath`""
        $res = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = $cmdLine; CurrentDirectory = $InstallDir }
        if ($res.ReturnValue -eq 0 -and $res.ProcessId) {
            Set-Content -Path $PidFilePath -Value $res.ProcessId -Encoding ASCII
        }

        $isLive = $false
        for ($i = 0; $i -lt 10; $i++) {
            Start-Sleep -Seconds 1
            $liveProcs = Get-SyntricProcesses
            if ($liveProcs.Length -gt 0) {
                $isLive = $true
                try {
                    $client = New-Object System.Net.Sockets.TcpClient
                    $iar = $client.BeginConnect("127.0.0.1", 8080, $null, $null)
                    if ($iar.AsyncWaitHandle.WaitOne(400) -and $client.Connected) {
                        $client.EndConnect($iar)
                        $client.Close()
                        break
                    }
                    $client.Close()
                } catch {}
            } else {
                $isLive = $false
                break
            }
        }

        if ($isLive) {
            $liveProcs = Get-SyntricProcesses
            $actualPid = if ($liveProcs.Length -gt 0) { $liveProcs[0].ProcessId } else { $res.ProcessId }
            Write-Host "[OK] SyntricDB Server launched in background (PID: $actualPid)." -ForegroundColor Green
            Write-Host "     Log File     : $LogFile" -ForegroundColor Gray
            Write-Host "     Web Dashboard: http://localhost:8080/" -ForegroundColor Cyan
            Write-Host "     REST API     : http://localhost:8080/api/sql" -ForegroundColor Cyan
        } else {
            Write-Host "[!] Warning: SyntricDB server process did not start properly." -ForegroundColor Yellow
            if (Test-Path $LogFile) { Get-Content -Path $LogFile -Tail 15 }
            if (Test-Path $ErrLogFile) { Get-Content -Path $ErrLogFile -Tail 15 }
        }
    }
    "server" {
        & $javaExe -Xms512m -Xmx4g -jar "$JarPath"
    }
    "stop" {
        Write-Host "Stopping SyntricDB Server..." -ForegroundColor Yellow
        $procs = Get-SyntricProcesses
        if ($procs.Length -gt 0) {
            foreach ($p in $procs) {
                Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
            }
            if (Test-Path $PidFilePath) { Remove-Item $PidFilePath -Force -ErrorAction SilentlyContinue }
            Write-Host "[OK] SyntricDB Server stopped." -ForegroundColor Green
        } else {
            if (Test-Path $PidFilePath) { Remove-Item $PidFilePath -Force -ErrorAction SilentlyContinue }
            Write-Host "[*] SyntricDB Server is not currently running." -ForegroundColor Yellow
        }
    }
    "status" {
        $procs = Get-SyntricProcesses
        if ($procs.Length -gt 0) {
            $runningServerPid = $procs[0].ProcessId
            Write-Host "[OK] SyntricDB Server is running (PID: $runningServerPid)" -ForegroundColor Green
            Write-Host "     Web Console: http://localhost:8080/" -ForegroundColor Cyan
        } else {
            Write-Host "[*] SyntricDB Server is stopped." -ForegroundColor Red
        }
    }
    "cli" {
        & $javaExe -cp "$JarPath" com.syntricdb.cli.SyntricCLI $RestArgs
    }
    "logs" {
        if (Test-Path $LogFile) {
            Get-Content -Path $LogFile -Tail 50
        } elseif (Test-Path $ErrLogFile) {
            Get-Content -Path $ErrLogFile -Tail 50
        } else {
            Write-Host "No log file found at $LogFile" -ForegroundColor Yellow
        }
    }
    default {
        Write-Host "==========================================================" -ForegroundColor Cyan
        Write-Host "SyntricDB: Next-Generation AI-Native Unified Database" -ForegroundColor Cyan
        Write-Host "==========================================================" -ForegroundColor Cyan
        Write-Host "Usage: syntricdb {start|stop|status|cli|logs|server}"
        Write-Host "  syntricdb start   : Launch background server daemon"
        Write-Host "  syntricdb stop    : Shutdown background server daemon"
        Write-Host "  syntricdb status  : Check server status"
        Write-Host "  syntricdb cli     : Launch interactive SQL & Vector shell"
        Write-Host "  syntricdb logs    : Show server stdout/stderr logs"
        Write-Host "  syntricdb server  : Run server in foreground console"
        Write-Host "==========================================================" -ForegroundColor Cyan
    }
}
'@

$batWrapperContent = @'
@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0syntricdb.ps1" %*
'@

Set-Content -Path "$InstallDir\syntricdb.ps1" -Value $ps1Content -Encoding UTF8
Set-Content -Path "$InstallDir\syntricdb.bat" -Value $batWrapperContent -Encoding ASCII
Set-Content -Path "$InstallDir\syntricdb.cmd" -Value $batWrapperContent -Encoding ASCII

# 7. Update PATH Environment Variable (Registry + Active Process Session)
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($UserPath -notlike "*$InstallDir*") {
    [Environment]::SetEnvironmentVariable("Path", "$UserPath;$InstallDir", "User")
    Write-Host "[OK] Added $InstallDir to User PATH environment variable." -ForegroundColor Green
}

if ($isAdmin) {
    $MachinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($MachinePath -notlike "*$InstallDir*") {
        [Environment]::SetEnvironmentVariable("Path", "$MachinePath;$InstallDir", "Machine")
        Write-Host "[OK] Added $InstallDir to System PATH environment variable." -ForegroundColor Green
    }
}

# Immediately make syntricdb available in current active terminal
if ($env:Path -notlike "*$InstallDir*") {
    $env:Path = "$InstallDir;$env:Path"
}

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "[SUCCESS] SyntricDB Windows Installation Complete!" -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "  Start Server   : syntricdb start" -ForegroundColor Yellow
Write-Host "  Stop Server    : syntricdb stop" -ForegroundColor Yellow
Write-Host "  Check Status   : syntricdb status" -ForegroundColor Yellow
Write-Host "  Launch CLI     : syntricdb cli" -ForegroundColor Yellow
Write-Host "  Web Dashboard  : http://localhost:8080/" -ForegroundColor Yellow
Write-Host "==========================================================================" -ForegroundColor Cyan
