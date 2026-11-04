#!/usr/bin/env bash
# CleanYourTV PS4 - deshacer la instalacion (Linux / macOS)
# =============================================================
#
# Devuelve el televisor a como estaba: quita el tema, vuelve a activar el inicio de fabrica y las apps
# desactivadas por la limpieza, y (si quieres) desinstala el launcher PS4.
#
# Necesita la depuracion ADB activa otra vez en el televisor:
#   Ajustes > Sistema > Opciones para desarrolladores > Depuracion USB / de red.
#
# Uso:  ./desinstalar.sh                (usa los datos guardados por instalar.sh)
#       ./desinstalar.sh --tv 192.168.1.50
set -uo pipefail

TV_IP=''
while [ $# -gt 0 ]; do
  case "$1" in
    --tv) TV_IP="${2:-}"; shift 2 ;;
    *) echo "Opcion no reconocida: $1" >&2; exit 2 ;;
  esac
done

BASE="$HOME/.local/share/CleanYourTV-PS4"
PROYECTO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_PY="$PROYECTO/tools/venv/bin/python"
[ -x "$VENV_PY" ] || { echo 'Primero ejecuta instalar.sh (crea las herramientas que usa este script).' >&2; exit 1; }

ARCHIVO="$BASE/mi-instalacion.json"
DATOS_TV=''; DATOS_LISTA=''
if [ -f "$ARCHIVO" ]; then
  DATOS_TV=$("$VENV_PY" -I -c "import json; print(json.load(open('$ARCHIVO')).get('tv',''))" 2>/dev/null || true)
  DATOS_LISTA=$("$VENV_PY" -I -c "import json; print(json.load(open('$ARCHIVO')).get('lista',''))" 2>/dev/null || true)
fi
[ -z "$TV_IP" ] && TV_IP="$DATOS_TV"
if [ -z "$TV_IP" ]; then read -r -p '? IP del televisor: ' TV_IP; fi

tv() { TV_HOST="$TV_IP" "$VENV_PY" -I "$PROYECTO/tools/tv.py" "$@" 2>&1; }

echo
echo "  Deshaciendo la instalacion PS4 en $TV_IP"
echo '  Personalizado por JeisonCadenaC'
echo '  Si el televisor pregunta "¿Permitir la depuracion USB?", acepta.'
TV_HOST="$TV_IP" "$VENV_PY" -I "$PROYECTO/tools/tv.py" --probe || { echo '  No hay conexion ADB: activa la depuracion en el televisor y vuelve a intentarlo.' >&2; exit 1; }

echo '  > Quitando el tema ...'
TV_HOST="$TV_IP" "$PROYECTO/tools/theme.sh" --reset >/dev/null

# El inicio de fabrica: launcherx en Google TV, tvlauncher en Android TV.
STOCK='com.google.android.tvlauncher'
LX=$(tv 'pm list packages com.google.android.apps.tv.launcherx')
[[ "$LX" == *launcherx* ]] && STOCK='com.google.android.apps.tv.launcherx'

if [ -n "$DATOS_LISTA" ] && [ -f "$DATOS_LISTA" ]; then
  echo "  > Reactivando las apps de la limpieza y el inicio de fabrica ($STOCK) ..."
  TV_HOST="$TV_IP" "$PROYECTO/tools/restore.sh" "$DATOS_LISTA" "$STOCK"
else
  echo "  > Reactivando el inicio de fabrica ($STOCK) ..."
  tv "pm enable $STOCK" >/dev/null
  ACTIVIDAD=$(tv "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME -p $STOCK" | tail -n 1)
  [[ "$ACTIVIDAD" == */* ]] && tv "cmd package set-home-activity --user 0 $(printf '%s' "$ACTIVIDAD" | tr -d '[:space:]')" >/dev/null
fi

tv 'input keyevent KEYCODE_HOME' >/dev/null
sleep 3
ENFOQUE=$(tv 'dumpsys activity activities | grep mFocusedApp')
echo "  Inicio actual: $ENFOQUE"

read -r -p '? ¿Desinstalar tambien el launcher PS4 (com.gathin.tvhome)? (s/N) ' RESPUESTA
if [[ "$(printf '%s' "$RESPUESTA" | tr '[:upper:]' '[:lower:]')" =~ ^(s|si|sí|y)$ ]]; then
  if [[ "$ENFOQUE" == *com.gathin.tvhome* ]]; then
    echo '  El launcher PS4 sigue siendo el inicio; no lo desinstalo para no dejar el televisor sin inicio.'
    echo '  Elige el inicio de fabrica en Ajustes > Apps > Apps predeterminadas y vuelve a ejecutar esto.'
  else
    tv 'pm uninstall com.gathin.tvhome'
  fi
fi
echo
echo '  Listo. Recuerda volver a apagar la depuracion en Opciones para desarrolladores.'
