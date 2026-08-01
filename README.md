# Trampoline Pattern — Sample App (POC)

This is a minimal implementation of the **Trampoline Pattern** to handle App Link routing and Authentication isolation for the Staples Android app. It serves as a "Traffic Cop" to classify incoming URLs and route them to the correct destination while preventing common issues like browser loops.

---

## 1. Core Routing Logic (The 3 Buckets)

The app classifies incoming URLs into three distinct "buckets":

| Bucket Name | Logic Path | Resulting Behavior |
| :--- | :--- | :--- |
| **App-Deep-Link** | `/product/*`, `/p/*` | Routes directly to a **Native Activity**. |
| **App-Overlay-Browser**| `/idm/api/*` | Opens the URL in a **Chrome Custom Tab** (Overlay). |
| **System-Browser** | `/lp/*`, `/unsubscribe` | Launches the user's **Default Browser** explicitly. |

---

## 2. Technical Implementation Details

### The "Traffic Cop" (`TrampolineActivity`)
A UI-less, transparent activity that acts as the single entry point for all domain links.
- **Invisible:** Uses `Theme.Transparent` to avoid screen flicker.
- **Hygiene:** Uses `noHistory="true"` and `excludeFromRecents="true"` to stay out of the back-stack.

### Browser Resolution & Loop Fix
To prevent the "bounce loop" (where a browser link triggers the app again), we use **Explicit Intents**:
- The `BrowserResolver` finds the user's preferred browser package (e.g., `com.android.chrome`).
- The intent is fired with `intent.setPackage(browserPackage)`.
- This tells Android to **skip** App Link verification for that launch and go straight to the web.

### Custom Tab Fallback
- The app detects if the default browser supports Custom Tabs.
- If not, it falls back to any installed CCT-capable browser, or finally a plain system browser.

---

## 3. How to Run and Test

### Phase 1 — Run the unit tests (no device needed)
```bash
./gradlew test
```
This verifies the classification logic and browser resolution rules.

### Phase 2 — Manual Testing (Dashboard)
1. Run the `app` module on an emulator (Android 13 recommended).
2. Use the 3 buttons on the dashboard to test each bucket internally.
3. The **App-Deep-Link** button will show a native screen with a 🪑 icon.

### Phase 3 — External Link Testing (Gmail/SMS)
On Android 12+, `http` links require domain verification. Since this is a POC, run this command to **force-enable** Staples links for the app:

```bash
adb shell pm set-app-links-user-selection --user 0 --package com.staples.trampolinepoc true staples.com www.staples.com
```

**Test Links for Copy-Paste:**
- **Product:** http://www.staples.com/product/ergonomic-chair
- **Auth:** http://www.staples.com/idm/api/identityProxy/sdc/login
- **Regulatory:** http://www.staples.com/lp/easyrewardsoverview

---

## 4. Future Integration

Once proven, port `PathClassifier.kt`, `BrowserResolver.kt`, and `TrampolineActivity.kt` into the production app. Replace the dummy destinations with real product screens and handle the OAuth token in `handleAuthCallback`.
