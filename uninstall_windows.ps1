# SyntricDB Windows Root Uninstaller
$scriptPath = Join-Path $PSScriptRoot "deploy\windows\uninstall_windows.ps1"
if (Test-Path $scriptPath) {
    & $scriptPath @args
} else {
    Write-Error "Could not find uninstaller script at $scriptPath"
}
