# Trampoline Pattern — Sample App (POC)

This is a minimal, working implementation of the Trampoline Pattern described
in the Android App Link Routing & Authentication Isolation approach doc. It
implements all three routing buckets (App Deep Link, Auth, Browser-Only) plus
the custom-scheme auth callback handoff.

**What this is:** a throwaway project to prove the pattern works before
integrating it into the production Staples app.
**What this is not:** production-ready code. Package name, domain, and Ping
config below are all placeholders you need to swap in.

---

## How to open and run it

1. Open this folder directly in Android Studio (`File > Open`, point at this
   directory — it's a standard Gradle project, no import wizard needed).
2. Let Gradle sync. Requires JDK 17 (Android Studio bundles this).
3. Run the `app` module on an emulator or device to see the manual test
   screen (`MainActivity`) with buttons for each bucket.

## What's already implemented

| File | Purpose |
|---|---|
| `PathClassifier.kt` | Pure routing logic — decides which of the 3 buckets a URL belongs to. No Android framework dependency beyond `Uri` parsing, so it's fully unit-testable. |
| `TrampolineActivity.kt` | The actual "Traffic Cop" — transparent activity that receives the intent, classifies it, and routes via **explicit** intents (the bounce-loop fix from the addendum). |
| `DeepLinkDestinationActivity.kt` | Dummy stand-in for a real product/category screen — just proves the App Deep Link bucket worked. |
| `MainActivity.kt` | Manual test harness — buttons to fire each bucket's URL without needing adb, real SMS, or real email, for quick local iteration. |
| `PathClassifierTest.kt` | Unit tests covering all 3 buckets + the specific bounce-loop and callback-misconfiguration edge cases called out in the approach doc. |

---

## Phase 1 — Run the unit tests (do this first, no device needed)

```
./gradlew test
```

This runs `PathClassifierTest`, including the two regression guards worth
noting specifically:
- Confirms an `https://.../callback` URL does **NOT** get treated as a valid
  auth callback (only the custom scheme does) — this is the exact
  misconfiguration the addendum warns would break the callback handoff.
- Confirms an unrecognized path classifies as `Unknown` rather than crashing
  or accidentally matching another bucket.

## Phase 2 — Emulator matrix (App Link resolution across API levels)

You'll need to do this part yourself in Android Studio (I can't launch
emulators from here). Create AVDs at API 23, 26, 29, 31, 33, 34 and for each:

```
adb shell am start -a android.intent.action.VIEW -d "https://www.staples.com/p/123"
adb shell am start -a android.intent.action.VIEW -d "https://www.staples.com/login"
adb shell am start -a android.intent.action.VIEW -d "https://www.staples.com/unsubscribe"
adb shell dumpsys activity activities   # confirm Trampoline isn't left in the back-stack
```

**This will not auto-open the app** until Step A below (assetlinks.json) is
done — until then Android will show its normal disambiguation chooser. That's
expected, not a bug in this code.

---

## What you still need to do (I can't do these — need real access/hardware)

### A. Publish `assetlinks.json` (domain access required)
App Links won't verify without this. You (or whoever owns web infra) need to
publish a Digital Asset Links file at:

```
https://www.staples.com/.well-known/assetlinks.json
```

containing your app's package name (`com.staples.trampolinepoc` for this POC)
and the SHA-256 signing certificate fingerprint of the APK you're testing
with. Google has a verification tool here once it's published:
https://developers.google.com/digital-asset-links/tools/generator

Then replace every `www.staples.com` in this project
(`AndroidManifest.xml` and `MainActivity.kt`) with the real domain.

### B. Stand up a test Ping client (IDM access required)
To test the real `/login` → `/callback` round trip (not just the POC's fake
callback button), IDM needs to register a test client in Ping with:
- Redirect URI: `com.staples.trampolinepoc://callback` (custom scheme, per the
  addendum — **not** an `https://...` redirect URI)

Once that exists, swap the `/login` test button's target for a real Ping
authorization URL, and Ping's redirect will land on the custom scheme, which
`TrampolineActivity.handleAuthCallback()` already listens for.

### C. Real-device pass (physical hardware required)
Per the test plan already discussed — this is the part emulators can't
substitute for:

- **1 stock Pixel** — baseline Chrome, no OEM skin
- **1 Samsung device** — Samsung Internet as default browser
- **1 Xiaomi or OnePlus** — aggressive background-kill behavior

On each, with real triggers (not adb):
1. Send yourself a real SMS with the `/login` link → confirm CCT opens →
   complete real Ping login → confirm `/callback` hands back to the app.
2. Send yourself a real marketing email (via the actual email app, e.g. Gmail)
   with an `/unsubscribe` link → confirm it opens in the regular browser and
   does not bounce back into the app.
3. Force-kill the app in the background, then repeat step 1 — does the
   callback still resolve on cold start?
4. Change the device's default browser away from Chrome and repeat step 1 —
   confirm the CCT-availability fallback in `resolveCustomTabsPackage()`
   degrades gracefully.

### D. Telemetry (once integrated into the real app)
Instrument auth completion rate, bounce-loop detection (Trampoline launched
twice in a short window for one session), and deep-link open-in-app rate by
device manufacturer, before full rollout.

---

## Once this is proven out

Port `PathClassifier.kt` and `TrampolineActivity.kt` into the production app,
replacing the dummy destinations with real screens and real Ping token
handling in `handleAuthCallback()`. At that point it's a known-working
pattern being integrated, not an experiment.
