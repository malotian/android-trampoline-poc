# Trampoline Pattern — POC App

A minimal implementation of the **Trampoline Pattern** for the Staples Android app. Acts as a "Traffic Cop" — a single, transparent entry point that classifies every incoming `staples.com` URL and routes it to the correct destination, preventing browser bounce loops.

---

## Routing Logic — The 3 Buckets

| Bucket | URL Path Prefixes | Destination |
| :--- | :--- | :--- |
| **App-Deep-Link** | `/product/`, `/p/`, `/c/`, `/deals/`, `/s/` | Native `DeepLinkDestinationActivity` |
| **App-Overlay-Browser** | `/idm/api/*`, `/login`, `/signin`, `/sign-in`, `/auth/` | Chrome Custom Tab (overlay) |
| **System-Browser** | `/lp/*`, `/legal/`, `/unsubscribe`, `/terms`, `/privacy-policy` | Default browser (explicit package, no bounce loop) |
| **AuthCallback** | `com.staples.trampolinepoc://callback` | Handled in-app (OAuth return) |
| **Unknown** | anything else | Falls back to default browser |

---

## How to Build & Run

### Step 1 — Unit tests (no device needed)
```bash
./gradlew test
```
Verifies all routing classification logic and browser resolution rules via Robolectric.

### Step 2 — Install on device / emulator
Run the `app` module from Android Studio, or:
```bash
./gradlew installDebug
```

---

## Fresh Install Setup (Required — Android 12+)

Android 12+ requires **Digital Asset Links verification** before the OS automatically routes `http(s)://staples.com` links to the app. Since this is a POC with no server-side asset file, run this command once after every fresh install to force-enable the links:

```bash
adb shell pm set-app-links-user-selection \
  --user 0 \
  --package com.staples.trampolinepoc \
  true \
  staples.com www.staples.com
```

> **Why this is needed:** On Android 12+, `autoVerify="true"` in the manifest triggers a domain verification request at install time. Without a valid `/.well-known/assetlinks.json` on the domain, verification fails and the OS will not auto-route links. The `adb` command manually overrides this for development. See the [Digital Asset Links](#digital-asset-links-production-path) section below to eliminate this step permanently.

### Confirm it worked
```bash
adb shell pm get-app-links --user 0 com.staples.trampolinepoc
```
You should see `staples.com: verified` and `www.staples.com: verified` in the output.

---

## Testing the Buckets

### Dashboard (internal — no adb needed)
Launch the app. Use the 3 buttons to fire each bucket through `TrampolineActivity` directly:
- **App-Deep-Link** → shows the native `DeepLinkDestinationActivity` with the path
- **App-Overlay-Browser** → opens SDC login in a Chrome Custom Tab
- **System-Browser** → opens Easy Rewards page in the default browser

### External Link Testing (Gmail / SMS / Browser)
Paste these URLs into Gmail or an SMS on the test device. Tap the link — the app should intercept and route it.

| Bucket | URL |
| :--- | :--- |
| App-Deep-Link | `https://www.staples.com/product/ergonomic-chair` |
| App-Overlay-Browser | `https://www.staples.com/idm/api/identityProxy/sdc/login` |
| System-Browser | `https://www.staples.com/lp/easyrewardsoverview` |

> Use `https://` — these match the real Staples link format and the manifest registers both `http` and `https`.

---

## Digital Asset Links — Production Path

To eliminate the `adb` command entirely, host a `/.well-known/assetlinks.json` file on `staples.com`. The OS reads it at install time and verifies automatically — no manual step ever again.

### Step 1 — Get your app's SHA-256 fingerprint

**Debug builds** (for development):
```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

**Release builds** (for production):
```bash
keytool -list -v \
  -keystore /path/to/your/release.keystore \
  -alias your-key-alias \
  -storepass YOUR_STORE_PASS
```

Copy the `SHA256:` fingerprint from the output — it looks like:
```
AB:CD:EF:12:34:56:78:90:...
```

### Step 2 — Create `assetlinks.json`

Host this file at `https://staples.com/.well-known/assetlinks.json` and `https://www.staples.com/.well-known/assetlinks.json`:

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

> Replace the fingerprint with the value from Step 1. For production, include both the debug and release fingerprints in the array.

### Step 3 — Verify domain association

The file must be served with `Content-Type: application/json` and **no redirects** (it must be accessible at exactly `https://staples.com/.well-known/assetlinks.json`).

Use Google's Statement List Generator & Tester to validate:
```
https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://staples.com&relation=delegate_permission/common.handle_all_urls
```

### Step 4 — Verify on device

Reinstall the app (clean install to trigger domain verification) then:
```bash
adb shell pm get-app-links --user 0 com.staples.trampolinepoc
```
Both `staples.com` and `www.staples.com` should show `verified`.

---

## Technical Notes

### TrampolineActivity — The "Traffic Cop"
- **Invisible:** Uses `Theme.Transparent` — no screen flicker when routing
- **No back-stack pollution:** `noHistory="true"` + `excludeFromRecents="true"`
- **Loop prevention:** `BrowserResolver` pins the explicit browser package name on the intent (`intent.setPackage(browserPackage)`), bypassing App Link verification for that launch

### Custom Tab Fallback Chain
1. Default browser, if it supports Custom Tabs → use it
2. Any other installed browser that supports Custom Tabs → use it
3. No CCT support anywhere → fall back to explicit plain browser intent

---

## Porting to Production

Copy these three files into the production app and wire them up:

| File | What to replace |
| :--- | :--- |
| `PathClassifier.kt` | Update `DEFAULT_CONFIG` prefixes to match production URL patterns |
| `BrowserResolver.kt` | No changes needed — generic |
| `TrampolineActivity.kt` | Replace `routeToAppDeepLink` stub with real PDP/destination routing; implement `handleAuthCallback` with real OAuth token handling |
