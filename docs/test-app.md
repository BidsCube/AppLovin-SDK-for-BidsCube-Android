# Test app — Banner QA without MAX keys

The sibling project `bidscube-testapp-android` validates Bidscube SDK integration. Build from the SDK root:

```bash
./gradlew :bidscube-testapp-android:installDebug
```

## Prerequisites

1. Open the SDK root in Android Studio (includes `:sdk`, `:applovin-adapter`, `:bidscube-testapp-android`).
2. Initialize SDK on the home screen (debug mode enabled automatically).
3. **No AppLovin MAX keys required** for banner verification flows below.

## Banner Verification QA (recommended)

Home → **Banner verification QA (no MAX keys)**

| Button | What it tests |
|--------|----------------|
| **Offline full path** | `BidscubeSDK.setQaAdmOverride()` → `getImageAdView()` — full HTML/WebView/imp trace, no network |
| **Offline document.write + wrapper** | Same path with document.write + wrapper ADM fixture |
| **Mock SSP** | HTTP fetch to local mock (`placement mock-banner`) |
| **Live SSP** | Production SSP (`placement 21980`) |
| **Adapter-equivalent** | Same as offline; documents equivalence with `BidscubeMediationAdapter.loadAdViewAd()` |

### On-screen checklist

Records callback order: `onAdLoading` → `onAdLoaded` → `onAdDisplayed`.

`onAdDisplayed` means container is ready — **not** MRC viewability.

### Logcat verification

```bash
adb logcat -s BannerAdTrace DeviceInfoProvider BannerVerificationQa
```

Expected for a successful banner with imp pixel:

- `DeviceInfoProvider request bundle=com.bidscube.testapp appId=5421`
- `BannerAdTrace phase=finalHtml hasImpPixel=true`
- `BannerAdTrace phase=network impRequest first=true`
- No `DUPLICATE imp request` for a single show

### What logcat does **not** prove

- SSP server credited the impression (`h=` match on backend)
- Creative viewability (only container metrics in trace)

## Mock SSP setup

1. Start mock server (auto HTTPS tunnel):

```bash
python3 scripts/mock_bidscube_ssp.py
```

2. Copy printed host into `bidscube-testapp-android/gradle.properties`:

```properties
bidcube.testSspAuthority=your-tunnel-host.trycloudflare.com
```

3. Rebuild and install:

```bash
./gradlew :bidscube-testapp-android:installDebug
```

4. Banner Verification QA → **Mock SSP**

Mock presets (via `preset` query param on server): `default`, `banner_imp`, `banner_docwrite`.

The mock logs `placementId`, `bundle`, `c`, `m` for each request.

## Unit tests (SDK)

```bash
./gradlew :sdk:testFullVideoDebugUnitTest
```

Covers banner HTML prep, ADM sanitizer, bundle URL param, `BannerAdTrace` imp dedup.

## MAX keys (optional)

**OpenMyGames MAX banner QA** requires AppLovin SDK key + MAX ad unit in `gradle.properties`:

```properties
applovin.sdk.key=YOUR_KEY
max.banner.ad.unit=YOUR_AD_UNIT
```

MAX UI is optional; SDK-direct and offline paths cover the same banner rendering code.

## Other QA screens

| Screen | Keys | Network |
|--------|------|---------|
| OpenMyGames QA (SDK direct) | No | Live SSP |
| Offline creatives QA | No | No (bypasses getImageAdView) |
| OpenMyGames MAX QA | Yes | Live SSP + MAX |
