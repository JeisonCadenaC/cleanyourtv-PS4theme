@echo off
rem Doble clic para instalar el inicio estilo PS4 en tu Android TV.
rem Abre el instalador de PowerShell sin cambiar la configuracion de seguridad de Windows.
chcp 65001 >nul
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0instalar.ps1" %*
