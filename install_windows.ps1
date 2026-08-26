# SyntricDB Windows Root Installer
$scriptPath = Join-Path $PSScriptRoot "deploy\windows\install_windows.ps1"
if (Test-Path $scriptPath) {
    & $scriptPath @args
} else {
    Write-Error "Could not find installer script at $scriptPath"
}
