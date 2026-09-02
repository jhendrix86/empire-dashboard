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

### Optional: get notified on a sale or a failed run

To hear about these without having the app open, point an outbound webhook at any
endpoint that accepts a JSON POST -- a Slack/Discord incoming webhook, ntfy.sh, etc.:

```powershell
$env:EMPIRE_NOTIFY_WEBHOOK_URL = "https://hooks.slack.com/services/..."
```

Each notification is `{"event": "...", "text": "..."}`, sent for a recorded sale
(`sale_recorded`) and a run that ends in error (`run_failed`) -- including a run that
hit the spend cap above. Unset (the default) sends nothing; a delivery failure is
logged but never breaks the sale/run that triggered it.

### Optional: pull real sales in from Stripe

Point the server at a Stripe account to have it pull in actual charges as sales,
instead of (or alongside) recording them by hand:

```powershell
$env:STRIPE_SECRET_KEY = "sk_live_..."
```

Every `STRIPE_SYNC_INTERVAL_MINUTES` (default `15`), the server fetches your most
recent Stripe charges and records any successful one it hasn't seen yet, firing the
same `sale_recorded` notification a manual sale does. You can also trigger a sync on
demand with the Revenue screen's "Sync Stripe" button, or `POST /revenue/sync-stripe`
directly. Unset (the default) does nothing -- no Stripe calls, no background loop.
Only the most recent ~100 charges are checked per sync, so keep the interval short
enough that you don't do more than ~100 transactions between syncs.

### Optional: create a draft Shopify listing for each product

Point the server at a Shopify store to have the Shipping stage create a listing for
every product it ships, so it's ready for you to review instead of manually re-typing
the title, description, and price:

```powershell
$env:SHOPIFY_STORE_DOMAIN = "your-store.myshopify.com"
$env:SHOPIFY_ACCESS_TOKEN = "shpat_..."
```

The listing is always created with `status: draft` -- it is never active/live, so
nothing is actually for sale until you review it in Shopify admin and publish it
yourself. The product's title comes from the niche, the description from the
blueprint's product outline, and the price is parsed from the blueprint's pricing
text. A link to the draft appears on the Dashboard's Product Bundle card once created.
Unset (the default) skips Shopify entirely -- no calls, no listing. Override the API
version with `SHOPIFY_API_VERSION` (defaults to `2025-01`).

### Optional: create a draft Etsy listing for each product

Point the server at an Etsy shop to have the Shipping stage create a draft listing
there too, the same way it does for Shopify above -- created via Etsy's own
`createDraftListing` endpoint, so it's a draft by definition and never active/live
until you publish it yourself.

Etsy's API is OAuth2-only and access tokens expire after about an hour, so setup is
a one-time manual step instead of just pasting in a static key:

1. Create an app under [your Etsy developer account](https://www.etsy.com/developers) to
   get an API key (keystring).
2. Complete Etsy's OAuth2 authorization-code flow once in a browser (see
   [Etsy's authentication docs](https://developers.etsy.com/documentation/essentials/authentication))
   to get an initial refresh token. This is the only manual step -- the server takes it
   from there.
3. Look up the numeric shop ID for your Etsy shop, and the taxonomy (category) ID you
   want listings filed under (Etsy's `getSellerTaxonomyNodes` endpoint, or note it while
   starting a listing by hand in the Etsy UI). There's no safe default for this since
   Etsy's category IDs are opaque and would silently misfile every listing if guessed
   wrong -- Etsy is skipped entirely if it's not set.

```powershell
$env:ETSY_API_KEY = "..."
$env:ETSY_SHOP_ID = "12345678"
$env:ETSY_TAXONOMY_ID = "..."
$env:ETSY_REFRESH_TOKEN = "..."   # only needed once, to bootstrap
```

After the first successful call, the server persists its own access/refresh token pair
to disk (`server/data/etsy-tokens.json` by default) and rotates it automatically as
Etsy requires -- `ETSY_REFRESH_TOKEN` is only ever read again if that file is missing.
Unset `ETSY_API_KEY`/`ETSY_SHOP_ID`/`ETSY_TAXONOMY_ID` (the default) skips Etsy entirely.
A link to the draft appears on the Dashboard's Product Bundle card once created.

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
