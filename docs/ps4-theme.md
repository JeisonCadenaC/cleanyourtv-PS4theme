# CleanYourTV PS4-inspired theme

This public fork adds an optional, PS4-inspired home-screen layout to the CleanYourTV launcher. It is an
Android TV launcher mode, not PlayStation software, and it does not run PlayStation games.

## What this fork adds

- A full-screen, D-pad-navigable home with a focused app tile, app actions, and a favorites row.
- A quick menu for profile, theme accents, sound, network, picture, notifications, and power actions.
- An app library, a notifications screen, and a now-playing card when Android exposes a media session.
- Optional idle dimming, accent colors, a profile avatar, and generated navigation sounds.
- A launcher notification listener and optional Usage Access and Bluetooth access for the related UI.

The mode is opt-in. Existing themes and the standard CleanYourTV layout continue to work unless the active
theme sets `ps4_layout` to `true`. The fork's launcher configuration also enables the Settings key panel and
maps `KEYCODE_BUTTON_3` to YouTube TV; remote key codes vary by TV and remote.

## Enable the layout

Build and install this fork's launcher on your Android TV or Google TV, following the original project's
safety checks and Home guard. On Windows, from the repository root:

```powershell
.\launcher\build-and-install.ps1 -TvHost 192.168.1.50
```

Create a theme folder with this `config.json` (or add these keys to an existing theme):

```json
{
  "ps4_layout": true,
  "ps4_accent": "auto",
  "ps4_idle_seconds": 10,
  "ps4_sounds": true,
  "ps4_user_name": ""
}
```

Push it from PowerShell with:

```powershell
.\tools\theme.ps1 -TvHost 192.168.1.50 C:\path\to\your-theme
```

The theme can include a `wallpaper.jpg`, `avatar.jpg`, and `sounds/move.ogg`, `sounds/ok.ogg`, or
`sounds/back.ogg`. Use images and sounds that you have permission to redistribute. Leave the profile name
blank or set it locally; do not publish personal names in a theme.

`ps4_accent` accepts `auto` or a color such as `#1F5FC4`. `ps4_idle_seconds` controls when the apps fade
away (use `0` to disable idle dimming), and `ps4_sounds` turns navigation sounds on or off. Quick Menu
choices are stored on the TV and can override the theme accent. Notification access, Usage Access, and
Bluetooth permissions are optional: enable only the access for features you want, using Android's own
Settings screens. The launcher does not request Internet access.

## Build and tests

Use JDK 17 and Android SDK platform/build-tools 34. From `launcher\` in PowerShell:

```powershell
.\gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

This repository does not include a prebuilt APK. A successful build is not a substitute for checking
D-pad navigation and remote behavior on the target TV.

## Credits and licensing

- **CleanYourTV** — this work is a fork of [Gathin23/CleanYourTV](https://github.com/Gathin23/CleanYourTV),
  created by Gathin and its contributors. The upstream project and its existing notices remain credited;
  see [`../LICENSE`](../LICENSE).
- **PS4-inspired adaptation** — implementation and fork documentation by
  [JeisonCadenaC](https://github.com/JeisonCadenaC).
- **Bebas Neue** — font by Dharma Type, distributed under the SIL Open Font License 1.1; see
  [`../launcher/FONT-LICENSE-BEBAS-NEUE-OFL.txt`](../launcher/FONT-LICENSE-BEBAS-NEUE-OFL.txt).
- **Sony, PlayStation, and PS4** are trademarks of Sony Interactive Entertainment. This is an independent,
  unofficial project and is not affiliated with or endorsed by Sony. No Sony UI screenshots, logos, or
  wallpapers are included as project assets.

Keep the upstream license and third-party font license with any redistribution. Review each asset's license
before adding it to this repository.
