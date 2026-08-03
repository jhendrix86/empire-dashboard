# Empire Dashboard

Live UI showing your entire autonomous pipeline — runs on Android, Windows, and Linux from one codebase.

## Prerequisites

- JDK 17+ (`java -version`)
- Android SDK (for APK builds) — install via Android Studio
- Gradle 8.9 (wrapper included)

## Step 1 — Start the API server

By default the pipeline calls Anthropic:

```powershell
$env:ANTHROPIC_API_KEY = "sk-ant-..."
gradle :server:run
```

To use OpenAI instead, set `LLM_PROVIDER` and an OpenAI key (defaults to `gpt-4o-mini`,
override with `OPENAI_MODEL`):

```powershell
$env:LLM_PROVIDER = "openai"
$env:OPENAI_API_KEY = "sk-..."
gradle :server:run
```

Server runs at `http://127.0.0.1:8765` by default. The app auto-polls every 5 seconds.

### Optional: cap LLM spend per run

To stop an autonomous run before it burns through your API budget (e.g. a runaway
Polish/Audit retry loop, or a Design stage deciding on an unusually large number of
output formats), set a per-run ceiling:

```powershell
$env:EMPIRE_MAX_RUN_COST_USD = "2.00"
```

Spend is estimated from request/response character counts against public per-model
rates (not exact billing). When a run crosses the cap, the current stage fails with a
clear "exceeded its $X.XX LLM budget" error instead of continuing to spend. Unset (the
default) means unlimited. Estimated spend for each run is written to that run's log
either way, whether it finished or errored.

### Optional: LAN access (for the Android app)

By default the server only listens on loopback, so nothing outside this machine can
reach it. To let the Android app connect over WiFi, opt in explicitly and set a
shared token — the server refuses to start in LAN mode without one:

```powershell
$env:EMPIRE_BIND_ALL = "true"
$env:EMPIRE_AUTH_TOKEN = "some-long-random-string"
gradle :server:run
```

Then point the app at both the server IP and the same token:

```kotlin
App(serverUrl = "http://192.168.1.X:8765", authToken = "some-long-random-string")
```

Read-only endpoints (status, niches, customers/leads/revenue lists) don't require
the token; mutating ones (`/run`, `/run/cancel`, adding a customer/lead, recording a
sale/refund) do, whenever `EMPIRE_BIND_ALL` is set.

A run in progress can be stopped from the Progress screen's "STOP RUN" button (or by
calling `POST /run/cancel` directly). Cancellation is cooperative -- it interrupts the
run at its next LLM call rather than mid-write, so the manifest is left consistent and
marked `cancelled`.

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
shared/commonMain       → DTOs shared between composeApp and server
server/                 → Ktor backend: the autonomous pipeline itself
```

## Android on-device setup

For the APK to connect to your PC's API server, both devices must be on the same WiFi,
and the server must be started with `EMPIRE_BIND_ALL=true` (see above). Pass the server
IP and token when building a custom variant, or edit `App.kt`:

```kotlin
App(serverUrl = "http://192.168.1.X:8765", authToken = "some-long-random-string")
```
