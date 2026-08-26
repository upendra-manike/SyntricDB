# ==============================================================================
# SyntricDB Windows Native PowerShell Uninstaller
# Usage (Online One-Liner):
# powershell -ExecutionPolicy Bypass -Command "iwr -useb 'https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/windows/uninstall_windows.ps1?v=1.0.1' | iex"
# ==============================================================================

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "🗑️  SyntricDB Windows Native Database Uninstaller ⚡" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

# 1. Stop running SyntricDB server processes
Write-Host "🛑 Stopping running SyntricDB server processes..." -ForegroundColor Yellow
try {
    $procs = Get-WmiObject Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*syntricdb-engine.jar*' }
    if ($procs) {
        foreach ($p in $procs) {
            Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
        }
        Write-Host "✅ Stopped background server processes." -ForegroundColor Green
    }
} catch {}

# 2. Remove Installation & Configuration Directories
$targets = @(
    "$env:LOCALAPPDATA\Programs\SyntricDB",
    "$env:ProgramFiles\SyntricDB",
    "$env:APPDATA\SyntricDB",
    "$env:USERPROFILE\.syntricdb"
)

foreach ($target in $targets) {
    if (Test-Path $target) {
        try {
            Remove-Item -Path $target -Recurse -Force -ErrorAction SilentlyContinue
            Write-Host "✅ Removed directory: $target" -ForegroundColor Green
        } catch {
            Write-Host "⚠️ Warning: Could not remove $target" -ForegroundColor Yellow
        }
    }
}

# 3. Clean PATH Environment Variable (User & Machine)
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($UserPath -and ($UserPath -like "*SyntricDB*")) {
    $cleanUserPath = ($UserPath -split ';' | Where-Object { $_ -and ($_ -notlike "*SyntricDB*") }) -join ';'
    [Environment]::SetEnvironmentVariable("Path", $cleanUserPath, "User")
    Write-Host "✅ Removed SyntricDB from User PATH environment variable." -ForegroundColor Green
}

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if ($isAdmin) {
    $MachinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($MachinePath -and ($MachinePath -like "*SyntricDB*")) {
        $cleanMachinePath = ($MachinePath -split ';' | Where-Object { $_ -and ($_ -notlike "*SyntricDB*") }) -join ';'
        [Environment]::SetEnvironmentVariable("Path", $cleanMachinePath, "Machine")
        Write-Host "✅ Removed SyntricDB from System PATH environment variable." -ForegroundColor Green
    }
}

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "🎉 SyntricDB has been completely uninstalled from Windows!" -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Cyan
