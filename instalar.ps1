<#
  CleanYourTV PS4 - instalador automático para Windows
  ====================================================

  Convierte un televisor o una caja con Android TV / Google TV en un inicio estilo PS4:
  instala todo lo necesario en este ordenador, encuentra el televisor, hace una copia de seguridad,
  (opcionalmente) limpia el software de fábrica, compila e instala el launcher desde el código fuente,
  aplica tu tema y, al final, cierra el acceso ADB.

  Uso (doble clic en instalar.bat, o desde PowerShell):
      .\instalar.ps1
      .\instalar.ps1 -TvIp 192.168.1.50            # sin buscar el televisor
      .\instalar.ps1 -TvIp 192.168.1.50 -SoloTema   # solo volver a enviar el tema

  Todo se instala por usuario, sin permisos de administrador, en:
      %LOCALAPPDATA%\CleanYourTV-PS4   (Python, Java 17, Android SDK y ADB)
  Nada se desinstala del televisor: las apps de fábrica solo se desactivan y desinstalar.ps1 lo deshace.

  Basado en CleanYourTV (https://github.com/Gathin23/CleanYourTV), licencia MIT.
#>
param(
    [string]$TvIp = '',
    [switch]$SoloTema,
    [switch]$SinLimpieza,
    [string]$Repo = 'https://github.com/JeisonCadenaC/cleanyourtv-PS4theme'
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # Invoke-WebRequest es 10 veces más lento con la barra de progreso
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
try { [Console]::OutputEncoding = [Text.Encoding]::UTF8 } catch { }
# Los comandos nativos reciben UTF-8 sin BOM (con BOM, el primer comando que se envía al televisor se corrompe).
$OutputEncoding = New-Object Text.UTF8Encoding $false

# ---------------------------------------------------------------------------------------------------------------
# Presentación y ayudas
# ---------------------------------------------------------------------------------------------------------------

$Base = Join-Path $env:LOCALAPPDATA 'CleanYourTV-PS4'
New-Item -ItemType Directory -Force -Path $Base | Out-Null
$Log = Join-Path $Base 'instalar.log'
try { Start-Transcript -Path $Log -Append | Out-Null } catch { }

function Titulo([string]$t) {
    Write-Host ''
    Write-Host ('=' * 78) -ForegroundColor DarkCyan
    Write-Host "  $t" -ForegroundColor Cyan
    Write-Host ('=' * 78) -ForegroundColor DarkCyan
}
function Paso([string]$t) { Write-Host "  > $t" -ForegroundColor White }
function Ok([string]$t) { Write-Host "  [OK] $t" -ForegroundColor Green }
function Aviso([string]$t) { Write-Host "  [!] $t" -ForegroundColor Yellow }
function Falla([string]$t) {
    Write-Host ''
    Write-Host "  [X] $t" -ForegroundColor Red
    Write-Host "      Registro completo: $Log" -ForegroundColor DarkGray
    try { Stop-Transcript | Out-Null } catch { }
    Read-Host '  Pulsa Enter para salir'
    exit 1
}
function Pregunta([string]$t, [string]$porDefecto = '') {
    $sufijo = if ($porDefecto) { " [$porDefecto]" } else { '' }
    $r = Read-Host "  ? $t$sufijo"
    if ([string]::IsNullOrWhiteSpace($r)) { return $porDefecto }
    return $r.Trim()
}
function SiNo([string]$t, [bool]$porDefecto = $true) {
    $d = if ($porDefecto) { 'S/n' } else { 's/N' }
    while ($true) {
        $r = Read-Host "  ? $t ($d)"
        if ([string]::IsNullOrWhiteSpace($r)) { return $porDefecto }
        switch -Regex ($r.Trim().ToLower()) {
            '^(s|si|sí|y|yes)$' { return $true }
            '^(n|no)$' { return $false }
        }
    }
}
function Descargar([string]$url, [string]$destino) {
    Paso "Descargando $(Split-Path $url -Leaf) ..."
    for ($i = 1; $i -le 3; $i++) {
        try {
            Invoke-WebRequest -Uri $url -OutFile $destino -UseBasicParsing
            return
        } catch {
            if ($i -eq 3) { Falla "No se pudo descargar $url. ¿Hay conexión a Internet? ($($_.Exception.Message))" }
            Start-Sleep -Seconds 3
        }
    }
}
function Descomprimir([string]$zip, [string]$destino) {
    if (Test-Path $destino) { Remove-Item -Recurse -Force $destino }
    Expand-Archive -Path $zip -DestinationPath $destino -Force
    Remove-Item -Force $zip
}

Clear-Host
Write-Host ''
Write-Host '   ____ _                    __   __               _______     __' -ForegroundColor Cyan
Write-Host '  / ___| | ___  __ _ _ __    \ \ / /__  _   _ _ __|_   _\ \   / /' -ForegroundColor Cyan
Write-Host ' | |   | |/ _ \/ _` | ''_ \    \ V / _ \| | | | ''__| | |  \ \ / / ' -ForegroundColor Cyan
Write-Host ' | |___| |  __/ (_| | | | |    | | (_) | |_| | |    | |   \ V /  ' -ForegroundColor Cyan
Write-Host '  \____|_|\___|\__,_|_| |_|    |_|\___/ \__,_|_|    |_|    \_/   PS4' -ForegroundColor Cyan
Write-Host ''
Write-Host '  Instalador del inicio estilo PS4 para Android TV y Google TV' -ForegroundColor White
Write-Host '  Personalizado por JeisonCadenaC' -ForegroundColor DarkGray
Write-Host '  Basado en CleanYourTV (Gathin y colaboradores), licencia MIT.' -ForegroundColor DarkGray
Write-Host ''
Write-Host '  Antes de seguir, en el TELEVISOR:' -ForegroundColor Yellow
Write-Host '   1. Ajustes > Sistema (o Preferencias del dispositivo) > Información.'
Write-Host '   2. Pulsa OK 7 veces sobre "Compilación" / "Build" hasta ver "Ya eres desarrollador".'
Write-Host '   3. Vuelve atrás > Opciones para desarrolladores > activa "Depuración por USB"'
Write-Host '      (y "Depuración de red/ADB" o "Depuración inalámbrica" si aparecen).'
Write-Host '   4. El televisor y este ordenador deben estar en la misma red Wi-Fi o cable.'
Write-Host ''
Write-Host '  Guía paso a paso con fotos de los menús: docs\ps4-theme\TUTORIAL.md' -ForegroundColor DarkGray
Write-Host ''
if (-not (SiNo '¿Listo para empezar?' $true)) { exit 0 }

# ---------------------------------------------------------------------------------------------------------------
# 1. Herramientas del ordenador (sin administrador)
# ---------------------------------------------------------------------------------------------------------------

Titulo '1/9  Preparando las herramientas de este ordenador'

$libre = (Get-PSDrive -Name ($env:LOCALAPPDATA.Substring(0, 1))).Free
if ($libre -lt 4GB) { Aviso ("Quedan {0:N1} GB libres; se recomiendan 4 GB (Java + Android SDK + compilación)." -f ($libre / 1GB)) }

# Python 3 (las herramientas de CleanYourTV hablan ADB desde Python)
function Buscar-Python {
    foreach ($c in @(
            (Join-Path $Base 'python\python.exe'),
            'py -3', 'python', 'python3')) {
        try {
            if ($c -like '*.exe') { if (-not (Test-Path $c)) { continue }; $v = & $c --version 2>&1 }
            elseif ($c -eq 'py -3') { $v = & py -3 --version 2>&1; if ($LASTEXITCODE -eq 0) { return (& py -3 -c 'import sys;print(sys.executable)') } else { continue } }
            else { $v = & $c --version 2>&1 }
            if ($LASTEXITCODE -eq 0 -and "$v" -match 'Python 3\.(\d+)' -and [int]$Matches[1] -ge 9) {
                if ($c -like '*.exe') { return $c }
                return (& $c -c 'import sys;print(sys.executable)')
            }
        } catch { }
    }
    return $null
}
$python = Buscar-Python
if (-not $python) {
    Paso 'Python 3 no está instalado: se instala solo para tu usuario.'
    $inst = Join-Path $env:TEMP 'python-instalador.exe'
    Descargar 'https://www.python.org/ftp/python/3.12.8/python-3.12.8-amd64.exe' $inst
    $destPy = Join-Path $Base 'python'
    $p = Start-Process -FilePath $inst -Wait -PassThru -ArgumentList @(
        '/quiet', 'InstallAllUsers=0', 'PrependPath=0', 'Include_test=0', 'Include_launcher=0',
        'Shortcuts=0', 'AssociateFiles=0', "TargetDir=$destPy")
    Remove-Item -Force $inst -ErrorAction SilentlyContinue
    if ($p.ExitCode -ne 0) { Falla "El instalador de Python terminó con código $($p.ExitCode)." }
    $python = Join-Path $destPy 'python.exe'
}
Ok "Python: $python"

# Java 17 (para compilar el launcher)
$jdkDir = Join-Path $Base 'jdk17'
$java = Get-ChildItem -Path $jdkDir -Recurse -Filter java.exe -ErrorAction SilentlyContinue | Where-Object { $_.FullName -like '*\bin\java.exe' } | Select-Object -First 1
if (-not $java) {
    Paso 'Instalando Java 17 (Eclipse Temurin, portable) ...'
    $zip = Join-Path $env:TEMP 'jdk17.zip'
    Descargar 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' $zip
    Descomprimir $zip $jdkDir
    $java = Get-ChildItem -Path $jdkDir -Recurse -Filter java.exe | Where-Object { $_.FullName -like '*\bin\java.exe' } | Select-Object -First 1
    if (-not $java) { Falla 'No se encontró java.exe después de descomprimir Java 17.' }
}
$env:JAVA_HOME = Split-Path (Split-Path $java.FullName)
Ok "Java 17: $env:JAVA_HOME"

# Android SDK: plataforma 34, build-tools 34.0.0 y platform-tools (ADB)
$sdk = Join-Path $Base 'android-sdk'
$sdkmanager = Join-Path $sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
if (-not (Test-Path $sdkmanager)) {
    Paso 'Instalando las herramientas de Android SDK ...'
    $zip = Join-Path $env:TEMP 'cmdline-tools.zip'
    Descargar 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' $zip
    $tmp = Join-Path $Base 'cmdline-tmp'
    Descomprimir $zip $tmp
    New-Item -ItemType Directory -Force -Path (Join-Path $sdk 'cmdline-tools') | Out-Null
    $latest = Join-Path $sdk 'cmdline-tools\latest'
    if (Test-Path $latest) { Remove-Item -Recurse -Force $latest }
    Move-Item (Join-Path $tmp 'cmdline-tools') $latest
    Remove-Item -Recurse -Force $tmp
}
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
if (-not (Test-Path (Join-Path $sdk 'build-tools\34.0.0')) -or -not (Test-Path (Join-Path $sdk 'platforms\android-34')) -or
    -not (Test-Path (Join-Path $sdk 'platform-tools\adb.exe'))) {
    Paso 'Aceptando licencias y descargando Android 14 (SDK 34), build-tools y ADB (~350 MB) ...'
    $si = ('y' + [Environment]::NewLine) * 30
    $si | & $sdkmanager --sdk_root="$sdk" --licenses *> $null
    & $sdkmanager --sdk_root="$sdk" 'platforms;android-34' 'build-tools;34.0.0' 'platform-tools' | Out-Null
    if (-not (Test-Path (Join-Path $sdk 'build-tools\34.0.0'))) { Falla 'sdkmanager no instaló build-tools 34.0.0.' }
}
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$env:PATH = "$(Join-Path $sdk 'platform-tools');$env:JAVA_HOME\bin;$env:PATH"
Ok "Android SDK y ADB: $sdk"

# ---------------------------------------------------------------------------------------------------------------
# 2. El proyecto
# ---------------------------------------------------------------------------------------------------------------

Titulo '2/9  Descargando el proyecto'

$Proyecto = $null
if ($PSScriptRoot -and (Test-Path (Join-Path $PSScriptRoot 'launcher\build-and-install.ps1'))) {
    $Proyecto = $PSScriptRoot
    Ok "Usando esta copia: $Proyecto"
} else {
    $Proyecto = Join-Path $env:USERPROFILE 'cleanyourtv-PS4theme'
    if (Test-Path (Join-Path $Proyecto 'launcher\build-and-install.ps1')) {
        Ok "Ya estaba descargado en $Proyecto"
    } else {
        $zip = Join-Path $env:TEMP 'cleanyourtv-ps4.zip'
        Descargar "$Repo/archive/refs/heads/main.zip" $zip
        $tmp = Join-Path $env:TEMP 'cleanyourtv-ps4'
        Descomprimir $zip $tmp
        $carpeta = Get-ChildItem $tmp -Directory | Select-Object -First 1
        Move-Item $carpeta.FullName $Proyecto
        Remove-Item -Recurse -Force $tmp
        Ok "Proyecto en $Proyecto"
    }
}
Set-Location $Proyecto

$venvPy = Join-Path $Proyecto 'tools\venv\Scripts\python.exe'
if (-not (Test-Path $venvPy)) {
    Paso 'Creando el entorno de Python de las herramientas ...'
    & $python -m venv (Join-Path $Proyecto 'tools\venv')
    if ($LASTEXITCODE -ne 0) { Falla 'No se pudo crear tools\venv.' }
}
& $venvPy -m pip install --quiet --disable-pip-version-check -r (Join-Path $Proyecto 'tools\requirements.txt')
if ($LASTEXITCODE -ne 0) { Falla 'pip no pudo instalar tools\requirements.txt.' }
Ok 'Herramientas de Python listas'

function Tv([string]$comando) {
    $out = & $venvPy -I (Join-Path $Proyecto 'tools\tv.py') $comando 2>&1
    $script:tvCodigo = $LASTEXITCODE
    return ($out | Out-String).Trim()
}

# ---------------------------------------------------------------------------------------------------------------
# 3. Encontrar y conectar el televisor
# ---------------------------------------------------------------------------------------------------------------

Titulo '3/9  Conectando con el televisor'

function Puerto-Abierto([string]$ip, [int]$puerto, [int]$ms = 400) {
    $c = New-Object Net.Sockets.TcpClient
    try {
        $r = $c.BeginConnect($ip, $puerto, $null, $null)
        if ($r.AsyncWaitHandle.WaitOne($ms) -and $c.Connected) { return $true }
        return $false
    } catch { return $false } finally { $c.Close() }
}

if (-not $TvIp) {
    Paso 'Buscando televisores con ADB abierto en tu red (solo equipos que este PC ya conoce) ...'
    $vecinos = @()
    try {
        $vecinos = Get-NetNeighbor -AddressFamily IPv4 -ErrorAction Stop |
            Where-Object { $_.State -in 'Reachable', 'Stale', 'Delay', 'Probe' -and $_.IPAddress -notmatch '\.(0|255)$' -and $_.IPAddress -notlike '224.*' -and $_.IPAddress -notlike '239.*' } |
            Select-Object -ExpandProperty IPAddress -Unique
    } catch { }
    $encontrados = @($vecinos | Where-Object { Puerto-Abierto $_ 5555 })
    if ($encontrados.Count -gt 0) {
        Write-Host '  Encontrados:'
        for ($i = 0; $i -lt $encontrados.Count; $i++) { Write-Host "    [$($i + 1)] $($encontrados[$i])" }
        $sel = Pregunta 'Elige el número de tu televisor (o escribe su IP)' '1'
        if ($sel -match '^\d+$' -and [int]$sel -ge 1 -and [int]$sel -le $encontrados.Count) { $TvIp = $encontrados[[int]$sel - 1] } else { $TvIp = $sel }
    } else {
        Aviso 'No encontré el televisor automáticamente.'
        Write-Host '  Mira su IP en el televisor: Ajustes > Red e Internet > (tu red) > Dirección IP.'
        $TvIp = Pregunta 'Escribe la IP del televisor (por ejemplo 192.168.1.50)'
    }
}
if ($TvIp -notmatch '^\d{1,3}(\.\d{1,3}){3}$') { Falla "'$TvIp' no parece una dirección IP." }
$env:TV_HOST = $TvIp

if (-not (Puerto-Abierto $TvIp 5555 1500)) {
    Aviso "El puerto 5555 de $TvIp está cerrado."
    Write-Host '  Muchos Google TV solo ofrecen "Depuración inalámbrica". En el televisor:'
    Write-Host '   Opciones para desarrolladores > Depuración inalámbrica > Vincular dispositivo con código.'
    if (SiNo '¿Quieres vincularlo ahora con el código que muestra el televisor?' $true) {
        $vinc = Pregunta 'IP y puerto de VINCULACIÓN que muestra la TV (ej. 192.168.1.50:37123)'
        $codigo = Pregunta 'Código de 6 dígitos'
        & $adb pair $vinc $codigo
        $con = Pregunta 'IP y puerto de CONEXIÓN de la pantalla de Depuración inalámbrica (ej. 192.168.1.50:41235)'
        & $adb connect $con | Out-Host
        & $adb -s $con tcpip 5555 | Out-Host
        Start-Sleep -Seconds 4
    }
    if (-not (Puerto-Abierto $TvIp 5555 2000)) {
        Write-Host ''
        Write-Host '  Si tu televisor es Samsung (Tizen), LG (webOS), Roku, Hisense VIDAA o Vizio, no es Android:'
        Write-Host '  este proyecto no aplica. Para quitarle el rastreo con el control: https://cleanyour.tv/not-android'
        Falla "No hay acceso ADB en $TvIp:5555. Activa la depuración de red y vuelve a ejecutar el instalador."
    }
}

Write-Host ''
Write-Host '  >>> Mira el televisor: si aparece "¿Permitir la depuración USB?", marca' -ForegroundColor Yellow
Write-Host '      "Permitir siempre desde este ordenador" y pulsa PERMITIR.' -ForegroundColor Yellow
$probe = ''
for ($i = 1; $i -le 6; $i++) {
    $probe = Tv '--probe'
    if ($tvCodigo -eq 0) { break }
    Start-Sleep -Seconds 5
}
if ($tvCodigo -ne 0) { Falla "No se pudo hablar con el televisor:`n$probe" }
$modelo = (Tv 'getprop ro.product.model')
$marca = (Tv 'getprop ro.product.brand')
$sdkTv = [int]((Tv 'getprop ro.build.version.sdk') -replace '\D', '')
$version = (Tv 'getprop ro.build.version.release')
Ok "Conectado: $marca $modelo (Android $version, SDK $sdkTv)"
if ($sdkTv -lt 26) {
    Falla "Este televisor tiene Android $version (SDK $sdkTv). El launcher necesita Android 8.0 (SDK 26) o superior."
}

# ---------------------------------------------------------------------------------------------------------------
# 4. Copia de seguridad
# ---------------------------------------------------------------------------------------------------------------

$seguro = ($modelo -replace '[^A-Za-z0-9_-]', '_')
$backup = Join-Path $Proyecto ("backup\$seguro-" + (Get-Date -Format 'yyyyMMdd-HHmmss'))
if (-not $SoloTema) {
    Titulo '4/9  Copia de seguridad del televisor'
    New-Item -ItemType Directory -Force -Path $backup | Out-Null
    (Tv 'pm list packages -f') | Set-Content -Encoding UTF8 (Join-Path $backup 'paquetes.txt')
    (Tv 'pm list packages -d') | Set-Content -Encoding UTF8 (Join-Path $backup 'desactivados.txt')
    (Tv 'getprop') | Set-Content -Encoding UTF8 (Join-Path $backup 'getprop.txt')
    foreach ($n in 'global', 'secure', 'system') { (Tv "settings list $n") | Set-Content -Encoding UTF8 (Join-Path $backup "settings-$n.txt") }
    (Tv 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME') |
        Set-Content -Encoding UTF8 (Join-Path $backup 'inicio-original.txt')
    Ok "Guardada en $backup (es privada: no la subas a Internet)"
}

# ---------------------------------------------------------------------------------------------------------------
# 5. Limpieza opcional del software de fábrica
# ---------------------------------------------------------------------------------------------------------------

$listaUsada = ''
if (-not $SoloTema -and -not $SinLimpieza) {
    Titulo '5/9  Limpieza de apps de fábrica (opcional)'
    Write-Host '  El proyecto trae listas probadas por modelo. Solo DESACTIVAN apps de rastreo y publicidad;'
    Write-Host '  no borran nada y desinstalar.ps1 lo deshace. Nunca tocan HDMI, el control, el teclado ni Play Store.'
    $candidatos = @()
    foreach ($j in Get-ChildItem (Join-Path $Proyecto 'examples') -Recurse -Filter tv.json -ErrorAction SilentlyContinue) {
        try { $info = Get-Content $j.FullName -Raw -Encoding UTF8 | ConvertFrom-Json } catch { continue }
        $carp = $j.Directory
        if (-not (Test-Path (Join-Path $carp.FullName 'debloat.txt'))) { continue }
        $texto = "$($carp.Name) $($info.tv) $($info.brand)".ToLower()
        $puntos = 0
        if ($marca -and $texto.Contains($marca.ToLower())) { $puntos += 2 }
        foreach ($p in ($modelo.ToLower() -split '[^a-z0-9]+' | Where-Object { $_.Length -ge 3 })) { if ($texto.Contains($p)) { $puntos += 1 } }
        if ($puntos -ge 2) { $candidatos += [pscustomobject]@{ Puntos = $puntos; Carpeta = $carp; Tv = $info.tv; So = $info.os } }
    }
    $candidatos = @($candidatos | Sort-Object Puntos -Descending | Select-Object -First 8)
    if ($candidatos.Count -eq 0) {
        Aviso "No hay una lista probada para $marca $modelo. Se omite la limpieza (el launcher funciona igual)."
        Write-Host '  Puedes hacerla después con un asistente de IA siguiendo PROMPT.md.'
    } else {
        Write-Host ''
        Write-Host '  Listas que coinciden con tu televisor:'
        for ($i = 0; $i -lt $candidatos.Count; $i++) { Write-Host "    [$($i + 1)] $($candidatos[$i].Tv)  ($($candidatos[$i].So))" }
        Write-Host '    [0] No limpiar nada'
        $sel = Pregunta 'Elige una lista' '0'
        if ($sel -match '^\d+$' -and [int]$sel -ge 1 -and [int]$sel -le $candidatos.Count) {
            $lista = Join-Path $candidatos[[int]$sel - 1].Carpeta.FullName 'debloat.txt'
            Write-Host ''
            Write-Host '  Lo que hará esta lista (lee los comentarios: dicen qué conservar):' -ForegroundColor DarkGray
            Get-Content $lista -Encoding UTF8 | Where-Object { $_ -match '^\s*[a-z]' } | ForEach-Object { Write-Host "    - $_" -ForegroundColor DarkGray }
            if (SiNo '¿Aplicar esta lista?' $true) {
                try {
                    & (Join-Path $Proyecto 'tools\disable.ps1') -TvHost $TvIp $lista
                    if ($LASTEXITCODE -ne 0) { Aviso 'La lista terminó con avisos (lee arriba). Los paquetes protegidos se respetaron.' }
                    else { Ok 'Limpieza aplicada' }
                } catch {
                    Aviso "La limpieza se detuvo: $($_.Exception.Message). El resto de la instalación continúa."
                }
                $listaUsada = $lista
            }
        }
    }
}

# ---------------------------------------------------------------------------------------------------------------
# 6. Compilar e instalar el launcher
# ---------------------------------------------------------------------------------------------------------------

if (-not $SoloTema) {
    Titulo '6/9  Compilando e instalando el launcher (5-10 minutos la primera vez)'
    $esGoogleTv = (Tv 'pm list packages com.google.android.apps.tv.launcherx') -match 'launcherx'
    $ayudante = $false
    Write-Host '  Ayudante de botones del control: devuelve la función a botones como Ajustes (engrane) o YouTube'
    Write-Host '  que dependían del inicio de fábrica. En algunos Google TV hace que mantener pulsado el volumen'
    Write-Host '  suba de uno en uno. Se puede desactivar luego en Ajustes > Accesibilidad.'
    $ayudante = SiNo '¿Activar el ayudante de botones?' $esGoogleTv
    $parametros = @{ TvHost = $TvIp }
    if ($ayudante) { $parametros.HomeKeyService = $true }
    Push-Location (Join-Path $Proyecto 'launcher')
    try {
        & (Join-Path $Proyecto 'launcher\build-and-install.ps1') @parametros
    } catch {
        Pop-Location
        Falla "La instalación del launcher se detuvo: $($_.Exception.Message)`n      El inicio de fábrica se dejó como estaba."
    }
    Pop-Location
    Ok 'Launcher instalado y elegido como inicio'

    Titulo '7/9  Permisos de las funciones PS4'
    Write-Host '  - Notificaciones: para la pantalla de notificaciones y el reproductor de música.'
    Write-Host '  - Uso de apps: para poner primero la última app abierta y el disco girando.'
    Write-Host '  - Bluetooth: para ver tus dispositivos conectados.'
    if (SiNo '¿Conceder estos permisos?' $true) {
        Tv 'cmd notification allow_listener com.gathin.tvhome/com.gathin.tvhome.Ps4Listener' | Out-Null
        Tv 'appops set com.gathin.tvhome GET_USAGE_STATS allow' | Out-Null
        if ($sdkTv -ge 31) { Tv 'pm grant com.gathin.tvhome android.permission.BLUETOOTH_CONNECT' | Out-Null }
        Ok 'Permisos concedidos'
    }
}

# ---------------------------------------------------------------------------------------------------------------
# 8. Tu tema: nombre, fondo y foto
# ---------------------------------------------------------------------------------------------------------------

Titulo '8/9  Tu tema PS4'
Add-Type -AssemblyName System.Drawing

function Guardar-Jpg([string]$origen, [string]$destino, [int]$ancho, [int]$alto) {
    # Escala "cubriendo" el tamaño pedido y recorta el centro (como un fondo de pantalla).
    $img = [Drawing.Image]::FromFile($origen)
    try {
        $esc = [Math]::Max($ancho / $img.Width, $alto / $img.Height)
        $w = [int]($img.Width * $esc); $h = [int]($img.Height * $esc)
        $bmp = New-Object Drawing.Bitmap $ancho, $alto
        $g = [Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.DrawImage($img, [int](($ancho - $w) / 2), [int](($alto - $h) / 2), $w, $h)
        $g.Dispose()
        $codec = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
        $p = New-Object Drawing.Imaging.EncoderParameters 1
        $p.Param[0] = New-Object Drawing.Imaging.EncoderParameter ([Drawing.Imaging.Encoder]::Quality), 90L
        $bmp.Save($destino, $codec, $p)
        $bmp.Dispose()
    } finally { $img.Dispose() }
}

$tema = Join-Path ([Environment]::GetFolderPath('MyDocuments')) 'Tema-PS4'
New-Item -ItemType Directory -Force -Path $tema | Out-Null
$cfgFile = Join-Path $tema 'config.json'
if (-not (Test-Path $cfgFile)) { Copy-Item (Join-Path $Proyecto 'docs\ps4-theme\tema-ejemplo\config.json') $cfgFile }
$cfg = Get-Content $cfgFile -Raw -Encoding UTF8 | ConvertFrom-Json

$nombre = Pregunta '¿Qué nombre muestro en tu perfil y en el saludo?' ($(if ($cfg.ps4_user_name) { $cfg.ps4_user_name } else { $env:USERNAME }))
$cfg | Add-Member -NotePropertyName ps4_user_name -NotePropertyValue $nombre -Force

Write-Host ''
Write-Host '  Fondo de pantalla: arrastra aquí una imagen (JPG o PNG, mejor 1920x1080) o deja vacío para el'
Write-Host '  fondo azul PS4. El color del tema se toma solo de tu fondo.'
$fondo = (Pregunta 'Imagen de fondo' '').Trim('"')
if ($fondo -and (Test-Path $fondo)) {
    Guardar-Jpg $fondo (Join-Path $tema 'wallpaper.jpg') 1920 1080
    Ok 'Fondo preparado (1920x1080)'
} elseif ($fondo) { Aviso "No encontré '$fondo'; se usará el fondo actual o el azul PS4." }

$avatar = (Pregunta 'Foto de perfil (opcional; arrastra una imagen o deja vacío)' '').Trim('"')
if ($avatar -and (Test-Path $avatar)) {
    Guardar-Jpg $avatar (Join-Path $tema 'avatar.jpg') 256 256
    Ok 'Foto de perfil preparada'
}

$cfg | ConvertTo-Json -Depth 5 | ForEach-Object { [IO.File]::WriteAllText($cfgFile, $_, (New-Object Text.UTF8Encoding $false)) }

Paso 'Enviando el tema al televisor ...'
& (Join-Path $Proyecto 'tools\theme.ps1') -TvHost $TvIp $tema
if ($LASTEXITCODE -ne 0) { Falla 'No se pudo enviar el tema (mira el mensaje de arriba).' }

# avatar.jpg y sounds/ no los envía theme.ps1: van por el proveedor de temas del launcher.
function Enviar-Extra([string]$archivo, [string]$ruta) {
    $tmp = '/data/local/tmp/cyt-extra'
    & $venvPy -I (Join-Path $Proyecto 'tools\b64push.py') $archivo $tmp | Out-Null
    Tv "content write --uri content://com.gathin.tvhome.theme/$ruta < $tmp; rm -f $tmp" | Out-Null
}
$extras = $false
if (Test-Path (Join-Path $tema 'avatar.jpg')) { Enviar-Extra (Join-Path $tema 'avatar.jpg') 'avatar.jpg'; $extras = $true }
foreach ($s in Get-ChildItem (Join-Path $tema 'sounds') -File -ErrorAction SilentlyContinue) {
    if ($s.Name -match '^(move|ok|back)\.(ogg|mp3|wav)$') { Enviar-Extra $s.FullName "sounds/$($s.Name)"; $extras = $true }
}
if ($extras) { & $venvPy -I (Join-Path $Proyecto 'tools\theme_restart.py') | Out-Null }
Ok "Tema aplicado. Tu carpeta de tema: $tema"

$capturas = Join-Path $Proyecto 'capturas'
New-Item -ItemType Directory -Force -Path $capturas | Out-Null
$foto = Join-Path $capturas 'resultado.png'
Start-Sleep -Seconds 3
& $venvPy -I (Join-Path $Proyecto 'tools\tv.py') --screenshot $foto HOME | Out-Null
if (Test-Path $foto) { Ok "Captura del resultado: $foto"; Invoke-Item $foto }

# Datos para desinstalar.ps1 (solo en este ordenador; contiene la IP local)
@{ tv = $TvIp; lista = $listaUsada; backup = $backup; tema = $tema; modelo = "$marca $modelo"; fecha = (Get-Date -Format s) } |
    ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $Base 'mi-instalacion.json')

# ---------------------------------------------------------------------------------------------------------------
# 9. Cerrar ADB
# ---------------------------------------------------------------------------------------------------------------

Titulo '9/9  Cerrar el acceso ADB (recomendado)'
Write-Host '  Mientras ADB esté abierto, cualquier equipo de tu red puede controlar el televisor.'
Write-Host '  El launcher sigue funcionando con ADB cerrado. Para cambiar el tema más adelante, vuelve a activar'
Write-Host '  la depuración en el televisor y ejecuta:  .\instalar.ps1 -SoloTema'
if (SiNo '¿Cerrar ADB ahora? (el televisor se reiniciará)' $true) {
    # Depuración inalámbrica: su puerto cambia en cada arranque, así que se comprueba antes de reiniciar.
    $inalam = (& $adb mdns services 2>$null) -join "`n"
    $puertoInal = $null
    if ($inalam -match [regex]::Escape($TvIp) + ':(\d+)') { $puertoInal = $Matches[1] }
    Tv 'settings put global adb_wifi_enabled 0' | Out-Null
    if ($puertoInal) {
        Start-Sleep -Seconds 2
        & $venvPy -I (Join-Path $Proyecto 'tools\adb-closed.py') $TvIp $puertoInal
        if ($LASTEXITCODE -eq 0) { Ok "Depuración inalámbrica cerrada (puerto $puertoInal)" } else { Aviso 'La depuración inalámbrica sigue abierta: apágala en el televisor.' }
    }
    Paso 'Apagando la depuración y reiniciando el televisor ...'
    Tv 'settings put global adb_enabled 0; reboot' | Out-Null
    Write-Host '  Esperando a que el televisor vuelva a arrancar (hasta 2 minutos) ...'
    $cerrado = $false
    for ($i = 0; $i -lt 24; $i++) {
        Start-Sleep -Seconds 5
        & $venvPy -I (Join-Path $Proyecto 'tools\adb-closed.py') $TvIp 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $cerrado = $true; break }
    }
    if ($cerrado) { Ok 'El puerto 5555 está cerrado: ADB quedó desactivado.' }
    else {
        Aviso 'No pude confirmar que el puerto 5555 esté cerrado.'
        Write-Host '  En el televisor: Opciones para desarrolladores > desactiva Depuración USB, Depuración de red e'
        Write-Host '  inalámbrica y "Revocar autorizaciones de depuración". Después comprueba con:'
        Write-Host "     tools\venv\Scripts\python.exe tools\adb-closed.py $TvIp"
    }
} else {
    Aviso 'ADB sigue abierto. Ciérralo cuando termines (Opciones para desarrolladores).'
}

Titulo '¡Listo! Tu televisor ya tiene el inicio estilo PS4'
Write-Host '  - Mantén OK sobre una app (o el botón Menú) para moverla, marcarla como favorita o desinstalarla.'
Write-Host '  - El botón de engrane del control (o el engrane de la barra superior) abre el Menú rápido.'
Write-Host '  - Menú rápido > Tema cambia el color; "Automático" lo toma de tu fondo.'
Write-Host '  - Para deshacer todo:  .\desinstalar.ps1'
Write-Host ''
Write-Host "  Registro de esta instalación: $Log" -ForegroundColor DarkGray
try { Stop-Transcript | Out-Null } catch { }
Read-Host '  Pulsa Enter para cerrar'
