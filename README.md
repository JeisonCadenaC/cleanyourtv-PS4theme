<p align="center">
  <img src="docs/ps4-theme/screenshots/home.png" alt="Inicio del launcher PS4 en Android TV, con una aplicación enfocada y controles multimedia" width="960">
</p>

<h1 align="center">Launcher Android TV con interfaz inspirada en PS4</h1>

<p align="center">
  Un inicio para Android TV y Google TV, navegable con el control remoto y personalizable con tus aplicaciones, fondo y colores.
</p>

<p align="center">
  <a href="https://github.com/JeisonCadenaC/cleanyourtv-PS4theme">Repositorio de este fork</a> ·
  <a href="#tutorial-completo-paso-a-paso-sin-experiencia-previa"><strong>Tutorial paso a paso (en esta página)</strong></a> ·
  <a href="docs/ps4-theme.md">Guía técnica corta</a> ·
  <a href="https://github.com/Gathin23/CleanYourTV">Proyecto base CleanYourTV</a>
</p>

> **Este es un fork independiente de CleanYourTV**, mantenido por [JeisonCadenaC](https://github.com/JeisonCadenaC).
> La modificación principal es un modo de inicio opcional inspirado en la navegación de PlayStation 4.
> No es software de Sony, no ejecuta juegos de PS4 y no se conecta a PlayStation Network.

> **Idioma:** este README en español es la documentación específica del fork. Los README en otros idiomas que
> aparecen abajo se heredaron de CleanYourTV y aún no describen la interfaz PS4 de esta versión.

## Índice

- [Qué es este proyecto](#qué-es-este-proyecto)
- [Capturas](#capturas)
- [Funciones](#funciones)
- [Requisitos](#requisitos)
- [Instalación en un paso](#instalación-en-un-paso-recomendado-para-cualquier-persona)
- [Tutorial completo, paso a paso (sin experiencia previa)](#tutorial-completo-paso-a-paso-sin-experiencia-previa)
  - [Qué vas a conseguir y qué necesitas](#qué-vas-a-conseguir-y-qué-necesitas)
  - [Lo que este proyecto nunca hace](#lo-que-este-proyecto-nunca-hace)
  - [Paso 1 — Comprueba que tu televisor es compatible](#paso-1--comprueba-que-tu-televisor-es-compatible)
  - [Paso 2 — Activa las Opciones de desarrollador](#paso-2--activa-las-opciones-de-desarrollador)
  - [Paso 3 — Activa la depuración (ADB)](#paso-3--activa-la-depuración-adb)
  - [Paso 4 — Misma red](#paso-4--conecta-el-televisor-y-el-ordenador-a-la-misma-red)
  - [Paso 5 — Descarga el proyecto](#paso-5--descarga-el-proyecto)
  - [Paso 6 — Ejecuta el instalador](#paso-6--ejecuta-el-instalador-automático)
  - [Qué hace el instalador, pantalla por pantalla](#qué-hace-el-instalador-paso-a-paso-en-pantalla)
  - [Cómo usar el inicio PS4](#después-de-instalar-cómo-usar-el-inicio-ps4)
  - [Cambiar el tema más adelante](#cómo-cambiar-el-tema-más-adelante)
  - [Deshacer todo (desinstalar)](#cómo-deshacer-todo-desinstalar)
  - [Solución de problemas](#solución-de-problemas)
  - [Preguntas frecuentes](#preguntas-frecuentes)
  - [Seguridad y privacidad](#seguridad-y-privacidad)
- [Instalación manual](#instalación-manual)
- [Conexión ADB](#conexión-adb)
- [Cerrar ADB al terminar](#cerrar-adb-al-terminar)
- [Configuración](#configuración)
- [Permisos y privacidad](#permisos-y-privacidad)
- [Compilar y comprobar](#compilar-y-comprobar)
- [Relación con CleanYourTV y créditos](#relación-con-cleanyourtv-y-créditos)
- [Aportaciones y problemas](#aportaciones-y-problemas)

<p align="center">
  Leer las traducciones heredadas:
  <a href="README.zh-CN.md">简体中文</a> ·
  <a href="README.zh-TW.md">正體中文</a> ·
  <a href="README.vi.md">Tiếng Việt</a> ·
  <a href="README.ja.md">日本語</a> ·
  <a href="README.it.md">Italiano</a> ·
  <a href="README.pt-BR.md">Português</a> ·
  <a href="README.nl.md">Nederlands</a> ·
  <a href="README.de.md">Deutsch</a> ·
  <a href="README.uk.md">Українська</a> ·
  <a href="README.tr.md">Türkçe</a>
</p>

## Qué es este proyecto

Este repositorio conserva el launcher Android TV de [CleanYourTV](https://github.com/Gathin23/CleanYourTV) y
añade una presentación opcional de pantalla completa, tarjetas de aplicaciones y menú rápido con estética
inspirada en PS4. Está pensado para navegar desde el sofá con las flechas, OK y Atrás del control remoto.

La interfaz PS4 no se activa para todos por defecto. Se habilita en un tema con `ps4_layout: true`; los temas
que no incluyan esa opción conservan la pantalla estándar del launcher. El modo PS4 no convierte el televisor
en una consola ni modifica, instala o emula juegos.

## Capturas

Las capturas muestran el launcher en ejecución en Android TV. Antes de publicar imágenes, asegúrate de que
no revelen datos personales, redes Wi-Fi o notificaciones privadas, y de que los fondos y recursos visuales
sean propios o tengan licencia para redistribuirse. Los iconos de aplicaciones visibles pertenecen a esas
aplicaciones y conservan sus marcas.

<table>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/ps4-theme/screenshots/quick-menu.png" alt="Menú rápido con perfil local, tema, sonido, Bluetooth, red, imagen, notificaciones y configuración" width="100%">
      <p align="center"><strong>Menú rápido</strong></p>
    </td>
    <td width="50%" valign="top">
      <img src="docs/ps4-theme/screenshots/library.png" alt="Biblioteca de aplicaciones de TV y aplicaciones instaladas" width="100%">
      <p align="center"><strong>Biblioteca</strong></p>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/ps4-theme/screenshots/notifications.png" alt="Pantalla de notificaciones del launcher" width="100%">
      <p align="center"><strong>Notificaciones</strong></p>
    </td>
    <td width="50%" valign="top">
      <img src="docs/ps4-theme/screenshots/screen-protector.png" alt="Protector de pantalla del launcher Android TV" width="100%">
      <p align="center"><strong>Protector de pantalla</strong></p>
    </td>
  </tr>
</table>

La pantalla de inicio está al comienzo de esta página. La tarjeta inferior es el reproductor multimedia de
Android; solo aparece si el sistema expone una sesión multimedia activa. Cambia `screen-protector-propia.png`
por una captura que tengas derecho a publicar.

## Funciones

| Área | Qué hace |
|---|---|
| **Inicio** | Presenta las aplicaciones de TV en una fila horizontal; la aplicación enfocada crece y muestra acciones para abrirla y consultar su información. |
| **Navegación** | Usa el foco y las teclas de dirección del control remoto; no requiere pantalla táctil. |
| **Menú rápido** | Agrupa perfil local, tema y acento, volumen y dispositivos Bluetooth, energía, red, imagen, notificaciones, personalización y configuración del televisor. |
| **Biblioteca** | Muestra las aplicaciones de TV y permite recorrer también las aplicaciones iniciables que no declaran una actividad de Android TV. |
| **Notificaciones** | Permite ver y borrar notificaciones mediante acceso opcional de Android. |
| **Reproducción** | Muestra controles y metadatos de la sesión multimedia cuando Android los proporciona y se habilita el acceso necesario. |
| **Personalización** | Acepta fondo, avatar, nombre local, acento automático o elegido y sonidos propios en un tema. |
| **Reposo visual** | Tras 10 segundos sin interacción, oculta temporalmente la fila para mostrar el fondo; cualquier tecla vuelve a mostrarla. Se puede desactivar. |
| **Accesos del launcher** | Conserva los ajustes habituales del launcher base, como orden de aplicaciones, etiquetas y aplicaciones ocultas. |

### Uso con el control remoto

- **Izquierda / derecha:** mover el foco entre aplicaciones o elementos del menú.
- **Arriba / abajo:** recorrer las zonas del inicio, las acciones de la aplicación y los controles disponibles.
- **OK:** abrir una aplicación o confirmar una opción.
- **Mantener OK sobre una aplicación:** abrir el menú contextual disponible para esa aplicación.
- **Atrás:** volver al panel o zona anterior.
- **Tecla de ajustes del control remoto:** puede abrir el menú rápido según el mando y la configuración del televisor.

La asignación de teclas depende del fabricante. Este fork incluye una asignación de YouTube TV para
`KEYCODE_BUTTON_3` en su configuración de launcher; los códigos físicos de los mandos varían y se pueden
personalizar en `config.xml`.

## Requisitos

- Un televisor o dispositivo con **Android TV o Google TV**, Android 8.0 / API 26 o posterior.
- Acceso ADB autorizado para instalar y configurar el launcher.
- Para compilar en Windows: PowerShell, Python 3, JDK 17, Android SDK Platform 34 y Build Tools 34.0.0.
- Para compilar en Linux o macOS: los mismos componentes de Android/Java y los scripts equivalentes `.sh`.

Este proyecto no es para Tizen, webOS, Roku OS, VIDAA ni televisores sin Android. No requiere root ni añade
permiso `INTERNET` al launcher. La instalación no es una limpieza general del televisor: este README se centra
en el launcher y su interfaz PS4.

## Instalación en un paso (recomendado para cualquier persona)

Instalador personalizado por JeisonCadenaC.

No necesitas saber programación ni usar la terminal. Un único script instala todo lo que falte en tu
ordenador (Python, Java 17, el SDK de Android), encuentra el televisor, hace copia de seguridad, compila
e instala el launcher y aplica tu tema, con preguntas en español en cada paso.

| Sistema | Qué ejecutar |
|---|---|
| **Windows** | Descarga el proyecto (botón verde "Code" → "Download ZIP" en esta página), descomprime y haz doble clic en [`instalar.bat`](instalar.bat). |
| **macOS / Linux** | `git clone https://github.com/JeisonCadenaC/cleanyourtv-PS4theme.git && cd cleanyourtv-PS4theme && ./instalar.sh` |

Para deshacer todo más adelante: `desinstalar.ps1` (Windows) o `desinstalar.sh` (macOS/Linux).

El tutorial completo que sigue explica cada pantalla de ese instalador, pensado para quien lo usa por
primera vez y no ha tocado Android antes. También existe como archivo independiente en
[`docs/ps4-theme/TUTORIAL.md`](docs/ps4-theme/TUTORIAL.md), por si prefieres leerlo fuera de GitHub.

## Tutorial completo, paso a paso (sin experiencia previa)

Esta sección no da por hecho ningún conocimiento previo de programación, ADB o Android. Sigue los pasos
en orden; cada uno dice exactamente qué vas a ver en pantalla y qué hacer. Si te atascas, ve directo a
[Solución de problemas](#solución-de-problemas).

### Qué vas a conseguir y qué necesitas

Al terminar, tu televisor con Android TV o Google TV tendrá una pantalla de inicio nueva, inspirada en
la navegación de PlayStation 4: una fila de aplicaciones que se recorre con el control remoto, un menú
rápido con ajustes comunes, y un fondo y nombre de perfil personalizados por ti.

Necesitas:

- **Un televisor o caja con Android TV o Google TV**, con Android 8.0 ("API 26" u "Oreo") o una versión
  posterior. Casi cualquier Smart TV reciente de Sony, TCL, Hisense, Philips, Xiaomi o similar con
  Android TV/Google TV sirve. Si tu televisor es Samsung (Tizen), LG (webOS), o es un Roku, Vizio o Fire
  TV, **este proyecto no es compatible**: ve a [Preguntas frecuentes](#preguntas-frecuentes).
- **Un ordenador con Windows, macOS o Linux**, en la misma red Wi-Fi o cable que el televisor.
- **Unos 15-20 minutos** la primera vez (la mayor parte es esperar descargas automáticas).
- **El mando a distancia del televisor** a mano, porque algunos pasos se confirman en su pantalla.
- **No necesitas** una cuenta de desarrollador, tarjeta de crédito, un ordenador potente ni saber
  programar. El instalador hace el resto.

### Lo que este proyecto nunca hace

- **No instala root** ni desbloquea el bootloader del televisor.
- **No desinstala nada de fábrica.** El paso de limpieza solo *desactiva* apps (`pm disable-user`), y
  `desinstalar.sh`/`desinstalar.ps1` las reactiva todas.
- **No toca** HDMI, el sintonizador de TV, el emparejamiento del mando o Bluetooth, el teclado en
  pantalla, Play Store ni Play Services.
- **No instala juegos ni software de PlayStation**, y no se conecta a PlayStation Network. Es solo una
  forma de presentar tus propias aplicaciones, no una consola ni su emulación.
- **No añade publicidad, analítica ni conexión a Internet propia**: el launcher no pide el permiso
  `INTERNET`.
- **Es completamente reversible**, con la copia de seguridad que el propio instalador guarda antes de
  tocar nada.

### Paso 1 — Comprueba que tu televisor es compatible

En el mando, ve a **Ajustes** (icono de engranaje) y busca, según el fabricante:

- **Google TV** (pestañas "Inicio", "Apps", "Buscar"): `Ajustes` → `Sistema` → `Información`.
- **Android TV clásico**: `Ajustes` → `Preferencias del dispositivo` → `Información`.

Anota lo que veas junto a **"Versión de Android TV"** o **"Versión de Android"**: necesitas **8.0 o
superior**. Si ves "6.0" o "7.0", este proyecto no es compatible con ese modelo. Si no encuentras un
menú "Información", tu televisor probablemente no usa Android TV (podría ser Tizen, webOS, Roku OS,
VIDAA u otro sistema propio): detente y lee las [preguntas frecuentes](#preguntas-frecuentes).

### Paso 2 — Activa las Opciones de desarrollador

Este paso permite que un ordenador instale aplicaciones en tu televisor por cable o por red, igual que
en un móvil Android. Es un paso estándar de Android, no algo propio de este proyecto.

1. En el menú **Información** del paso anterior, busca **"Compilación"** (o **"Build"**, o **"Número de
   compilación"**).
2. Selecciónala y pulsa **OK** (botón central del mando) **7 veces, rápido**. Cada pulsación muestra un
   aviso, por ejemplo: *"Ya eres desarrollador"* o *"Faltan 4 pasos para ser desarrollador"*.
3. Cuando aparezca **"Ya eres desarrollador"**, vuelve atrás con **Atrás**.
4. Ahora verás **"Opciones para desarrolladores"** en el menú **Sistema** (o **Preferencias del
   dispositivo**). Entra en ella.

### Paso 3 — Activa la depuración (ADB)

Dentro de **Opciones para desarrolladores**, activa las que tu televisor muestre (no todos los modelos
tienen las tres):

- **"Depuración USB"** (*USB debugging*): casi todos la tienen. Actívala.
- **"Depuración de red"** o **"Depuración ADB"** (*Network/ADB debugging*): algunos Android TV más
  antiguos la muestran. Actívala si existe.
- **"Depuración inalámbrica"** (*Wireless debugging*): la mayoría de los Google TV recientes (Chromecast
  con Google TV, Sony Bravia, TCL, Hisense de 2021 en adelante) usan esta. Actívala; el instalador te
  pedirá el código de emparejamiento que verás tú mismo en pantalla.

Activa todas las que te ofrezca el televisor; el instalador detecta cuál usar.

### Paso 4 — Conecta el televisor y el ordenador a la misma red

El ordenador debe estar en la **misma red Wi-Fi** que el televisor (o ambos por cable). No funciona si
el televisor está en la red del hogar y el ordenador en una red de invitados o en datos móviles.

Apunta la IP del televisor por si el instalador no la encuentra sola:
`Ajustes` → `Red e Internet` → tu red → **"Dirección IP"** (forma `192.168.1.50`).

### Paso 5 — Descarga el proyecto

**Windows:** ve a <https://github.com/JeisonCadenaC/cleanyourtv-PS4theme>, pulsa el botón verde
**"Code"** → **"Download ZIP"**, y extrae el ZIP en una carpeta que recuerdes (por ejemplo,
Descargas o Documentos).

**macOS o Linux**, en una Terminal:

```bash
cd ~
git clone https://github.com/JeisonCadenaC/cleanyourtv-PS4theme.git
```

(Sin `git`: descarga el ZIP igual que en Windows y descomprímelo con el gestor de archivos.)

### Paso 6 — Ejecuta el instalador automático

**Windows:** en la carpeta del proyecto, doble clic en **`instalar.bat`**. Si Windows muestra la
advertencia azul de "Windows protegió su PC" (SmartScreen), pulsa **"Más información"** →
**"Ejecutar de todas formas"**; esa advertencia aparece para cualquier script descargado de Internet,
no solo este, y `instalar.bat` solo abre `instalar.ps1`, que puedes leer tú mismo antes, es texto normal.

**macOS o Linux**, en una Terminal dentro de la carpeta del proyecto:

```bash
chmod +x instalar.sh
./instalar.sh
```

En los tres sistemas se abre una ventana de texto titulada **CleanYourTV PS4**, con un recordatorio de
los pasos 2 y 3, y pregunta:

```
¿Listo para empezar? (S/n)
```

Pulsa **Enter** (o `s` y Enter) para continuar.

### Qué hace el instalador, paso a paso en pantalla

El instalador avisa de cada fase con un título como `1/9  Preparando las herramientas de este
ordenador`. Esto es lo que verás y qué significa:

**1/9 — Preparando las herramientas de este ordenador.** Descarga Python, Java 17 y el SDK de Android si
hacen falta. No pide permisos de administrador en Windows; en macOS/Linux puede preguntarte antes de
usar `sudo` solo para instalar Python si tu sistema no lo trae (Java y el SDK de Android nunca necesitan
`sudo`). Tarda varios minutos la primera vez (~400-600 MB de descargas); después es instantáneo.

**2/9 — Descargando el proyecto.** Reutiliza la carpeta si ya la descargaste, o la descarga por ti.

**3/9 — Conectando con el televisor.** Busca televisores con ADB abierto en tu red y te deja elegir uno
de una lista, o te pide la IP del paso 4. **Mira la pantalla del televisor**: aparecerá

```
¿Permitir la depuración USB?
[ ] Permitir siempre desde este equipo
[Cancelar]  [Permitir]
```

Marca **"Permitir siempre desde este equipo"** y pulsa **Permitir**.

**4/9 — Copia de seguridad del televisor.** Guarda en tu ordenador (nunca en Internet) la lista de
aplicaciones, cuáles ya estaban desactivadas y los ajustes del sistema, en `backup/`. Esto es lo que
permite deshacer todo después.

**5/9 — Limpieza de apps de fábrica (opcional).** Si hay una lista probada para tu modelo exacto, te la
muestra y decides si aplicarla; **"0"** no limpia nada. Si aceptas, el instalador imprime qué va a
desactivar antes de tocar nada.

**6/9 — Compilando e instalando el launcher.** Compila el código en tu propio ordenador (nunca un
`.apk` ya hecho) y lo instala como inicio. Es el paso más largo: 5-10 minutos la primera vez. Pregunta
si quieres el "ayudante de botones del control" (recupera la función de botones como Ajustes o
YouTube); puedes aceptar la opción que te sugiere.

**7/9 — Permisos de las funciones PS4.** Pregunta si concedes permisos opcionales para notificaciones,
saber qué app usaste por última vez y ver tus dispositivos Bluetooth. Si dices que no, el inicio PS4
funciona igual, sin esos añadidos.

**8/9 — Tu tema PS4.** Pregunta tu nombre para el saludo, y te deja arrastrar una imagen de fondo y una
foto de perfil (o dejarlo vacío para el azul PS4 por defecto). Envía el tema y guarda una captura del
resultado en `capturas/`.

**9/9 — Cerrar el acceso ADB.** Pregunta si apagar la depuración ahora (recomendado): el televisor se
reinicia una vez y el acceso ADB queda cerrado. El launcher sigue funcionando igual con ADB cerrado;
solo hace falta reactivarlo para cambiar el tema o desinstalar más adelante.

### Después de instalar: cómo usar el inicio PS4

- **Flechas del mando:** mueven el foco entre aplicaciones y opciones del menú.
- **OK:** abre la aplicación o confirma la opción marcada.
- **Mantener pulsado OK** sobre una aplicación: abre su menú (moverla, favorita, desinstalarla).
- **Atrás:** vuelve a la pantalla anterior.
- **Botón de Ajustes del mando** o el engranaje de la barra superior: abre el **Menú rápido** (tema,
  sonido, Bluetooth, red, imagen, notificaciones, configuración).
- Pasados 10 segundos sin pulsar nada, la fila se oculta para mostrar el fondo; cualquier tecla la
  vuelve a mostrar.

### Cómo cambiar el tema más adelante

No hace falta repetir todo el proceso para cambiar el fondo, el nombre o el color:

1. Reactiva la depuración en el televisor (Paso 3) si la habías cerrado.
2. Ejecuta:
   - Windows: `.\instalar.ps1 -TvIp 192.168.1.50 -SoloTema`
   - macOS/Linux: `./instalar.sh --tv 192.168.1.50 --solo-tema`
3. Responde solo las preguntas del paso 8 (nombre, fondo, foto); el resto no se repite.

También puedes cambiar el acento de color sin ordenador, directamente desde **Menú rápido → Tema**.

### Cómo deshacer todo (desinstalar)

1. Reactiva la depuración ADB en el televisor (Paso 3).
2. Ejecuta `desinstalar.ps1` (doble clic, o desde PowerShell: `.\desinstalar.ps1`) en Windows, o
   `./desinstalar.sh` en macOS/Linux.
3. El script quita el tema, reactiva lo que la limpieza había desactivado y devuelve el inicio de
   fábrica. Al final pregunta si también quieres desinstalar el launcher PS4.

### Solución de problemas

**"No hay acceso ADB en `<ip>`:5555"** — El puerto de depuración está cerrado. Repite el Paso 3; si tu
televisor solo tiene "Depuración inalámbrica", deja que el instalador te guíe para emparejarlo (te
pedirá la IP:puerto de vinculación y un código de 6 cifras que verás en el propio televisor).

**El instalador pide un "código de 6 dígitos" y no sé dónde está** — En el televisor:
`Opciones para desarrolladores` → `Depuración inalámbrica` → `Vincular dispositivo con código de
emparejamiento`. Copia la IP:puerto y el código tal cual los ves; cambian cada vez.

**"Este televisor tiene Android ... (SDK ...). El launcher necesita Android 8.0 (SDK 26) o superior"**
— Tu televisor es demasiado antiguo para el launcher en este proyecto.

**El televisor no aparece en "Encontrados"** — Introduce la IP manualmente (Paso 4 dice cómo verla).
Normal en routers que aíslan dispositivos entre sí o en redes de invitados.

**"pm install failed" o "INSTALL_FAILED_OLDER_SDK"** — El televisor no alcanza el mínimo de Android que
pide el launcher; el mensaje indica ambos números.

**Un botón del mando no hace nada después de instalar** — Depende del "ayudante de botones" del paso 6.
Repite la instalación y actívalo si dijiste que no la primera vez.

**El instalador se queda esperando tras reiniciar el televisor** — Normal, algunos tardan más de 2
minutos en arrancar. Si avisa que no pudo confirmar el puerto cerrado, revisa manualmente en
`Opciones para desarrolladores` que toda la depuración esté apagada.

**Quiero cancelar a mitad de la instalación** — Puedes cerrar la ventana en cualquier momento; el
televisor queda como estaba hasta ese punto, y la copia de seguridad ya hecha no se pierde.

### Preguntas frecuentes

**¿Esto convierte mi televisor en una PlayStation 4?** No. Solo cambia la pantalla de inicio para que se
vea y navegue de forma similar a PS4. No instala juegos, no emula una consola y no se conecta a
PlayStation Network. PlayStation, PS4 y Sony son marcas de Sony Interactive Entertainment; este proyecto
es independiente y no tiene relación con esa compañía.

**Mi televisor es Samsung / LG / Roku / Vizio / Fire TV, ¿puedo usar esto?** No: Samsung usa Tizen, LG
usa webOS, y Roku, Vizio y Fire TV tienen sus propios sistemas, ninguno Android TV. Para reducir el
rastreo de publicidad en esos televisores sin instalar nada, visita <https://cleanyour.tv/not-android>.

**¿Necesito dejar la depuración ADB siempre activa?** No; el instalador la cierra al final (paso 9/9).
El launcher funciona igual con la depuración apagada.

**¿Puedo perder mis aplicaciones o mi cuenta de Google?** No. El instalador no desinstala nada ni borra
cuentas; solo desactiva, de forma reversible, lo que tú apruebas en el paso opcional de limpieza.

**¿Cuesta dinero?** No. El proyecto es de código abierto, licencia MIT, gratuito.

**¿Puedo usarlo en varios televisores?** Sí; repite el Paso 6 en cada uno. Las herramientas descargadas
(Java, el SDK) se reutilizan, así que las siguientes veces es más rápido.

### Seguridad y privacidad

- El launcher no usa analítica, no tiene publicidad y no pide el permiso `INTERNET`.
- La depuración ADB, mientras está activa, permite que **cualquier equipo de tu misma red** controle el
  televisor; por eso el instalador la cierra al final.
- La copia de seguridad en `backup/` contiene datos propios de tu televisor y nunca se sube a Internet
  por este proyecto: queda solo en tu ordenador. No la compartas sin revisarla antes.
- Si compartes capturas del nuevo inicio, tapa antes el nombre de tu red Wi-Fi, notificaciones privadas
  y cualquier dato de cuenta visible.

## Instalación manual

### 1. Clonar este fork

```powershell
git clone https://github.com/JeisonCadenaC/cleanyourtv-PS4theme.git
cd cleanyourtv-PS4theme
```

### 2. Preparar las herramientas

Instala Android Studio o un JDK 17 y el SDK indicado arriba. Desde PowerShell, en la carpeta del repositorio,
crea el entorno Python que usan las herramientas ADB:

```powershell
python -m venv tools\venv
tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt
```

En el televisor, habilita las opciones de desarrollador y ADB, y acepta la autorización que aparece en pantalla.
El ordenador y el televisor deben poder comunicarse por la red local. Sustituye `192.168.1.50` en los ejemplos
por la dirección de tu televisor.

### 3. Compilar e instalar el launcher

```powershell
.\launcher\build-and-install.ps1 -TvHost 192.168.1.50
```

El script compila el APK desde el código de este repositorio, lo instala y comprueba el inicio predeterminado
con sus protecciones de Home. No descargues ni uses APK precompilados de terceros. Si el script informa que
no puede identificar con seguridad el launcher del televisor, detente y revisa el mensaje; no fuerces la
desactivación manualmente.

### 4. Activar el modo PS4 con un tema

Guarda este contenido como `config.json` dentro de una carpeta de tema:

```json
{
  "ps4_layout": true,
  "ps4_accent": "auto",
  "ps4_idle_seconds": 10,
  "ps4_sounds": true,
  "ps4_user_name": ""
}
```

Aplica la carpeta al televisor:

```powershell
.\tools\theme.ps1 -TvHost 192.168.1.50 C:\ruta\al\tema-ps4
```

La carpeta puede incluir `wallpaper.jpg`, `avatar.jpg` y sonidos en `sounds/move.ogg`, `sounds/ok.ogg` o
`sounds/back.ogg`. Usa únicamente imágenes y sonidos propios o con licencia que permita su redistribución.
Para quitar el tema del televisor sin borrarlo de tu ordenador:

```powershell
.\tools\theme.ps1 -TvHost 192.168.1.50 -Reset
```

En Linux o macOS, usa `launcher/build-and-install.sh` y `tools/theme.sh`; sus parámetros y protecciones son
equivalentes. Para instrucciones ampliadas, consulta la [guía del modo PS4](docs/ps4-theme.md).

## Conexión ADB

`tools/tv.py --probe` se comunica con el televisor por el puerto ADB 5555. Si la conexión se rechaza y el
dispositivo ofrece **Wireless debugging**, emparéjalo con ADB y luego habilita el puerto con `adb tcpip 5555`,
o sigue las instrucciones de [conexión y emparejamiento](docs/TROUBLESHOOTING.md#connecting). No abras ADB
en el router ni expongas el puerto a Internet.

## Cerrar ADB al terminar

<details>
<summary><b>Isn't ADB access a security hole?</b></summary>

Desactiva ADB cuando termines de instalar el launcher, aunque el launcher seguirá funcionando:

1. En el televisor, desactiva **USB debugging**, **Network/ADB debugging** y **Wireless debugging** si aparecen, y revoca las autorizaciones ADB.
2. Reinicia el televisor y confirma desde el ordenador que `python3 tools/adb-closed.py <tv ip>` informa que el puerto está cerrado. Para el puerto propio de Wireless debugging, compruébalo también inmediatamente después de desactivarlo y antes de reiniciar.

Consulta los [pasos ampliados de seguridad](docs/faq.md#isnt-adb-access-a-security-hole).

</details>

## Configuración

| Clave del tema | Valores | Efecto |
|---|---|---|
| `ps4_layout` | `true` / `false` | Activa el inicio PS4 o mantiene el diseño estándar. |
| `ps4_accent` | `"auto"` o un color hexadecimal como `"#1F5FC4"` | Elige el acento de la interfaz. |
| `ps4_idle_seconds` | Segundos; `0` desactiva el reposo | Tiempo sin interacción antes de ocultar temporalmente la fila. El valor predeterminado es 10. |
| `ps4_sounds` | `true` / `false` | Activa o silencia los sonidos de navegación del launcher. |
| `ps4_user_name` | Texto local o vacío | Etiqueta de perfil que se muestra en el televisor. Déjala vacía en temas compartidos. |

La selección de acento hecha desde el menú rápido se guarda en el televisor y puede prevalecer sobre el valor
del tema. El orden, las etiquetas y las aplicaciones ocultas usan además la configuración normal del launcher;
consulta [`config.xml`](launcher/app/src/main/res/values/config.xml) y
[`docs/launcher.md`](docs/launcher.md).

## Permisos y privacidad

El launcher no usa analítica ni requiere Internet. Algunas funciones son opcionales y necesitan accesos
especiales de Android:

| Acceso | Para qué sirve | Decisión |
|---|---|---|
| **Acceso a notificaciones** | Mostrar y borrar notificaciones, y consultar sesiones multimedia activas. | Concédelo desde Ajustes del televisor → Acceso a notificaciones solo si quieres estas funciones. |
| **Acceso de uso** | Identificar la aplicación usada recientemente para algunos indicadores del inicio. | Es opcional; actívalo desde Acceso especial de aplicaciones si lo necesitas. |
| **Bluetooth** | Mostrar dispositivos emparejados en el menú de sonido. | En Android 12 o posterior, concede `BLUETOOTH_CONNECT` solo para usar esa lista. |
| **Ubicación precisa** | Android la exige en algunas versiones para que el launcher pueda leer el nombre de la red Wi-Fi. | El instalador puede concederla por ADB; el launcher la usa para esa etiqueta de red. No obtiene coordenadas. |

El permiso de notificaciones permite que Android entregue al launcher el contenido notificado; revisa las
notificaciones que aparecen en tu pantalla antes de habilitarlo. Los datos de perfil, dispositivos, red y
reproducción del televisor no deben incluirse en capturas o temas públicos.

## Compilar y comprobar

Con JDK 17 y Android SDK Platform / Build Tools 34 instalados, desde `launcher\`:

```powershell
.\gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

El APK se genera localmente en `launcher\app\build\outputs\apk\release\app-release.apk`. El repositorio no
distribuye binarios. Una compilación correcta no sustituye la comprobación de foco, controles y teclas del
mando en el televisor real.

## Relación con CleanYourTV y créditos

Este proyecto es un fork de [Gathin23/CleanYourTV](https://github.com/Gathin23/CleanYourTV), creado por Gathin
y sus colaboradores. Se conservan el launcher base, su estructura Android, el motor de temas y las
herramientas existentes; la adaptación visual y funcional PS4 y esta documentación pertenecen a
[JeisonCadenaC](https://github.com/JeisonCadenaC). Gracias a los autores originales y a quienes contribuyeron
al proyecto base. Consulta también el [historial de este fork](https://github.com/JeisonCadenaC/cleanyourtv-PS4theme/commits/main).

- El código heredado conserva la [licencia MIT](LICENSE).
- La tipografía Outfit se distribuye bajo SIL Open Font License; consulta
  [`launcher/FONT-LICENSE-OFL.txt`](launcher/FONT-LICENSE-OFL.txt).
- Bebas Neue conserva su licencia SIL Open Font License en
  [`launcher/FONT-LICENSE-BEBAS-NEUE-OFL.txt`](launcher/FONT-LICENSE-BEBAS-NEUE-OFL.txt).
- **PlayStation, PS4 y Sony** son marcas de sus respectivos propietarios. Este proyecto es independiente,
  no está afiliado ni respaldado por Sony Interactive Entertainment y no incluye software de consola,
  servicios de PSN ni fondos oficiales de Sony.

## Aportaciones y problemas

Las aportaciones a este fork deben centrarse en Android TV y en el modo de inicio PS4, conservar los créditos
del proyecto base y respetar las licencias de recursos. Antes de publicar capturas, oculta redes Wi-Fi, nombres,
cuentas, números de serie, notificaciones privadas y cualquier dato del dispositivo. No subas APK, claves ADB
ni imágenes para las que no tengas permiso.

Para errores de esta adaptación, abre un
[issue en este fork](https://github.com/JeisonCadenaC/cleanyourtv-PS4theme/issues). Para errores del launcher
o las herramientas originales, consulta el [proyecto CleanYourTV](https://github.com/Gathin23/CleanYourTV).
