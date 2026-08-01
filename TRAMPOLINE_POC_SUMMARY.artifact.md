# Trampoline POC — Architectural Summary

This project implements the **Trampoline Pattern** to handle App Link routing and Authentication isolation for the Staples Android app.

## 1. Core Routing Logic (The 3 Buckets)

The app classifies incoming URLs into three distinct "buckets" using the `PathClassifier`.

| Bucket Name | Logic Path | Resulting Behavior |
| :--- | :--- | :--- |
| **App-Deep-Link** | `/product/*`, `/p/*` | Routes directly to a **Native Activity**. |
| **App-Overlay-Browser**| `/idm/api/*` | Opens the URL in a **Chrome Custom Tab** (Overlay). |
| **System-Browser** | `/lp/*`, `/unsubscribe` | Launches the user's **Default Browser** explicitly. |

## 2. Technical Implementation Details

### The "Traffic Cop" (TrampolineActivity)
A UI-less, transparent activity that acts as the single entry point.
- **Invisible:** Uses `Theme.Transparent` to avoid screen flicker.
- **Hygiene:** Uses `noHistory="true"` and `excludeFromRecents="true"` to stay out of the back-stack.

### Browser Resolution & Loop Fix
To prevent the "bounce loop" (where a browser link triggers the app again), we use **Explicit Intents**:
- The `BrowserResolver` finds the user's preferred browser package (e.g., `com.android.chrome`).
- The intent is fired with `intent.setPackage(browserPackage)`.
- This tells Android to **skip** App Link verification for that one launch and go straight to the web.

### Custom Tab Fallback
- The app checks if the default browser supports Custom Tabs.
- If not, it looks for another installed browser that does.
- If none are found, it gracefully degrades to a plain system browser launch.

## 3. Testing Scenarios (Current POC)

You can test these 3 primary scenarios using the dashboard:

1.  **Product Deep Link:** `http://www.staples.com/product/ergonomic-chair`
    - *Expected:* Opens the native screen showing a 🪑 icon.
2.  **Auth (Overlay):** `http://www.staples.com/idm/api/identityProxy/sdc/login`
    - *Expected:* Opens in a Custom Tab (the App-Overlay-Browser).
3.  **Legal/Regulatory:** `http://www.staples.com/lp/easyrewardsoverview`
    - *Expected:* Opens in the external System-Browser.

## 4. Android 12+ Verification Note
On modern Android versions, `http` links require domain verification via an `assetlinks.json` file. For this POC, verification will fail since we don't own the Staples domain.
- **To Test:** Use the dashboard buttons (which bypass verification) OR manually enable the links in **Settings > Apps > Trampoline POC > Open by default**.
