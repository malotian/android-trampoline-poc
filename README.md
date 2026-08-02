# Trampoline Router — POC App

> **"Traffic Cop" for Staples deep links.**  
> A single transparent entry point that classifies every incoming `staples.com` URL and routes it to the right destination — without browser bounce loops, back-stack pollution, or manual per-screen handling.

---

## Routing Logic — The 3 Buckets

| # | Bucket | URL Path Prefixes | Destination |
| :-: | :--- | :--- | :--- |
| 🛒 | **App-Deep-Link** | `/product/`, `/p/`, `/c/`, `/deals/`, `/s/` | Native `DeepLinkDestinationActivity` |
| 🔐 | **App-Overlay-Browser** | `/idm/api/*`, `/login`, `/signin`, `/sign-in`, `/auth/` | Chrome Custom Tab (overlay, stays in-app) |
| 📋 | **System-Browser** | `/lp/*`, `/legal/`, `/unsubscribe`, `/terms`, `/privacy-policy` | Default browser (explicit package — no bounce loop) |
| 🔁 | **AuthCallback** | `com.staples.trampolinepoc://callback` | Handled in-app (OAuth return) |
| ❓ | **Unknown** | anything else | Falls back to default browser |

---

## Routing Flow Diagram

> Full diagram source: [`docs/routing-flow.puml`](docs/routing-flow.puml)  
> Render at [planttext.com](https://www.planttext.com) or with any PlantUML plugin.

```
 User taps link in Gmail / SMS
          │
          ▼
  Android OS — Intent Router
  (checks App Link domain verification)
          │
          ▼
 ┌─────────────────────────────┐
 │    TrampolineActivity       │  ← invisible (Theme.Transparent)
 │       "Traffic Cop"         │    no UI, no back-stack entry
 │                             │
 │  PathClassifier.classify()  │
 │  checks uri.path against    │
 │  RoutingConfig prefix lists │
 └──────────┬──────────────────┘
            │
     RouteBucket result
            │
  ┌─────────┼─────────┐
  │         │         │
  ▼         ▼         ▼
Native  Custom Tab  System
Screen   (overlay)  Browser
```

---

## How to Build & Run

### Step 1 — Unit tests (no device needed)
```bash
./gradlew test
```
Verifies all routing classification logic and browser resolution rules via Robolectric. **Run this first** — no emulator required.

### Step 2 — Install on device / emulator
Run the `app` module from Android Studio, or via command line:
```bash
./gradlew installDebug
```

---

## Fresh Install Setup (Required — Android 12+)

Android 12+ requires **Digital Asset Links verification** before the OS automatically routes `http(s)://staples.com` links to this app. Since this is a POC without a server-side asset file, run this command **once after every fresh install** to force-enable the links:

```bash
adb shell pm set-app-links-user-selection \
  --user 0 \
  --package com.staples.trampolinepoc \
  true \
  staples.com www.staples.com
```

> **Why is this needed?** On Android 12+, `autoVerify="true"` in the manifest fires a domain verification request at install time. Without a valid `/.well-known/assetlinks.json` served at `staples.com`, verification fails silently and Android will show a chooser dialog instead of auto-routing to the app. The `adb` command manually overrides this for local development. See [Digital Asset Links](#digital-asset-links-production-path) to eliminate this step for production.

### Confirm it worked
```bash
adb shell pm get-app-links --user 0 com.staples.trampolinepoc
```
Expected output:
```
staples.com:      verified
www.staples.com:  verified
```

---

## Testing the Buckets

### Dashboard (internal — no adb required)
Launch the app. Tap any button to send a real `ACTION_VIEW` intent through `TrampolineActivity` and observe the routing in action:

| Button | Tests | Expected Result |
| :--- | :--- | :--- |
| 🛒 Product Page → Native Screen | App-Deep-Link bucket | Opens `DeepLinkDestinationActivity` showing the URL path |
| 🔐 Auth / Login → Custom Tab Overlay | Auth bucket | Opens SDC login in a Chrome Custom Tab (slides up over app) |
| 📋 Promo / Legal → System Browser | System-Browser bucket | Opens Easy Rewards page in the default browser |

### External Link Testing (Gmail / SMS / Browser)
After running the `adb` setup command, paste any of these into Gmail or SMS and tap the link:

| Bucket | Test URL |
| :--- | :--- |
| 🪑 App-Deep-Link | `https://www.staples.com/product/ergonomic-chair` |
| 🔐 App-Overlay-Browser | `https://www.staples.com/idm/api/identityProxy/sdc/login` |
| 📋 System-Browser | `https://www.staples.com/lp/easyrewardsoverview` |

---

## Digital Asset Links — Production Path

To eliminate the `adb` command entirely, host a `/.well-known/assetlinks.json` on `staples.com`. Android reads it at install time and verifies automatically — no manual step ever needed again.

### Step 1 — Get your app's SHA-256 certificate fingerprint

**Debug keystore** (development / CI):
```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

**Release keystore** (production):
```bash
keytool -list -v \
  -keystore /path/to/your/release.keystore \
  -alias your-key-alias \
  -storepass YOUR_STORE_PASS
```

Copy the `SHA256:` line from the output:
```
AB:CD:EF:12:34:56:78:90:...
```

### Step 2 — Host `assetlinks.json`

Publish this file at **both**:
- `https://staples.com/.well-known/assetlinks.json`
- `https://www.staples.com/.well-known/assetlinks.json`

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.staples.trampolinepoc",
    "sha256_cert_fingerprints": [
      "AB:CD:EF:12:34:56:78:90:..."
    ]
  }
}]
```

> **Tips:**
> - Serve with `Content-Type: application/json` — no redirect, no auth.
> - Include **both** debug and release fingerprints in the array for each environment.

### Step 3 — Validate the hosted file

Use Google's Digital Asset Links API to confirm the file is reachable and correctly formatted:
```
https://digitalassetlinks.googleapis.com/v1/statements:list
  ?source.web.site=https://staples.com
  &relation=delegate_permission/common.handle_all_urls
```

### Step 4 — Verify on device (clean install)

Uninstall the app, reinstall (to trigger fresh domain verification), then:
```bash
adb shell pm get-app-links --user 0 com.staples.trampolinepoc
```
Both domains should show `verified`. No `adb` override command needed.

---

## Technical Notes

### TrampolineActivity — The "Traffic Cop"
| Property | How it's achieved |
| :--- | :--- |
| **Invisible** | `Theme.Transparent` — zero screen flicker |
| **No back-stack pollution** | `noHistory="true"` + `excludeFromRecents="true"` |
| **No bounce loop** | `BrowserResolver` pins the browser package on the intent (`intent.setPackage(pkg)`) — Android skips App Link verification for explicit package intents |
| **Single entry point** | `launchMode="singleTask"` — one live instance at a time |

### Custom Tab Fallback Chain
1. **Default browser** supports Custom Tabs → use it
2. **Any other installed browser** supports Custom Tabs → use it  
3. **No CCT support anywhere** → fall back to a plain explicit browser intent

### Key Files
| File | Role |
| :--- | :--- |
| `PathClassifier.kt` | Pure classification logic — no Android dependencies, fully unit-tested |
| `RoutingConfig` (in `PathClassifier.kt`) | Data class holding all URL prefix lists — swap out at runtime or from remote config |
| `BrowserResolver.kt` | Finds the correct browser package, handles the CCT fallback chain |
| `TrampolineActivity.kt` | Wires the above together; the only entry point |

---

## Porting to Production

Copy these three files into the production Staples app:

| File | What to update |
| :--- | :--- |
| `PathClassifier.kt` | Update `DEFAULT_CONFIG` prefix lists to match the production URL taxonomy |
| `BrowserResolver.kt` | No changes needed — fully generic |
| `TrampolineActivity.kt` | Replace `routeToAppDeepLink()` stub with real PDP / category screen routing; implement `handleAuthCallback()` with actual OAuth token extraction |
