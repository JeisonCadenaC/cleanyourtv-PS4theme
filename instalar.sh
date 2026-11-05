#!/usr/bin/env bash
# CleanYourTV PS4 - instalador automatico para Linux y macOS
# =============================================================
#
# Convierte un televisor o una caja con Android TV / Google TV en un inicio estilo PS4:
# instala todo lo necesario en este ordenador, encuentra el televisor, hace una copia de seguridad,
# (opcionalmente) limpia el software de fabrica, compila e instala el launcher desde el codigo fuente,
# aplica tu tema y, al final, cierra el acceso ADB.
#
# El televisor se reinicia 2 o 3 veces durante el proceso (no solo al final): cada reinicio intermedio
# comprueba, con ADB todavia activo, que el televisor arranca bien antes de aplicar el siguiente cambio.
# Esto evita quedarse con un televisor (o un script) que parece congelado sin ADB para recuperarlo, que
# es justo lo que puede pasar si el "ayudante de botones" se activa en el mismo paso que el cambio de
# inicio (ver docs/TROUBLESHOOTING.md).
#
# Uso:
#   ./instalar.sh
#   ./instalar.sh --tv 192.168.1.50            # sin buscar el televisor
#   ./instalar.sh --tv 192.168.1.50 --solo-tema  # solo volver a enviar el tema
#   ./instalar.sh --sin-limpieza                # omite el paso de limpieza de apps de fabrica
#
# Todo se instala por usuario, sin "sudo" para Java ni el SDK de Android, en:
#   ~/.local/share/CleanYourTV-PS4   (Java 17 y Android SDK/ADB)
# Nada se desinstala del televisor: las apps de fabrica solo se desactivan y desinstalar.sh lo deshace.
#
# Basado en CleanYourTV (https://github.com/Gathin23/CleanYourTV), licencia MIT.
set -uo pipefail

TV_IP=''
SOLO_TEMA=0
SIN_LIMPIEZA=0
REPO='https://github.com/JeisonCadenaC/cleanyourtv-PS4theme'
while [ $# -gt 0 ]; do
  case "$1" in
    --tv) TV_IP="${2:-}"; shift 2 ;;
    --solo-tema) SOLO_TEMA=1; shift ;;
    --sin-limpieza) SIN_LIMPIEZA=1; shift ;;
    *) echo "Opcion no reconocida: $1" >&2; exit 2 ;;
  esac
done

BASE="$HOME/.local/share/CleanYourTV-PS4"
mkdir -p "$BASE"
LOG="$BASE/instalar.log"
exec > >(tee -a "$LOG") 2>&1

# ------------------------------------------------------------------------------------------------
# Presentacion y ayudas
# ------------------------------------------------------------------------------------------------

C_CYAN='\033[36m'; C_DCYAN='\033[2;36m'; C_WHITE='\033[97m'; C_GREEN='\033[32m'
C_YELLOW='\033[33m'; C_RED='\033[31m'; C_GRAY='\033[90m'; C_RESET='\033[0m'

titulo() { echo; printf "${C_DCYAN}%s${C_RESET}\n" "$(printf '=%.0s' $(seq 1 78))"; printf "  ${C_CYAN}%s${C_RESET}\n" "$1"; printf "${C_DCYAN}%s${C_RESET}\n" "$(printf '=%.0s' $(seq 1 78))"; }
paso() { printf "  > ${C_WHITE}%s${C_RESET}\n" "$1"; }
ok() { printf "  ${C_GREEN}[OK]${C_RESET} %s\n" "$1"; }
aviso() { printf "  ${C_YELLOW}[!]${C_RESET} %s\n" "$1"; }
falla() {
  echo
  printf "  ${C_RED}[X]${C_RESET} %s\n" "$1"
  printf "      ${C_GRAY}Registro completo: %s${C_RESET}\n" "$LOG"
  read -r -p '  Pulsa Enter para salir ' _
  exit 1
}
pregunta() {
  local texto="$1" defecto="${2:-}" sufijo r
  sufijo=''; [ -n "$defecto" ] && sufijo=" [$defecto]"
  read -r -p "  ? $texto$sufijo " r
  [ -z "$r" ] && { printf '%s' "$defecto"; return; }
  printf '%s' "$r"
}
sino() {
  local texto="$1" defecto="${2:-s}" d r
  [ "$defecto" = s ] && d='S/n' || d='s/N'
  while true; do
    read -r -p "  ? $texto ($d) " r
    [ -z "$r" ] && { [ "$defecto" = s ] && return 0 || return 1; }
    case "$(printf '%s' "$r" | tr '[:upper:]' '[:lower:]')" in
      s|si|sí|y|yes) return 0 ;;
      n|no) return 1 ;;
    esac
  done
}
descargar() {
  local url="$1" destino="$2" i
  paso "Descargando $(basename "$url") ..."
  for i in 1 2 3; do
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 2 -o "$destino" "$url" && return
    elif command -v wget >/dev/null 2>&1; then
      wget -q -O "$destino" "$url" && return
    else
      falla 'Ni curl ni wget estan instalados; instala uno de los dos y vuelve a ejecutar este script.'
    fi
    [ "$i" -eq 3 ] && falla "No se pudo descargar $url. Revisa la conexion a Internet."
    sleep 3
  done
}

clear 2>/dev/null || true
echo
printf "${C_CYAN}   ____ _                    __   __               _______     __${C_RESET}\n"
printf "${C_CYAN}  / ___| | ___  __ _ _ __    \\\\ \\\\ / /__  _   _ _ __|_   _\\\\ \\\\   / /${C_RESET}\n"
printf "${C_CYAN} | |   | |/ _ \\\\/ _\` | '_ \\\\    \\\\ V / _ \\\\| | | | '__| | |  \\\\ \\\\ / / ${C_RESET}\n"
printf "${C_CYAN} | |___| |  __/ (_| | | | |    | | (_) | |_| | |    | |   \\\\ V /  ${C_RESET}\n"
printf "${C_CYAN}  \\\\____|_|\\\\___|\\\\__,_|_| |_|    |_|\\\\___/ \\\\__,_|_|    |_|    \\\\_/   PS4${C_RESET}\n"
echo
echo '  Instalador del inicio estilo PS4 para Android TV y Google TV (Linux / macOS)'
printf "  ${C_GRAY}Personalizado por JeisonCadenaC${C_RESET}\n"
printf "  ${C_GRAY}Basado en CleanYourTV (Gathin y colaboradores), licencia MIT.${C_RESET}\n"
echo
printf "  ${C_YELLOW}Antes de seguir, en el TELEVISOR:${C_RESET}\n"
echo '   1. Ajustes > Sistema (o Preferencias del dispositivo) > Informacion.'
echo '   2. Pulsa OK 7 veces sobre "Compilacion" / "Build" hasta ver "Ya eres desarrollador".'
echo '   3. Vuelve atras > Opciones para desarrolladores > activa "Depuracion por USB"'
echo '      (y "Depuracion de red/ADB" o "Depuracion inalambrica" si aparecen).'
echo '   4. El televisor y este ordenador deben estar en la misma red Wi-Fi o cable.'
echo
printf "  ${C_GRAY}Guia paso a paso con capturas: docs/ps4-theme/TUTORIAL.md${C_RESET}\n"
echo
sino '¿Listo para empezar?' s || exit 0

SO="$(uname -s)"
ARQ="$(uname -m)"
case "$SO" in
  Darwin) PLATAFORMA=mac ;;
  Linux) PLATAFORMA=linux ;;
  *) falla "Sistema operativo no soportado: $SO. Usa instalar.ps1 en Windows." ;;
esac
case "$ARQ" in
  x86_64|amd64) ARQ_JDK=x64; ARQ_SDK=x64 ;;
  arm64|aarch64) ARQ_JDK=aarch64; ARQ_SDK=aarch64 ;;
  *) falla "Arquitectura no reconocida: $ARQ" ;;
esac

# ------------------------------------------------------------------------------------------------
# 1. Herramientas del ordenador (sin sudo para Java ni el SDK de Android)
# ------------------------------------------------------------------------------------------------

titulo '1/10  Preparando las herramientas de este ordenador'

PYTHON=''
for c in python3 python; do
  if command -v "$c" >/dev/null 2>&1; then
    v="$("$c" --version 2>&1)"
    if [[ "$v" =~ Python\ 3\.([0-9]+) ]] && [ "${BASH_REMATCH[1]}" -ge 9 ]; then PYTHON="$c"; break; fi
  fi
done
if [ -z "$PYTHON" ]; then
  aviso 'Python 3.9 o superior no esta instalado.'
  if [ "$PLATAFORMA" = mac ] && command -v brew >/dev/null 2>&1; then
    if sino '¿Instalar Python 3 con Homebrew ahora?' s; then
      brew install python3 || falla 'Homebrew no pudo instalar Python 3.'
      PYTHON=python3
    fi
  elif [ "$PLATAFORMA" = linux ]; then
    GESTOR=''
    if command -v apt-get >/dev/null 2>&1; then GESTOR='sudo apt-get update && sudo apt-get install -y python3 python3-venv'
    elif command -v dnf >/dev/null 2>&1; then GESTOR='sudo dnf install -y python3'
    elif command -v pacman >/dev/null 2>&1; then GESTOR='sudo pacman -S --noconfirm python'
    elif command -v zypper >/dev/null 2>&1; then GESTOR='sudo zypper install -y python3'
    fi
    if [ -n "$GESTOR" ] && sino "¿Instalar Python 3 ahora con: $GESTOR ?" s; then
      eval "$GESTOR" || falla 'No se pudo instalar Python 3 con el gestor de paquetes detectado.'
      PYTHON=python3
    fi
  fi
  [ -z "$PYTHON" ] && falla 'Instala Python 3.9+ (https://www.python.org/downloads/) y vuelve a ejecutar este script.'
fi
ok "Python: $($PYTHON --version 2>&1) ($PYTHON)"

JDK_DIR="$BASE/jdk17"
JAVA_BIN="$(find "$JDK_DIR" -type f -name java -path '*/bin/java' 2>/dev/null | head -n1)"
if [ -z "$JAVA_BIN" ]; then
  paso 'Instalando Java 17 (Eclipse Temurin, portable) ...'
  TARBALL="$BASE/jdk17.tar.gz"
  descargar "https://api.adoptium.net/v3/binary/latest/17/ga/$PLATAFORMA/$ARQ_JDK/jdk/hotspot/normal/eclipse" "$TARBALL"
  rm -rf "$JDK_DIR"; mkdir -p "$JDK_DIR"
  tar -xzf "$TARBALL" -C "$JDK_DIR"
  rm -f "$TARBALL"
  JAVA_BIN="$(find "$JDK_DIR" -type f -name java -path '*/bin/java' 2>/dev/null | head -n1)"
  [ -z "$JAVA_BIN" ] && falla 'No se encontro java despues de descomprimir Java 17.'
fi
# java siempre vive en <JAVA_HOME>/bin/java, en Linux y en macOS (donde el tarball anida
# Contents/Home/bin/java): un nivel por encima de "bin" es JAVA_HOME en los dos casos.
export JAVA_HOME="$(cd "$(dirname "$JAVA_BIN")/.." && pwd)"
ok "Java 17: $JAVA_HOME"

if [ "$PLATAFORMA" = mac ]; then SDK_DIR="$HOME/Library/Android/sdk"; SDK_OS=mac; else SDK_DIR="$HOME/Android/Sdk"; SDK_OS=linux; fi
SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  paso 'Instalando las herramientas de Android SDK ...'
  ZIP="$BASE/cmdline-tools.zip"
  descargar "https://dl.google.com/android/repository/commandlinetools-${SDK_OS}-11076708_latest.zip" "$ZIP"
  TMP="$BASE/cmdline-tmp"
  rm -rf "$TMP"; mkdir -p "$TMP"
  (command -v unzip >/dev/null 2>&1 && unzip -q "$ZIP" -d "$TMP") || "$PYTHON" -I -c "import zipfile,sys; zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])" "$ZIP" "$TMP"
  rm -f "$ZIP"
  mkdir -p "$SDK_DIR/cmdline-tools"
  rm -rf "$SDK_DIR/cmdline-tools/latest"
  mv "$TMP/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
  rm -rf "$TMP"
fi
export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
if [ ! -d "$SDK_DIR/build-tools/34.0.0" ] || [ ! -d "$SDK_DIR/platforms/android-34" ] || [ ! -x "$SDK_DIR/platform-tools/adb" ]; then
  paso 'Aceptando licencias y descargando Android 14 (SDK 34), build-tools y ADB (~350 MB) ...'
  yes 2>/dev/null | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null 2>&1 || true
  "$SDKMANAGER" --sdk_root="$SDK_DIR" 'platforms;android-34' 'build-tools;34.0.0' 'platform-tools' >/dev/null
  [ -d "$SDK_DIR/build-tools/34.0.0" ] || falla 'sdkmanager no instalo build-tools 34.0.0.'
fi
ADB="$SDK_DIR/platform-tools/adb"
export PATH="$SDK_DIR/platform-tools:$JAVA_HOME/bin:$PATH"
ok "Android SDK y ADB: $SDK_DIR"

# ------------------------------------------------------------------------------------------------
# 2. El proyecto
# ------------------------------------------------------------------------------------------------

titulo '2/10  Descargando el proyecto'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/launcher/build-and-install.sh" ]; then
  PROYECTO="$SCRIPT_DIR"
  ok "Usando esta copia: $PROYECTO"
else
  PROYECTO="$HOME/cleanyourtv-PS4theme"
  if [ -f "$PROYECTO/launcher/build-and-install.sh" ]; then
    ok "Ya estaba descargado en $PROYECTO"
  elif command -v git >/dev/null 2>&1; then
    paso "Clonando $REPO ..."
    git clone --depth 1 "$REPO" "$PROYECTO" || falla 'git clone fallo. Revisa la conexion a Internet.'
    ok "Proyecto en $PROYECTO"
  else
    ZIP="$BASE/cleanyourtv-ps4.zip"
    descargar "$REPO/archive/refs/heads/main.zip" "$ZIP"
    TMP="$BASE/cleanyourtv-ps4"
    rm -rf "$TMP"; mkdir -p "$TMP"
    (command -v unzip >/dev/null 2>&1 && unzip -q "$ZIP" -d "$TMP") || "$PYTHON" -I -c "import zipfile,sys; zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])" "$ZIP" "$TMP"
    rm -f "$ZIP"
    mv "$TMP"/*/ "$PROYECTO"
    rm -rf "$TMP"
    ok "Proyecto en $PROYECTO"
  fi
fi
cd "$PROYECTO"

VENV_PY="$PROYECTO/tools/venv/bin/python"
if [ ! -x "$VENV_PY" ]; then
  paso 'Creando el entorno de Python de las herramientas ...'
  "$PYTHON" -m venv "$PROYECTO/tools/venv" || falla 'No se pudo crear tools/venv.'
fi
"$VENV_PY" -m pip install --quiet --disable-pip-version-check -r "$PROYECTO/tools/requirements.txt" || falla 'pip no pudo instalar tools/requirements.txt.'
ok 'Herramientas de Python listas'

tv() { TV_HOST="$TV_IP" TV_CONNECT_TIMEOUT_S="${TV_CONNECT_TIMEOUT_S:-120}" "$VENV_PY" -I "$PROYECTO/tools/tv.py" "$@" 2>&1; }

probar_adb() {
  local intentos="${1:-6}" espera="${2:-5}" i probe=''
  # 20s por intento, no 120 en silencio (se ve igual que un script congelado aunque el televisor este
  # bien) - pero solo aqui, exportado nada mas que para esta funcion: tv() ya toma TV_CONNECT_TIMEOUT_S si
  # esta puesto, y puesto global tambien recortaria operaciones que de verdad necesitan mas de 20s (un
  # "reboot", un lote de limpieza), haciendolas fallar con un error en vez de simplemente tardar.
  for ((i = 1; i <= intentos; i++)); do
    printf "    (intento %s de %s; cada uno tarda hasta 20s) ...\n" "$i" "$intentos"
    probe=$(TV_CONNECT_TIMEOUT_S=20 tv --probe); TV_OK=$?
    [ "$TV_OK" -eq 0 ] && break
    sleep "$espera"
  done
  printf '%s' "$probe"
}

ping_responde() {
  if command -v ping >/dev/null 2>&1; then
    if [ "$PLATAFORMA" = mac ]; then ping -c 1 -t 1 "$1" >/dev/null 2>&1
    else ping -c 1 -W 1 "$1" >/dev/null 2>&1; fi
  else
    return 1
  fi
}

VISTO_EN_RED=0
# Espera hasta $1 minutos a que el comando $2 (evaluado con eval) salga con 0, comprobando cada 5s.
# Mientras espera, recuerda si el televisor llego a responder en la red (aunque $2 no se cumpliera):
# VISTO_EN_RED distingue "sigue arrancando, solo tarda" de "no reacciona nada" si se agota el tiempo.
esperar_televisor() {
  local minutos="$1" condicion="$2" fin
  VISTO_EN_RED=0
  fin=$(($(date +%s) + minutos * 60))
  while [ "$(date +%s)" -lt "$fin" ]; do
    sleep 5
    if [ "$VISTO_EN_RED" -eq 0 ] && ping_responde "$TV_IP"; then
      VISTO_EN_RED=1
      echo '    (el televisor ya responde en la red; esperando a que termine de arrancar)'
    fi
    eval "$condicion" && return 0
  done
  return 1
}

# Reinicia el televisor con ADB todavia activo y comprueba que vuelve a arrancar con el nuevo launcher
# como inicio, antes de aplicar el siguiente cambio. Si el televisor no responde, nunca cierra ADB por
# su cuenta: este ordenador sigue teniendo acceso para diagnosticar o deshacer el paso.
reinicio_de_verificacion() {
  local titulo_paso="$1" motivo="$2"
  titulo "$titulo_paso"
  echo "  $motivo"
  echo '  La depuracion ADB sigue activa durante este reinicio: si algo va mal, este ordenador podra'
  echo '  reconectarse y deshacer el cambio. ADB no se cierra hasta el ultimo paso del instalador.'
  tv reboot >/dev/null
  sleep 10
  if ! esperar_televisor 3 "puerto_abierto \"\$TV_IP\" 5555 1"; then
    echo
    echo '  [!] El televisor no respondio por ADB en 3 minutos tras reiniciar.'
    if [ "$VISTO_EN_RED" -eq 1 ]; then
      echo '      Sigue respondiendo en la red (no esta totalmente congelado), pero ADB no vuelve.'
    else
      echo '      No responde ni siquiera en la red: puede estar congelado en la animacion de arranque.'
    fi
    echo '      - Si el televisor suele tardar en arrancar, dale un poco mas de tiempo.'
    echo '      - Si no reacciona, desenchufalo 30 segundos y enchufalo de nuevo: ADB seguia activo y'
    echo '        nada se ha desinstalado, asi que un corte de corriente no pierde ningun cambio.'
    if ! sino '  ¿Sigo esperando 3 minutos mas?' s; then
      falla 'Instalacion detenida: el televisor no respondio tras el reinicio de comprobacion.'
    fi
    esperar_televisor 3 "puerto_abierto \"\$TV_IP\" 5555 1" || \
      falla "El televisor sigue sin responder por ADB tras reiniciar; no se continua para no arriesgar mas cambios encima.
      Cuando vuelva a responder, revisa con:  TV_HOST=$TV_IP tools/venv/bin/python tools/tv.py --probe"
  fi
  PROBE=$(probar_adb)
  [ "$TV_OK" -eq 0 ] || falla "El televisor respondio en la red pero no por ADB tras reiniciar (puede que haya que aceptar otra vez la depuracion en pantalla):
$PROBE"
  HOME_AHORA=$(tv 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME')
  if [[ "$HOME_AHORA" != *com.gathin.tvhome* ]]; then
    aviso "Tras reiniciar, el inicio ya no es el nuevo launcher (resolvio a '$HOME_AHORA')."
    echo '  En Google TV esto suele significar que la pantalla de configuracion (setupwraith) tambien reclama'
    echo '  el inicio: anadela a tu lista de limpieza (ver README, Solucion de problemas) y repite el instalador.'
    falla 'Se detiene aqui para no cerrar ADB con un inicio que no es el correcto. Repite el instalador o usa desinstalar.sh.'
  fi
  ok 'El televisor arranco bien tras reiniciar: el inicio sigue siendo el nuevo launcher.'
}

# ------------------------------------------------------------------------------------------------
# 3. Encontrar y conectar el televisor
# ------------------------------------------------------------------------------------------------

titulo '3/10  Conectando con el televisor'

puerto_abierto() {
  local ip="$1" puerto="${2:-5555}" espera="${3:-0.4}"
  "$VENV_PY" -I -c "
import socket, sys
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.settimeout(float(sys.argv[2]))
try:
    sys.exit(0 if s.connect_ex((sys.argv[0], int(sys.argv[1]))) == 0 else 1)
except OSError:
    sys.exit(1)
" "$ip" "$puerto" "$espera"
}

if [ -z "$TV_IP" ]; then
  paso 'Buscando televisores con ADB abierto en tu red (equipos que este ordenador ya conoce) ...'
  VECINOS=$(arp -a 2>/dev/null | grep -oE '\(([0-9]{1,3}\.){3}[0-9]{1,3}\)' | tr -d '()' | sort -u)
  ENCONTRADOS=()
  for ip in $VECINOS; do
    if puerto_abierto "$ip" 5555 0.4; then ENCONTRADOS+=("$ip"); fi
  done
  if [ "${#ENCONTRADOS[@]}" -gt 0 ]; then
    echo '  Encontrados:'
    for i in "${!ENCONTRADOS[@]}"; do echo "    [$((i + 1))] ${ENCONTRADOS[$i]}"; done
    sel=$(pregunta 'Elige el numero de tu televisor (o escribe su IP)' '1')
    if [[ "$sel" =~ ^[0-9]+$ ]] && [ "$sel" -ge 1 ] && [ "$sel" -le "${#ENCONTRADOS[@]}" ]; then
      TV_IP="${ENCONTRADOS[$((sel - 1))]}"
    else
      TV_IP="$sel"
    fi
  else
    aviso 'No encontre el televisor automaticamente.'
    echo '  Mira su IP en el televisor: Ajustes > Red e Internet > (tu red) > Direccion IP.'
    TV_IP=$(pregunta 'Escribe la IP del televisor (por ejemplo 192.168.1.50)')
  fi
fi
[[ "$TV_IP" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]] || falla "'$TV_IP' no parece una direccion IP."

if ! puerto_abierto "$TV_IP" 5555 1.5; then
  aviso "El puerto 5555 de $TV_IP esta cerrado."
  echo '  Muchos Google TV solo ofrecen "Depuracion inalambrica". En el televisor:'
  echo '   Opciones para desarrolladores > Depuracion inalambrica > Vincular dispositivo con codigo.'
  if sino '¿Quieres vincularlo ahora con el codigo que muestra el televisor?' s; then
    vinc=$(pregunta 'IP y puerto de VINCULACION que muestra la TV (ej. 192.168.1.50:37123)')
    codigo=$(pregunta 'Codigo de 6 digitos')
    "$ADB" pair "$vinc" "$codigo"
    con=$(pregunta 'IP y puerto de CONEXION de la pantalla de Depuracion inalambrica (ej. 192.168.1.50:41235)')
    "$ADB" connect "$con"
    "$ADB" -s "$con" tcpip 5555
    sleep 4
  fi
  if ! puerto_abierto "$TV_IP" 5555 2; then
    echo
    echo '  Si tu televisor es Samsung (Tizen), LG (webOS), Roku, Hisense VIDAA o Vizio, no es Android:'
    echo '  este proyecto no aplica. Para quitarle el rastreo con el control: https://cleanyour.tv/not-android'
    falla "No hay acceso ADB en $TV_IP:5555. Activa la depuracion de red y vuelve a ejecutar el instalador."
  fi
fi

echo
printf "  ${C_YELLOW}>>> Mira el televisor: si aparece \"¿Permitir la depuracion USB?\", marca${C_RESET}\n"
printf "      ${C_YELLOW}\"Permitir siempre desde este ordenador\" y pulsa PERMITIR.${C_RESET}\n"
PROBE='' ; TV_OK=1
for i in 1 2 3 4 5 6; do
  PROBE=$(tv --probe); TV_OK=$?
  [ "$TV_OK" -eq 0 ] && break
  sleep 5
done
[ "$TV_OK" -eq 0 ] || falla "No se pudo hablar con el televisor:
$PROBE"
MODELO=$(tv 'getprop ro.product.model')
MARCA=$(tv 'getprop ro.product.brand')
SDK_TV=$(tv 'getprop ro.build.version.sdk' | tr -dc '0-9')
VERSION=$(tv 'getprop ro.build.version.release')
ok "Conectado: $MARCA $MODELO (Android $VERSION, SDK $SDK_TV)"
[ -n "$SDK_TV" ] && [ "$SDK_TV" -ge 26 ] || falla "Este televisor tiene Android $VERSION (SDK $SDK_TV). El launcher necesita Android 8.0 (SDK 26) o superior."

# ------------------------------------------------------------------------------------------------
# 4. Copia de seguridad
# ------------------------------------------------------------------------------------------------

SEGURO=$(printf '%s' "$MODELO" | tr -c 'A-Za-z0-9_-' '_')
BACKUP="$PROYECTO/backup/${SEGURO}-$(date +%Y%m%d-%H%M%S)"
if [ "$SOLO_TEMA" -ne 1 ]; then
  titulo '4/10  Copia de seguridad del televisor'
  mkdir -p "$BACKUP"
  tv 'pm list packages -f' > "$BACKUP/paquetes.txt"
  tv 'pm list packages -d' > "$BACKUP/desactivados.txt"
  tv 'getprop' > "$BACKUP/getprop.txt"
  for n in global secure system; do tv "settings list $n" > "$BACKUP/settings-$n.txt"; done
  tv 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' > "$BACKUP/inicio-original.txt"
  ok "Guardada en $BACKUP (es privada: no la subas a Internet)"
fi

# ------------------------------------------------------------------------------------------------
# 5. Limpieza opcional del software de fabrica
# ------------------------------------------------------------------------------------------------

LISTA_USADA=''
if [ "$SOLO_TEMA" -ne 1 ] && [ "$SIN_LIMPIEZA" -ne 1 ]; then
  titulo '5/10  Limpieza de apps de fabrica (opcional)'
  echo '  El proyecto trae listas probadas por modelo. Solo DESACTIVAN apps de rastreo y publicidad;'
  echo '  no borran nada y desinstalar.sh lo deshace. Nunca tocan HDMI, el control, el teclado ni Play Store.'
  CANDIDATOS_FILE="$BASE/candidatos.tsv"
  : > "$CANDIDATOS_FILE"
  while IFS= read -r -d '' j; do
    carpeta="$(dirname "$j")"
    [ -f "$carpeta/debloat.txt" ] || continue
    info=$("$PYTHON" -I -c "
import json, sys
try:
    d = json.load(open(sys.argv[1], encoding='utf-8'))
except Exception:
    d = {}
print(d.get('tv', ''))
print(d.get('brand', ''))
print(d.get('os', ''))
" "$j")
  tvname=$(printf '%s' "$info" | sed -n 1p)
  brand=$(printf '%s' "$info" | sed -n 2p)
  so=$(printf '%s' "$info" | sed -n 3p)
  texto=$(printf '%s %s %s' "$carpeta" "$tvname" "$brand" | tr '[:upper:]' '[:lower:]')
  puntos=0
  marca_l=$(printf '%s' "$MARCA" | tr '[:upper:]' '[:lower:]')
  [ -n "$marca_l" ] && [[ "$texto" == *"$marca_l"* ]] && puntos=$((puntos + 2))
  for p in $(printf '%s' "$MODELO" | tr '[:upper:]' '[:lower:]' | tr -c 'a-z0-9' ' '); do
    [ "${#p}" -ge 3 ] && [[ "$texto" == *"$p"* ]] && puntos=$((puntos + 1))
  done
  [ "$puntos" -ge 2 ] && printf '%s\t%s\t%s\t%s\n' "$puntos" "$carpeta/debloat.txt" "$tvname" "$so" >> "$CANDIDATOS_FILE"
  done < <(find "$PROYECTO/examples" -name tv.json -print0 2>/dev/null)
  mapfile -t CANDIDATOS < <(sort -t"$(printf '\t')" -k1,1nr "$CANDIDATOS_FILE" 2>/dev/null | head -n 8)
  if [ "${#CANDIDATOS[@]}" -eq 0 ]; then
    aviso "No hay una lista probada para $MARCA $MODELO. Se omite la limpieza (el launcher funciona igual)."
    echo '  Puedes hacerla despues con un asistente de IA siguiendo PROMPT.md.'
  else
    echo
    echo '  Listas que coinciden con tu televisor:'
    for i in "${!CANDIDATOS[@]}"; do
      IFS=$'\t' read -r _ _ tvname so <<< "${CANDIDATOS[$i]}"
      echo "    [$((i + 1))] $tvname  ($so)"
    done
    echo '    [0] No limpiar nada'
    sel=$(pregunta 'Elige una lista' '0')
    if [[ "$sel" =~ ^[0-9]+$ ]] && [ "$sel" -ge 1 ] && [ "$sel" -le "${#CANDIDATOS[@]}" ]; then
      IFS=$'\t' read -r _ lista _ _ <<< "${CANDIDATOS[$((sel - 1))]}"
      echo
      printf "  ${C_GRAY}Lo que hara esta lista (lee los comentarios: dicen que conservar):${C_RESET}\n"
      grep -E '^\s*[a-z]' "$lista" | while read -r l; do printf "    ${C_GRAY}- %s${C_RESET}\n" "$l"; done
      if sino '¿Aplicar esta lista?' s; then
        if TV_HOST="$TV_IP" "$PROYECTO/tools/disable.sh" "$lista"; then
          ok 'Limpieza aplicada'
        else
          aviso 'La lista termino con avisos (lee arriba). Los paquetes protegidos se respetaron.'
        fi
        LISTA_USADA="$lista"
      fi
    fi
  fi
fi

# ------------------------------------------------------------------------------------------------
# 6-7. Compilar e instalar el launcher, y sus permisos
# ------------------------------------------------------------------------------------------------

if [ "$SOLO_TEMA" -ne 1 ]; then
  titulo '6/10  Compilando e instalando el launcher (5-10 minutos la primera vez)'
  ES_GOOGLE_TV=$(tv 'pm list packages com.google.android.apps.tv.launcherx')
  echo '  Ayudante de botones del control: devuelve la funcion a botones como Ajustes (engrane) o YouTube'
  echo '  que dependian del inicio de fabrica. En algunos Google TV hace que mantener pulsado el volumen'
  echo '  suba de uno en uno. Se puede desactivar luego en Ajustes > Accesibilidad.'
  if [[ "$ES_GOOGLE_TV" == *launcherx* ]]; then DEF_AYUDANTE=s; else DEF_AYUDANTE=n; fi
  if sino '¿Activar el ayudante de botones?' "$DEF_AYUDANTE"; then AYUDANTE=1; else AYUDANTE=0; fi
  if [ "$AYUDANTE" -eq 1 ]; then
    echo '  Por seguridad, el ayudante se activa despues de comprobar con un reinicio que el launcher por si'
    echo '  solo arranca bien: activarlo en el mismo paso que el cambio de inicio ha dejado televisores'
    echo '  congelados en el arranque antes de ahora (docs/TROUBLESHOOTING.md).'
  fi
  (
    cd "$PROYECTO/launcher"
    TV_HOST="$TV_IP" ./build-and-install.sh
  ) || falla 'La instalacion del launcher se detuvo (revisa el mensaje de arriba). El inicio de fabrica se dejo como estaba.'
  ok 'Launcher instalado y elegido como inicio'

  reinicio_de_verificacion '7/10  Reinicio de comprobacion (launcher)' \
    'Se reinicia el televisor para confirmar que arranca bien con el nuevo launcher antes de seguir.'

  if [ "$AYUDANTE" -eq 1 ]; then
    if (cd "$PROYECTO/launcher" && TV_HOST="$TV_IP" HOME_KEY_SERVICE=1 ./build-and-install.sh); then
      ok 'Ayudante de botones activado'
      reinicio_de_verificacion '7/10  Reinicio de comprobacion (ayudante de botones)' \
        'Se reinicia otra vez para confirmar que el ayudante de botones no afecta al arranque.'
    else
      aviso 'No se pudo activar el ayudante de botones. El launcher sigue instalado y es el inicio; se continua sin el.'
    fi
  fi

  titulo '8/10  Permisos de las funciones PS4'
  echo '  - Notificaciones: para la pantalla de notificaciones y el reproductor de musica.'
  echo '  - Uso de apps: para poner primero la ultima app abierta y el disco girando.'
  echo '  - Bluetooth: para ver tus dispositivos conectados.'
  if sino '¿Conceder estos permisos?' s; then
    tv 'cmd notification allow_listener com.gathin.tvhome/com.gathin.tvhome.Ps4Listener' >/dev/null
    tv 'appops set com.gathin.tvhome GET_USAGE_STATS allow' >/dev/null
    [ "$SDK_TV" -ge 31 ] && tv 'pm grant com.gathin.tvhome android.permission.BLUETOOTH_CONNECT' >/dev/null
    ok 'Permisos concedidos'
  fi
fi

# ------------------------------------------------------------------------------------------------
# 8. Tu tema: nombre, fondo y foto
# ------------------------------------------------------------------------------------------------

titulo '9/10  Tu tema PS4'

TEMA="$HOME/Tema-PS4"
mkdir -p "$TEMA"
CFG_FILE="$TEMA/config.json"
[ -f "$CFG_FILE" ] || cp "$PROYECTO/docs/ps4-theme/tema-ejemplo/config.json" "$CFG_FILE"

NOMBRE_ACTUAL=$("$PYTHON" -I -c "import json,sys; print(json.load(open(sys.argv[1])).get('ps4_user_name','') or '')" "$CFG_FILE")
NOMBRE=$(pregunta '¿Que nombre muestro en tu perfil y en el saludo?' "${NOMBRE_ACTUAL:-$USER}")
"$PYTHON" -I -c "
import json, sys
f = sys.argv[1]
d = json.load(open(f))
d['ps4_user_name'] = sys.argv[2]
json.dump(d, open(f, 'w'), indent=2, ensure_ascii=False)
" "$CFG_FILE" "$NOMBRE"

# Escala "cubriendo" 1920x1080 y recorta el centro, con la primera herramienta disponible; sin
# ninguna, copia la imagen tal cual (puede no verse exactamente igual en pantalla completa).
redimensionar() {
  local origen="$1" destino="$2"
  if [ "$PLATAFORMA" = mac ] && command -v sips >/dev/null 2>&1; then
    cp "$origen" "$destino"
    sips -Z 1920 "$destino" >/dev/null 2>&1
    return
  fi
  if command -v magick >/dev/null 2>&1; then
    magick "$origen" -resize '1920x1080^' -gravity center -extent 1920x1080 -quality 90 "$destino" && return
  elif command -v convert >/dev/null 2>&1; then
    convert "$origen" -resize '1920x1080^' -gravity center -extent 1920x1080 -quality 90 "$destino" && return
  fi
  cp "$origen" "$destino"
}

echo
echo '  Fondo de pantalla: arrastra aqui una imagen (JPG o PNG, mejor 1920x1080) o deja vacio para el'
echo '  fondo azul PS4. El color del tema se toma solo de tu fondo.'
FONDO=$(pregunta 'Imagen de fondo' '')
FONDO="${FONDO%\"}"; FONDO="${FONDO#\"}"
if [ -n "$FONDO" ] && [ -f "$FONDO" ]; then
  redimensionar "$FONDO" "$TEMA/wallpaper.jpg"
  ok 'Fondo preparado'
elif [ -n "$FONDO" ]; then
  aviso "No encontre '$FONDO'; se usara el fondo actual o el azul PS4."
fi

AVATAR=$(pregunta 'Foto de perfil (opcional; arrastra una imagen o deja vacio)' '')
AVATAR="${AVATAR%\"}"; AVATAR="${AVATAR#\"}"
if [ -n "$AVATAR" ] && [ -f "$AVATAR" ]; then
  redimensionar "$AVATAR" "$TEMA/avatar.jpg"
  ok 'Foto de perfil preparada'
fi

paso 'Enviando el tema al televisor ...'
TV_HOST="$TV_IP" "$PROYECTO/tools/theme.sh" "$TEMA" || falla 'No se pudo enviar el tema (mira el mensaje de arriba).'

# avatar.jpg y sounds/ no los envia theme.sh: van por el proveedor de temas del launcher.
enviar_extra() {
  local archivo="$1" ruta="$2" tmp='/data/local/tmp/cyt-extra'
  TV_HOST="$TV_IP" "$VENV_PY" -I "$PROYECTO/tools/b64push.py" "$archivo" "$tmp" >/dev/null
  tv "content write --uri content://com.gathin.tvhome.theme/$ruta < $tmp; rm -f $tmp" >/dev/null
}
EXTRAS=0
if [ -f "$TEMA/avatar.jpg" ]; then enviar_extra "$TEMA/avatar.jpg" 'avatar.jpg'; EXTRAS=1; fi
if [ -d "$TEMA/sounds" ]; then
  for s in "$TEMA"/sounds/*; do
    [ -f "$s" ] || continue
    nombre_archivo="$(basename "$s")"
    case "$nombre_archivo" in
      move.ogg|move.mp3|move.wav|ok.ogg|ok.mp3|ok.wav|back.ogg|back.mp3|back.wav) enviar_extra "$s" "sounds/$nombre_archivo"; EXTRAS=1 ;;
    esac
  done
fi
[ "$EXTRAS" -eq 1 ] && TV_HOST="$TV_IP" "$VENV_PY" -I "$PROYECTO/tools/theme_restart.py" >/dev/null
ok "Tema aplicado. Tu carpeta de tema: $TEMA"

CAPTURAS="$PROYECTO/capturas"
mkdir -p "$CAPTURAS"
FOTO="$CAPTURAS/resultado.png"
sleep 3
TV_HOST="$TV_IP" "$VENV_PY" -I "$PROYECTO/tools/tv.py" --screenshot "$FOTO" HOME >/dev/null
[ -f "$FOTO" ] && ok "Captura del resultado: $FOTO"

"$PYTHON" -I -c "
import json, sys
json.dump({'tv': sys.argv[1], 'lista': sys.argv[2], 'backup': sys.argv[3], 'tema': sys.argv[4],
           'modelo': sys.argv[5], 'fecha': sys.argv[6]}, open(sys.argv[7], 'w'))
" "$TV_IP" "$LISTA_USADA" "$BACKUP" "$TEMA" "$MARCA $MODELO" "$(date -Iseconds)" "$BASE/mi-instalacion.json"

# ------------------------------------------------------------------------------------------------
# 9. Cerrar ADB
# ------------------------------------------------------------------------------------------------

titulo '10/10  Cerrar el acceso ADB (recomendado)'
echo '  Mientras ADB este abierto, cualquier equipo de tu red puede controlar el televisor.'
echo '  El launcher sigue funcionando con ADB cerrado. Para cambiar el tema mas adelante, vuelve a activar'
echo '  la depuracion en el televisor y ejecuta:  ./instalar.sh --tv '"$TV_IP"' --solo-tema'
if sino '¿Cerrar ADB ahora? (el televisor se reiniciara)' s; then
  INALAM=$("$ADB" mdns services 2>/dev/null || true)
  PUERTO_INAL=''
  if [[ "$INALAM" =~ ${TV_IP//./\\.}:([0-9]+) ]]; then PUERTO_INAL="${BASH_REMATCH[1]}"; fi
  tv 'settings put global adb_wifi_enabled 0' >/dev/null
  if [ -n "$PUERTO_INAL" ]; then
    sleep 2
    if TV_HOST="$TV_IP" "$VENV_PY" -I "$PROYECTO/tools/adb-closed.py" "$TV_IP" "$PUERTO_INAL"; then
      ok "Depuracion inalambrica cerrada (puerto $PUERTO_INAL)"
    else
      aviso 'La depuracion inalambrica sigue abierta: apagala en el televisor.'
    fi
  fi
  paso 'Apagando la depuracion y reiniciando el televisor (ya se comprobo antes que arranca bien) ...'
  tv 'settings put global adb_enabled 0; reboot' >/dev/null
  echo '  Esperando a que el televisor vuelva a arrancar (hasta 3 minutos) ...'
  if esperar_televisor 3 "TV_HOST=\"\$TV_IP\" \"$VENV_PY\" -I \"$PROYECTO/tools/adb-closed.py\" \"\$TV_IP\" >/dev/null 2>&1"; then
    ok 'El puerto 5555 esta cerrado: ADB quedo desactivado.'
  elif [ "$VISTO_EN_RED" -eq 1 ]; then
    aviso 'El televisor responde en la red pero no pude confirmar que el puerto 5555 este cerrado.'
    echo '  En el televisor: Opciones para desarrolladores > desactiva Depuracion USB, Depuracion de red e'
    echo '  inalambrica y "Revocar autorizaciones de depuracion". Despues comprueba con:'
    echo "     tools/venv/bin/python tools/adb-closed.py $TV_IP"
  else
    echo
    echo '  [!] El televisor no respondio en absoluto tras este reinicio (ni siquiera por la red).'
    echo '      Como ya se comprobo antes que arranca bien con estos mismos cambios, esto suele ser solo'
    echo '      un arranque lento: espera unos minutos y mira la pantalla del televisor.'
    echo '      Si no reacciona nunca, desenchufalo 30 segundos: nada se desinstalo ni se perdio.'
  fi
else
  aviso 'ADB sigue abierto. Cierralo cuando termines (Opciones para desarrolladores).'
fi

titulo '¡Listo! Tu televisor ya tiene el inicio estilo PS4'
echo '  - Mantén OK sobre una app (o el boton Menu) para moverla, marcarla como favorita o desinstalarla.'
echo '  - El boton de engrane del control (o el engrane de la barra superior) abre el Menu rapido.'
echo '  - Menu rapido > Tema cambia el color; "Automatico" lo toma de tu fondo.'
echo '  - Para deshacer todo:  ./desinstalar.sh'
echo
printf "  ${C_GRAY}Registro de esta instalacion: %s${C_RESET}\n" "$LOG"
read -r -p '  Pulsa Enter para cerrar ' _
