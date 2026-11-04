# Tutorial completo: transforma tu Android TV con el inicio estilo PS4

Instalador personalizado por JeisonCadenaC.

Esta guía está escrita para cualquier persona, **sin necesidad de experiencia previa con programación,
ADB o Android**. Sigue los pasos en orden; cada uno dice exactamente qué vas a ver en pantalla y qué
hacer. Si te atascas en cualquier punto, ve directamente a [Solución de problemas](#solución-de-problemas).

> Si prefieres la explicación corta y técnica, está en el [README](../../README.md) y en la
> [guía del modo PS4](../ps4-theme.md). Este documento es la versión larga, pensada para la primera vez.

## Índice

1. [Qué vas a conseguir y qué necesitas](#1-qué-vas-a-conseguir-y-qué-necesitas)
2. [Antes de empezar: lo que este proyecto nunca hace](#2-antes-de-empezar-lo-que-este-proyecto-nunca-hace)
3. [Paso 1 — Comprueba que tu televisor es compatible](#paso-1--comprueba-que-tu-televisor-es-compatible)
4. [Paso 2 — Activa las Opciones de desarrollador](#paso-2--activa-las-opciones-de-desarrollador)
5. [Paso 3 — Activa la depuración (ADB)](#paso-3--activa-la-depuración-adb)
6. [Paso 4 — Conecta el televisor y el ordenador a la misma red](#paso-4--conecta-el-televisor-y-el-ordenador-a-la-misma-red)
7. [Paso 5 — Descarga el proyecto](#paso-5--descarga-el-proyecto)
8. [Paso 6 — Ejecuta el instalador automático](#paso-6--ejecuta-el-instalador-automático)
9. [Qué hace el instalador, paso a paso en pantalla](#qué-hace-el-instalador-paso-a-paso-en-pantalla)
10. [Después de instalar: cómo usar el inicio PS4](#después-de-instalar-cómo-usar-el-inicio-ps4)
11. [Cómo cambiar el tema más adelante](#cómo-cambiar-el-tema-más-adelante)
12. [Cómo deshacer todo (desinstalar)](#cómo-deshacer-todo-desinstalar)
13. [Solución de problemas](#solución-de-problemas)
14. [Preguntas frecuentes](#preguntas-frecuentes)
15. [Seguridad y privacidad](#seguridad-y-privacidad)
16. [Dónde pedir ayuda](#dónde-pedir-ayuda)

## 1. Qué vas a conseguir y qué necesitas

Al terminar esta guía, tu televisor con Android TV o Google TV tendrá una pantalla de inicio nueva,
inspirada en la navegación de PlayStation 4: una fila de aplicaciones que se puede recorrer con el
control remoto, un menú rápido con ajustes comunes, y un fondo y nombre de perfil personalizados por ti.

Necesitas:

- **Un televisor o caja con Android TV o Google TV**, con Android 8.0 (también llamado "API 26" o
  "Oreo") o una versión posterior. Casi cualquier Smart TV de los últimos 6-7 años de Sony, TCL, Hisense,
  Philips, Xiaomi o similar que use Android TV/Google TV sirve. Si tu televisor es Samsung (usa Tizen),
  LG (usa webOS), o es un Roku, Vizio o Fire TV, **este proyecto no es compatible**: salta a
  [Preguntas frecuentes](#preguntas-frecuentes).
- **Un ordenador con Windows, macOS o Linux**, conectado a la misma red Wi-Fi o cable que el televisor.
- **Unos 15-20 minutos** la primera vez (la mayor parte es esperar descargas e instalaciones automáticas).
- **El mando a distancia del televisor** a mano, porque algunos pasos se confirman en la pantalla del TV.
- **No necesitas** una cuenta de desarrollador, una tarjeta de crédito, un ordenador potente ni
  conocimientos de programación. El instalador hace el resto.

## 2. Antes de empezar: lo que este proyecto nunca hace

Para que decidas con confianza, esto es lo que el instalador **nunca** hace, sin excepciones:

- **No instala root** ni desbloquea el bootloader del televisor.
- **No desinstala nada de fábrica.** Las aplicaciones que el paso de limpieza toca solo se *desactivan*
  (`pm disable-user`), y `desinstalar.sh` / `desinstalar.ps1` las reactiva todas.
- **No toca** el HDMI, el sintonizador de TV, el emparejamiento del mando o Bluetooth, el teclado en
  pantalla, Play Store ni Play Services: todo eso sigue funcionando igual.
- **No instala juegos, software de PlayStation ni se conecta a PlayStation Network.** El modo PS4 es solo
  una forma de presentar tus propias aplicaciones; no es una consola ni la emula.
- **No añade publicidad, analítica ni conexión a Internet propia del launcher.** El launcher que instala
  este proyecto no pide el permiso `INTERNET`.
- **Es completamente reversible.** Todo lo que el instalador cambia, `desinstalar.sh` / `desinstalar.ps1`
  lo puede deshacer con la copia de seguridad que el propio instalador guarda antes de tocar nada.

## Paso 1 — Comprueba que tu televisor es compatible

En el mando del televisor, ve a **Ajustes** (el icono de engranaje). Busca una de estas rutas, según el
fabricante:

- **Google TV** (interfaz con pestañas "Inicio", "Apps", "Buscar"): `Ajustes` → `Sistema` → `Información`.
- **Android TV clásico**: `Ajustes` → `Preferencias del dispositivo` → `Información`.

Anota lo que veas junto a **"Versión de Android TV"** o **"Versión de Android"**. Necesitas **8.0 o
superior**. Si el televisor solo muestra un número como "6.0" o "7.0", este proyecto no es compatible con
ese modelo. Si no encuentras un menú "Información" en absoluto, es probable que tu televisor **no** use
Android TV (podría ser Tizen, webOS, Roku OS, VIDAA o un sistema propio): detente y lee las
[preguntas frecuentes](#preguntas-frecuentes).

## Paso 2 — Activa las Opciones de desarrollador

Este paso convierte tu televisor en un dispositivo donde un ordenador puede instalar aplicaciones por
cable o por red, igual que harías con un móvil Android. Es un paso estándar de Android, no algo
inventado por este proyecto.

1. En el mismo menú **Información** del paso anterior, busca la línea **"Compilación"** (o **"Build"**,
   o **"Número de compilación"**).
2. Selecciónala y pulsa **OK** (el botón central del mando) **7 veces, rápido**. Cada pulsación muestra un
   mensaje contando hacia atrás, por ejemplo: *"Ya eres desarrollador"* o *"Faltan 4 pasos para ser
   desarrollador"*.
3. Cuando aparezca **"Ya eres desarrollador"**, vuelve atrás con el botón **Atrás** del mando.
4. Ahora verás una nueva opción en el menú **Sistema** (o **Preferencias del dispositivo**) llamada
   **"Opciones para desarrolladores"**. Entra en ella.

## Paso 3 — Activa la depuración (ADB)

Dentro de **Opciones para desarrolladores**, activa (pon en azul/verde) las siguientes opciones, según
las que tu televisor muestre — no todos los modelos tienen las tres:

- **"Depuración USB"** (*USB debugging*): casi todos los televisores la tienen. Actívala.
- **"Depuración de red"** o **"Depuración ADB"** (*Network debugging* / *ADB debugging*): algunos
  Android TV más antiguos la muestran. Actívala si existe.
- **"Depuración inalámbrica"** (*Wireless debugging*): la mayoría de los Google TV recientes (Chromecast
  con Google TV, Sony Bravia, TCL, Hisense de 2021 en adelante) usan esta en vez de las anteriores.
  Actívala; el instalador te pedirá el código de emparejamiento cuando lo necesite (lo verás tú mismo en
  pantalla, nadie más).

No te preocupes por elegir la "correcta": activa todas las que tu televisor te ofrezca. El instalador
detecta cuál usar. Déjate la pantalla de **Opciones para desarrolladores** abierta o recuerda cómo volver
a ella — más adelante te pedirá confirmar un mensaje de "¿Permitir la depuración USB?" en pantalla.

## Paso 4 — Conecta el televisor y el ordenador a la misma red

El ordenador que vas a usar para instalar debe estar en la **misma red Wi-Fi** que el televisor (o ambos
por cable, conectados al mismo router). No funciona si el televisor está en la red del hogar y el
ordenador en una red de invitados o en datos móviles.

Apunta la dirección IP del televisor por si el instalador no la encuentra sola:
`Ajustes` → `Red e Internet` → selecciona tu red → **"Dirección IP"**. Tendrá una forma como
`192.168.1.50`.

## Paso 5 — Descarga el proyecto

Elige tu sistema operativo:

### Windows

1. Ve a <https://github.com/JeisonCadenaC/cleanyourtv-PS4theme>.
2. Pulsa el botón verde **"Code"** → **"Download ZIP"**.
3. Extrae el ZIP descargado (botón derecho → "Extraer todo...") en una carpeta que recuerdes, por
   ejemplo tu carpeta de **Descargas** o **Documentos**. Evita escritorios con rutas muy largas o con
   caracteres especiales.

### macOS o Linux

Abre una Terminal y ejecuta:

```bash
cd ~
git clone https://github.com/JeisonCadenaC/cleanyourtv-PS4theme.git
```

(Si no tienes `git` instalado, descarga el ZIP igual que en Windows desde la página del proyecto y
descomprímelo con el gestor de archivos.)

> Si ya tienes experiencia y prefieres saltarte la descarga manual: el instalador también sabe descargar
> el proyecto él solo si lo ejecutas desde una carpeta vacía (ver paso siguiente).

## Paso 6 — Ejecuta el instalador automático

El instalador es **un solo archivo** por sistema operativo. Instala todo lo necesario (Python, Java,
el SDK de Android) sin que tengas que buscar nada por tu cuenta, encuentra el televisor, hace copia de
seguridad, instala el launcher y aplica tu tema.

### Windows

En la carpeta donde extrajiste el proyecto, haz **doble clic en `instalar.bat`**.

Si Windows muestra una advertencia azul de "Windows protegió su PC" (SmartScreen), pulsa
**"Más información"** y luego **"Ejecutar de todas formas"**. Esa advertencia aparece para cualquier
script descargado de Internet, no solo para este; `instalar.bat` solo abre `instalar.ps1`, que puedes
leer tú mismo antes de ejecutarlo, es texto normal.

### macOS o Linux

Abre una Terminal en la carpeta del proyecto y ejecuta:

```bash
cd cleanyourtv-PS4theme
chmod +x instalar.sh
./instalar.sh
```

### Lo que pasa a continuación es igual en los tres sistemas

Una ventana de texto se abre con el título **CleanYourTV PS4**, muestra un resumen de los pasos del
paso 2 y 3 de esta guía como recordatorio, y pregunta:

```
¿Listo para empezar? (S/n)
```

Pulsa **Enter** (o escribe `s` y Enter) para continuar.

## Qué hace el instalador, paso a paso en pantalla

El instalador avisa de cada fase con un título como `1/9  Preparando las herramientas de este ordenador`.
Esto es lo que verás y lo que significa cada uno:

**1/9 — Preparando las herramientas de este ordenador.** Descarga Python, Java 17 y el SDK de Android si
tu ordenador no los tiene ya. No pide permisos de administrador en Windows; en macOS/Linux puede
preguntarte antes de usar `sudo` solo para instalar Python si tu sistema no lo trae (Java y el SDK de
Android nunca necesitan `sudo`). Tarda varios minutos la primera vez por las descargas (~400-600 MB en
total); las siguientes veces es instantáneo porque ya está todo guardado.

**2/9 — Descargando el proyecto.** Si lo ejecutaste desde la carpeta ya descargada, la reutiliza. Si no,
la descarga por ti.

**3/9 — Conectando con el televisor.** Busca televisores con el puerto ADB abierto en tu red. Si lo
encuentra, te deja elegirlo de una lista numerada; si no, te pide la IP que apuntaste en el paso 4. En
este punto, **mira la pantalla del televisor**: aparecerá un cuadro de diálogo

```
¿Permitir la depuración USB?
El huella digital de la clave RSA de este equipo es: ...
[ ] Permitir siempre desde este equipo
[Cancelar]  [Permitir]
```

Marca la casilla **"Permitir siempre desde este equipo"** con el mando y pulsa **Permitir**. Si no actúas
a tiempo, el instalador reintenta solo durante medio minuto.

**4/9 — Copia de seguridad del televisor.** Guarda en tu ordenador (nunca en Internet) la lista de
aplicaciones, cuáles estaban ya desactivadas y los ajustes del sistema, en una carpeta `backup/` dentro
del proyecto. Esta copia es lo que permite deshacer todo después.

**5/9 — Limpieza de apps de fábrica (opcional).** Si tu modelo exacto tiene una lista probada por otra
persona de la comunidad, te la muestra y te deja decidir si aplicarla o no; puedes elegir **"0"** para no
limpiar nada y seguir solo con el launcher. Si decides aplicarla, el instalador imprime qué va a
desactivar antes de tocar nada, para que lo leas primero.

**6/9 — Compilando e instalando el launcher.** Descarga el código fuente, lo compila en tu propio
ordenador (no se usa ningún archivo `.apk` ya hecho) y lo instala como pantalla de inicio. Es el paso más
largo: 5-10 minutos la primera vez. Te preguntará si quieres el "ayudante de botones del control", que
simplemente hace que ciertos botones del mando (como el de Ajustes) vuelvan a funcionar con el nuevo
inicio; puedes aceptar la opción recomendada que te sugiere.

**7/9 — Permisos de las funciones PS4.** Pregunta si quieres conceder permisos opcionales para
notificaciones, saber qué app usaste por última vez, y ver tus dispositivos Bluetooth emparejados. Son
opcionales: si dices que no, el inicio PS4 funciona igual, solo sin esos añadidos.

**8/9 — Tu tema PS4.** Te pregunta tu nombre para el saludo, y te deja arrastrar una imagen de fondo y
una foto de perfil desde tu ordenador (o dejarlo en blanco para usar el azul PS4 por defecto). Al
terminar, envía el tema al televisor y guarda una captura de pantalla del resultado en una carpeta
`capturas/` para que la veas sin tener que mirar el televisor.

**9/9 — Cerrar el acceso ADB.** Te pregunta si quieres apagar la depuración ahora (recomendado): el
televisor se reinicia una vez y queda con el acceso ADB cerrado. El launcher sigue funcionando igual con
ADB cerrado; solo necesitas volver a activarlo si quieres cambiar el tema o desinstalar más adelante.

Al final verás un resumen con los controles básicos y la ruta del registro completo de la instalación.

## Después de instalar: cómo usar el inicio PS4

- **Flechas del mando:** mueven el foco entre aplicaciones y opciones del menú.
- **OK:** abre la aplicación o confirma la opción marcada.
- **Mantener pulsado OK** sobre una aplicación: abre su menú (moverla, marcarla como favorita,
  desinstalarla).
- **Atrás:** vuelve a la pantalla o panel anterior.
- **Botón de Ajustes del mando** (el engranaje, si tu mando lo tiene) o el icono de engranaje de la barra
  superior: abre el **Menú rápido**, con tema, sonido, Bluetooth, red, imagen, notificaciones y
  configuración del televisor.
- Pasados 10 segundos sin pulsar nada, la fila de aplicaciones se oculta para mostrar el fondo; cualquier
  tecla la vuelve a mostrar.

## Cómo cambiar el tema más adelante

No necesitas repetir todo el proceso para cambiar el fondo, el nombre o el color:

1. En el televisor, vuelve a activar la depuración (Paso 3 de esta guía) si la habías cerrado.
2. En tu ordenador, en la carpeta del proyecto, ejecuta:
   - Windows: `.\instalar.ps1 -TvIp 192.168.1.50 -SoloTema`
   - macOS/Linux: `./instalar.sh --tv 192.168.1.50 --solo-tema`
3. Responde solo las preguntas del paso 8 (nombre, fondo, foto); el resto de la instalación no se repite.

También puedes cambiar el acento de color sin ordenador: **Menú rápido → Tema**, directamente desde el
mando.

## Cómo deshacer todo (desinstalar)

Si quieres volver el televisor exactamente a como estaba:

1. Activa de nuevo la depuración ADB en el televisor (Paso 3).
2. Ejecuta:
   - Windows: doble clic en `desinstalar.ps1` (botón derecho → "Ejecutar con PowerShell"), o desde
     PowerShell: `.\desinstalar.ps1`
   - macOS/Linux: `./desinstalar.sh`
3. El script quita el tema, reactiva las aplicaciones que la limpieza había desactivado y devuelve el
   inicio de fábrica. Al final te pregunta si también quieres desinstalar el launcher PS4; si dices que
   sí y el inicio de fábrica ya quedó activo, lo quita.

## Solución de problemas

**"No hay acceso ADB en `<ip>`:5555"**
El puerto de depuración está cerrado. Repite el Paso 3: activa "Depuración USB"/"Depuración de red" o,
si tu televisor solo tiene "Depuración inalámbrica", deja que el instalador te guíe para emparejarlo
(te pedirá la IP:puerto de vinculación y un código de 6 cifras que verás en el propio televisor).

**El instalador pregunta por un "código de 6 dígitos" y no sé dónde está**
En el televisor: `Opciones para desarrolladores` → `Depuración inalámbrica` → `Vincular dispositivo con
código de emparejamiento`. Aparece una pantalla con una IP:puerto y un código de 6 cifras que cambia cada
vez; cópialos tal cual los ves, no se guardan de una vez a otra.

**"Este televisor tiene Android ... (SDK ...). El launcher necesita Android 8.0 (SDK 26) o superior"**
Tu televisor es demasiado antiguo para este proyecto. No hay forma de continuar con el launcher en ese
modelo (la limpieza de apps de fábrica, si existe una lista para tu modelo, sí podría aplicarse por
separado; pregunta en los issues del proyecto base).

**El televisor no aparece en la lista de "Encontrados"**
Introduce la IP manualmente cuando el instalador te la pida (Paso 4 de esta guía dice cómo verla). Esto
es normal en routers que aíslan los dispositivos entre sí o en redes de invitados.

**"pm install failed" o "INSTALL_FAILED_OLDER_SDK"**
El televisor no alcanza el mínimo de Android que pide el launcher. Revisa el mensaje completo: indica
qué versión tiene tu TV y cuál pide el launcher.

**Nada en pantalla cambia después de instalar, o un botón del mando no hace nada**
Algunos botones (como Ajustes o Inicio en ciertos mandos) dependen de un "ayudante de botones" que el
paso 6 preguntó si querías activar. Si dijiste que no, puedes repetir la instalación y activarlo; si
dijiste que sí y sigue sin funcionar, prueba a mantener pulsado el botón de encendido del televisor 5
segundos para forzar un reinicio, o revisa `docs/TROUBLESHOOTING.md` del proyecto base.

**El instalador se queda esperando después de reiniciar el televisor**
Es normal: algunos televisores tardan más de 2 minutos en arrancar. Si el aviso de "no pude confirmar que
el puerto esté cerrado" aparece, simplemente revisa manualmente en `Opciones para desarrolladores` que
toda la depuración esté apagada.

**Quiero cancelar a mitad de la instalación**
Puedes cerrar la ventana en cualquier momento. El televisor queda como estaba hasta ese punto: si ya
había hecho la copia de seguridad, nada se pierde, y puedes ejecutar `desinstalar.ps1` / `desinstalar.sh`
después si algo quedó a medias.

## Preguntas frecuentes

**¿Esto convierte mi televisor en una PlayStation 4?**
No. Solo cambia la pantalla de inicio para que se vea y se navegue de forma similar a la interfaz de
PS4. No instala juegos, no emula una consola y no se conecta a PlayStation Network. PlayStation, PS4 y
Sony son marcas de Sony Interactive Entertainment; este proyecto es independiente y no tiene relación
con esa compañía.

**Mi televisor es Samsung / LG / Roku / Vizio / Fire TV, ¿puedo usar esto?**
Samsung usa Tizen, LG usa webOS, y Roku, Vizio y Fire TV tienen sus propios sistemas: ninguno es Android
TV y este proyecto no se puede instalar en ellos. Si solo quieres reducir el rastreo de publicidad en
esos televisores sin instalar nada, visita <https://cleanyour.tv/not-android>, que lo hace con el mando
a distancia.

**¿Necesito dejar la depuración ADB siempre activa?**
No, y de hecho el propio instalador te ofrece cerrarla al final (paso 9/9). El launcher funciona igual
con la depuración apagada; solo necesitas volver a activarla para cambiar el tema o desinstalar.

**¿Puedo perder mis aplicaciones o mi cuenta de Google?**
No. El instalador no desinstala aplicaciones ni borra cuentas; solo desactiva (de forma reversible) las
que tú apruebas en el paso opcional de limpieza, y nunca toca Play Store, Play Services ni las apps de
streaming si no las apruebas explícitamente.

**¿Cuesta dinero?**
No. El proyecto es de código abierto bajo licencia MIT y gratuito.

**¿Puedo usarlo en varios televisores?**
Sí. Repite el Paso 6 en cada televisor; las herramientas descargadas (Java, el SDK) se reutilizan, así
que las siguientes veces es más rápido.

## Seguridad y privacidad

- El launcher instalado no usa analítica, no tiene publicidad y no pide el permiso `INTERNET`: no manda
  datos a ningún servidor.
- La depuración ADB, mientras está activa, permite que **cualquier equipo de tu misma red** controle el
  televisor. Por eso el instalador la cierra al final (puedes decir que no si vas a hacer más cambios
  pronto, pero cierra siempre cuando termines del todo).
- La copia de seguridad que se guarda en `backup/` contiene datos propios de tu televisor (nombre del
  modelo, lista de apps, ajustes). Esa carpeta nunca se sube a Internet por este proyecto: queda solo en
  tu ordenador. No la compartas en foros ni capturas de pantalla sin revisarla antes.
- Si compartes capturas de pantalla de tu televisor con el nuevo inicio, tapa o recorta antes el nombre
  de tu red Wi-Fi, notificaciones privadas y cualquier dato de cuenta visible.

## Dónde pedir ayuda

- Problemas con esta adaptación PS4 o con esta guía: abre un
  [issue en este fork](https://github.com/JeisonCadenaC/cleanyourtv-PS4theme/issues).
- Problemas con el launcher base, las herramientas (`tools/`) o la limpieza de apps de un modelo
  concreto: consulta el [proyecto CleanYourTV](https://github.com/Gathin23/CleanYourTV) y su
  [guía de solución de problemas](../TROUBLESHOOTING.md).
- Preguntas generales: [GitHub Discussions](https://github.com/Gathin23/CleanYourTV/discussions) del
  proyecto base.
