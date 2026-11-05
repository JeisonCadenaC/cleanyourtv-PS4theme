<#
  CleanYourTV PS4 - deshacer la instalación
  =========================================

  Devuelve el televisor a como estaba: quita el tema, vuelve a activar el inicio de fábrica y las apps
  desactivadas por la limpieza, y (si quieres) desinstala el launcher PS4.

  Necesita la depuración ADB activa otra vez en el televisor:
    Ajustes > Sistema > Opciones para desarrolladores > Depuración USB / de red.

  Uso:  .\desinstalar.ps1            (usa los datos guardados por instalar.ps1)
        .\desinstalar.ps1 -TvIp 192.168.1.50
#>
param([string]$TvIp = '')

$ErrorActionPreference = 'Stop'
# Ver el comentario junto a esta misma línea en instalar.ps1: en PowerShell 7.3+ esto evita que un comando
# nativo (tv.py) que termine con código de salida distinto de cero tire abajo todo el script, aunque su
# salida ya esté fundida con "2>&1". No hace nada en Windows PowerShell 5.1.
$PSNativeCommandUseErrorActionPreference = $false
try { [Console]::OutputEncoding = [Text.Encoding]::UTF8 } catch { }
$OutputEncoding = New-Object Text.UTF8Encoding $false

$Base = Join-Path $env:LOCALAPPDATA 'CleanYourTV-PS4'
$Proyecto = $PSScriptRoot
$venvPy = Join-Path $Proyecto 'tools\venv\Scripts\python.exe'
if (-not (Test-Path $venvPy)) { Write-Host 'Primero ejecuta instalar.ps1 (crea las herramientas que usa este script).' -ForegroundColor Red; exit 1 }

$datos = $null
$archivo = Join-Path $Base 'mi-instalacion.json'
if (Test-Path $archivo) { $datos = Get-Content $archivo -Raw -Encoding UTF8 | ConvertFrom-Json }
if (-not $TvIp -and $datos) { $TvIp = $datos.tv }
if (-not $TvIp) { $TvIp = Read-Host '? IP del televisor' }
$env:TV_HOST = $TvIp

function Tv([string]$c) { (& $venvPy -I (Join-Path $Proyecto 'tools\tv.py') $c 2>&1 | Out-String).Trim() }

Write-Host ''
Write-Host "  Deshaciendo la instalación PS4 en $TvIp" -ForegroundColor Cyan
Write-Host '  Personalizado por JeisonCadenaC' -ForegroundColor DarkGray
Write-Host '  Si el televisor pregunta "¿Permitir la depuración USB?", acepta.' -ForegroundColor Yellow
# 2>&1: cuando el televisor no responde, tv.py escribe el motivo en su salida de error, y sin fundirla
# con la normal, $ErrorActionPreference = 'Stop' detendría este script con un error críptico en vez de
# dejar que la comprobación de abajo (que es justo el caso que se está probando) lo explique bien.
& $venvPy -I (Join-Path $Proyecto 'tools\tv.py') --probe 2>&1
if ($LASTEXITCODE -ne 0) { Write-Host '  No hay conexión ADB: activa la depuración en el televisor y vuelve a intentarlo.' -ForegroundColor Red; exit 1 }

Write-Host '  > Quitando el tema ...'
& (Join-Path $Proyecto 'tools\theme.ps1') -TvHost $TvIp -Reset | Out-Null

# El inicio de fábrica: launcherx en Google TV, tvlauncher en Android TV.
$stock = 'com.google.android.tvlauncher'
if ((Tv 'pm list packages com.google.android.apps.tv.launcherx') -match 'launcherx') { $stock = 'com.google.android.apps.tv.launcherx' }

if ($datos -and $datos.lista -and (Test-Path $datos.lista)) {
    Write-Host "  > Reactivando las apps de la limpieza y el inicio de fábrica ($stock) ..."
    & (Join-Path $Proyecto 'tools\restore.ps1') -TvHost $TvIp $datos.lista -Stock $stock
} else {
    Write-Host "  > Reactivando el inicio de fábrica ($stock) ..."
    Tv "pm enable $stock" | Out-Null
    $actividad = (Tv "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME -p $stock" | Select-Object -Last 1)
    if ("$actividad" -match '/') { Tv "cmd package set-home-activity --user 0 $("$actividad".Trim())" | Out-Null }
}

Tv 'input keyevent KEYCODE_HOME' | Out-Null
Start-Sleep -Seconds 3
$enfoque = Tv 'dumpsys activity activities | grep mFocusedApp'
Write-Host "  Inicio actual: $enfoque"

$respuesta = Read-Host '? ¿Desinstalar también el launcher PS4 (com.gathin.tvhome)? (s/N)'
if ($respuesta -match '^(s|si|sí|y)$') {
    if ($enfoque -match 'com\.gathin\.tvhome') {
        Write-Host '  El launcher PS4 sigue siendo el inicio; no lo desinstalo para no dejar el televisor sin inicio.' -ForegroundColor Yellow
        Write-Host '  Elige el inicio de fábrica en Ajustes > Apps > Apps predeterminadas y vuelve a ejecutar esto.'
    } else {
        Tv 'pm uninstall com.gathin.tvhome' | Out-Host
    }
}
Write-Host ''
Write-Host '  Listo. Recuerda volver a apagar la depuración en Opciones para desarrolladores.' -ForegroundColor Green
