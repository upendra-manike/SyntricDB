@echo off
echo ==========================================================================
echo SyntricDB Windows Native Installer
echo ==========================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy\windows\install_windows.ps1"
pause
