@echo off
echo ==========================================================================
echo ⚡ SyntricDB Windows Installer ⚡
echo ==========================================================================
powershell -ExecutionPolicy Bypass -File "%~dp0deploy\windows\install_windows.ps1"
pause
