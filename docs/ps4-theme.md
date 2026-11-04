# Guía del modo de inicio inspirado en PS4

Este fork de [CleanYourTV](https://github.com/Gathin23/CleanYourTV) añade una interfaz opcional inspirada en
la navegación de PS4 al launcher de Android TV. No es software de Sony, no ejecuta juegos y no ofrece
funciones de consola ni de PlayStation Network.

La [portada del proyecto](../README.md) presenta las funciones y las capturas editadas del televisor.
Las imágenes de este repositorio se tomaron con ADB y se prepararon para publicación: fondos y datos
personales, de red, reproducción y notificaciones se sustituyeron por contenido de ejemplo. No se incluyen
capturas de referencia de Sony ni fondos oficiales.

## Activar

El modo es optativo y se activa desde el archivo `config.json` de un tema. Añade estas claves a tu tema o
crea uno nuevo:

```json
{
  "ps4_layout": true,
  "ps4_accent": "auto",
  "ps4_idle_seconds": 10,
  "ps4_sounds": true,
  "ps4_user_name": ""
}
```

Los temas que no incluyen `"ps4_layout": true` siguen usando el diseño normal del launcher.

## Instalar en el televisor

La compilación requiere JDK 17 y Android SDK Platform y Build Tools 34. En Windows, prepara el entorno Python
desde la carpeta raíz del repositorio:

```powershell
python -m venv tools\venv
tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt
```

Activa ADB en el televisor y acepta la autorización del ordenador. Después compila e instala el launcher:

```powershell
.\launcher\build-and-install.ps1 -TvHost 192.168.1.50
```

Usa la dirección real de tu televisor en lugar de `192.168.1.50`. El instalador conserva sus protecciones
de Home; no desactives manualmente el launcher existente si el script no puede identificarlo.

Guarda el archivo `config.json` en una carpeta de tema y envíala al televisor:

```powershell
.\tools\theme.ps1 -TvHost 192.168.1.50 C:\ruta\al\tema-ps4
```

Para quitar el tema del televisor, ejecuta `.\tools\theme.ps1 -TvHost 192.168.1.50 -Reset`. En Linux y
macOS, usa los scripts equivalentes `launcher/build-and-install.sh` y `tools/theme.sh`.

## Opciones del tema

| Clave | Descripción |
|---|---|
| `ps4_layout` | `true` activa el modo PS4; si falta o es `false`, se muestra el inicio estándar. |
| `ps4_accent` | `"auto"` adapta el acento o usa un color hexadecimal como `"#1F5FC4"`. |
| `ps4_idle_seconds` | Segundos hasta ocultar temporalmente la fila de aplicaciones; el valor predeterminado es `10`, `0` desactiva esta función. |
| `ps4_sounds` | `true` activa los sonidos de navegación; `false` los silencia. |
| `ps4_user_name` | Nombre local opcional que aparece como perfil. No publiques tu nombre personal en temas compartidos. |

El acento que se elige desde el menú rápido se guarda en el televisor y puede sustituir el valor del tema.
También se pueden incluir recursos propios:

```text
tema-ps4/
├── config.json
├── wallpaper.jpg
├── avatar.jpg
└── sounds/
    ├── move.ogg
    ├── ok.ogg
    └── back.ogg
```

Incluye únicamente recursos propios o con una licencia que permita publicarlos y redistribuirlos. No uses
fondos, iconos o sonidos oficiales de Sony sin permiso.

## Controles

- Usa las flechas del mando para mover el foco, **OK** para abrir y **Atrás** para regresar.
- Mantén **OK** sobre una tarjeta para mostrar las acciones disponibles para esa aplicación.
- El menú rápido ofrece accesos a tema, sonido, energía, red, imagen, notificaciones, biblioteca,
  personalización y configuración.
- La tecla de ajustes puede abrir el menú rápido; su comportamiento depende del mando y de la configuración
  del fabricante.
- Tras diez segundos sin teclas, la fila se oculta temporalmente; cualquier botón la muestra de nuevo.
  Configura `"ps4_idle_seconds": 0` para desactivar este comportamiento.

## Accesos opcionales

Las funciones que consultan información del sistema requieren accesos de Android. No tienes que habilitarlos
si no quieres usar la función correspondiente.

| Acceso | Función |
|---|---|
| Acceso a notificaciones | Mostrar y borrar notificaciones; también permite consultar sesiones multimedia activas. Se concede desde los ajustes de Acceso a notificaciones del televisor. |
| Acceso de uso | Mostrar indicadores de aplicación usada recientemente. Se concede desde Acceso especial de aplicaciones. |
| `BLUETOOTH_CONNECT` | Mostrar dispositivos emparejados en la sección Sonido/dispositivos. |
| `ACCESS_FINE_LOCATION` | Android lo exige en algunas versiones para leer el nombre de la red Wi-Fi; el instalador intenta concederlo por ADB. El launcher lo usa para esa etiqueta, no para obtener coordenadas. |

El acceso a notificaciones puede exponer el contenido de notificaciones a la interfaz del launcher. Habilítalo
solo si quieres usar las pantallas de notificaciones o el reproductor multimedia. El launcher no declara el
permiso `INTERNET` ni envía estos datos a un servicio remoto.

## Compilar y probar

Desde `launcher\`, en Windows:

```powershell
.\gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

Prueba la navegación con el control remoto del televisor real: foco en los bordes, retorno con Atrás, accesos
de ajustes y cualquier tecla de fabricante. El comportamiento de una tecla inyectada puede diferir del botón
físico del mando.

## Créditos

- El launcher y las herramientas se basan en [Gathin23/CleanYourTV](https://github.com/Gathin23/CleanYourTV),
  creado por Gathin y sus colaboradores. Se conservan sus avisos y la [licencia MIT](../LICENSE).
- La adaptación PS4 y esta guía son de [JeisonCadenaC](https://github.com/JeisonCadenaC).
- Las licencias de Outfit y Bebas Neue están en
  [`launcher/FONT-LICENSE-OFL.txt`](../launcher/FONT-LICENSE-OFL.txt) y
  [`launcher/FONT-LICENSE-BEBAS-NEUE-OFL.txt`](../launcher/FONT-LICENSE-BEBAS-NEUE-OFL.txt).
- PlayStation, PS4 y Sony son marcas de sus propietarios. Este proyecto es independiente y no está afiliado
  ni respaldado por Sony Interactive Entertainment.
