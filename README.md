# Empire Dashboard

Live UI showing your entire autonomous pipeline — runs on Android, Windows, and Linux from one codebase.

## Prerequisites

- JDK 17+ (`java -version`)
- Android SDK (for APK builds) — install via Android Studio
- Gradle 8.9 (wrapper included)

## Step 1 — Start the API server

In your OneDrive scripts folder:

```powershell
& 'C:\Users\LXGIXN\OneDrive\Copilot\Launch Assets - CEO Prompt Vault\empire-api-server.ps1'
```

Server runs at `http://localhost:8765`. The app auto-polls every 5 seconds.

## Step 2 — Run the desktop app instantly

```powershell
.\build.ps1 -Target run
```

## Step 3 — Build distributable packages

| Target | Command | Output |
|--------|---------|--------|
| Android APK | `.\build.ps1 -Target apk` | `composeApp\build\outputs\apk\release\*.apk` |
| Windows EXE | `.\build.ps1 -Target exe` | `composeApp\build\compose\binaries\main\exe\*.exe` |
| Linux .deb  | `.\build.sh deb` (on Linux/WSL) | `composeApp/build/compose/binaries/main/deb/*.deb` |

## App Screens

| Screen | What It Shows |
|--------|--------------|
| Dashboard | Live status, active niche with score bars, bundle info, recent runs |
| Niches | Full ranked niche list with all scoring dimensions |
| History | Pipeline run history + product bundle history |

## Architecture

```
composeApp/commonMain   → All UI + data (shared across platforms)
composeApp/androidMain  → MainActivity
composeApp/desktopMain  → main() window entry
empire-api-server.ps1   → Local HTTP API reading pipeline manifests
```

## Android on-device setup

For the APK to connect to your PC's API server, both devices must be on the same WiFi.
Pass the server IP when building a custom variant, or edit `App.kt`:

```kotlin
App(serverUrl = "http://192.168.1.X:8765")
```
