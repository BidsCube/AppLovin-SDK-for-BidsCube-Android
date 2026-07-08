# Bidscube SDK for Android

**SDK 1.2.9** · **AppLovin MAX adapter 1.2.9**

Android SDK and **AppLovin MAX** adapter for BidCube demand. Choose **one** adapter artifact for your video mode (see [Android AAR modes](#android-aar-modes)); each bundles the matching BidCube SDK runtime and the MAX adapter (`BidscubeMediationAdapter`).

**Repository:** https://github.com/BidsCube/AppLovin-SDK-for-BidsCube-Android

**Docs:** [CHANGELOG.md](CHANGELOG.md) · [RELEASE.md](RELEASE.md) · [Error codes](docs/errors.md) · [AppLovin MAX adapter](applovin-adapter/README.md) · [Custom rendering guide](docs/guide.md)

## Requirements

- **Android** minSdk **24+**
- **Gradle** with `google()` and `mavenCentral()`
- **AppLovin MAX:** `com.applovin:applovin-sdk` **13.0.x** (adapter built against 13.0.0; newer 13.x often works)
- **Adapter:** one of `com.bidscube:applovin-bidscube-max-adapter-*` at **1.2.9** (see [Android AAR modes](#android-aar-modes))
- **JDK 17** and **Android SDK** for building this repo
- **Permissions:** `INTERNET`, `ACCESS_NETWORK_STATE`

In MAX, set **Server Parameters** `app_id` and the MAX **Placement ID** per ad unit (see below).

---

## AppLovin MAX — installing the adapter

### Gradle

Add **AppLovin MAX** and **one** Bidscube adapter line for your chosen video mode. **Do not** add a separate core SDK dependency unless you need it explicitly — the adapter POM pulls the matching SDK artifact transitively.

**Kotlin (`build.gradle.kts`) — FullWithVideo example:**

```kotlin
dependencies {
    implementation("com.applovin:applovin-sdk:13.0.0@aar")
    implementation("com.bidscube:applovin-bidscube-max-adapter-full-video:1.2.9@aar")
}
```

**Groovy (`build.gradle`) — FullWithVideo example:**

```groovy
dependencies {
    implementation 'com.applovin:applovin-sdk:13.0.0@aar'
    implementation 'com.bidscube:applovin-bidscube-max-adapter-full-video:1.2.9@aar'
}
```

See [Android AAR modes](#android-aar-modes) for the other three adapter artifacts.

Sync Gradle. If Maven Central does not resolve the adapter, use a local AAR (see [applovin-adapter/README.md](applovin-adapter/README.md)) or build from this repository.

> **Migration from 1.0.x:** the single coordinate `com.bidscube:applovin-bidscube-adapter` is replaced by four mode-specific artifacts. Pick the mode that matches your video requirements.

---

## MAX Dashboard

Follow AppLovin’s guide for custom SDK networks:  
[Integrating custom SDK networks](https://support.axon.ai/en/max/mediated-network-guides/integrating-custom-sdk-networks/)

1. Open your app in the **AppLovin MAX Dashboard** (application id / package must match your app).
2. Go to **MAX → Mediation → Manage → Networks**.
3. Add a **Custom Network**:
   - **Network Type:** SDK  
   - **Name:** Bidscube (or your label)  
   - **Android Adapter Class Name:** `com.applovin.mediation.adapters.BidscubeMediationAdapter`
4. Go to **MAX → Mediation → Manage → Ad Units**, open each ad unit that should use Bidscube, enable **Bidscube**, and set placement fields as below.

---

## MAX parameters

| Field | Value |
|--------|--------|
| **Android Adapter Class Name** | `com.applovin.mediation.adapters.BidscubeMediationAdapter` |
| **`app_id` (Server Parameters)** | BidCube **application / init** identifier used when the adapter initializes the SDK |
| **Placement ID** | BidCube **placement** id for that MAX ad unit |
| **Server parameters (optional)** | **`request_authority`** or **`ssp_host`** — SSP host or `host:port` (normalized the same way as standalone `adRequestAuthority`) |

If `request_authority` or `ssp_host` is set, the adapter passes it into `SDKConfig.Builder.adRequestAuthority(...)`.

---

## Supported ad formats

Banner, MREC, Interstitial, Rewarded.

| MAX format | Adapter behavior | Reward |
|------------|------------------|--------|
| Interstitial | Preload on load; show cached creative | Never |
| Rewarded | Preload on load; show cached creative | Only on `onUserRewarded` |
| Banner / MREC | `getImageAdView`; click forwarded to MAX | — |
| Native | **Not supported** via MAX adapter in 1.2.9 | — |

Use the standalone Bidscube SDK (`getNativeAdView`) for native outside MAX.

### OpenRTB 2.6

OpenRTB 2.6-style podded video response parsing is not implemented in this Android AppLovin adapter package yet.

---

## Troubleshooting

- **Ads do not load:** confirm **`app_id`** (server parameters) and the MAX **Placement ID** match your BidCube setup.
- **SSP override:** use only **host** or **host:port** in `request_authority` / `ssp_host` (not a full ad URL with query string).
- **Custom network not found:** class name must be exactly `com.applovin.mediation.adapters.BidscubeMediationAdapter`.
- **Consent:** run your UMP / consent flow before loading ads where required.
- **Rewarded never fires:** reward is sent only from Bidscube `onUserRewarded`, not on close or skip.
- **LiteNoVideo + video ad unit:** rewarded / interstitial video fail gracefully as unsupported — switch to a video-capable adapter artifact.
- **Ad failures / error codes:** see [docs/errors.md](docs/errors.md) for `AdCallback.onAdFailed` codes (e.g. **204** = no fill, **1004** = missing Activity context).

More detail: [applovin-adapter/README.md](applovin-adapter/README.md).

---

## Error codes (`AdCallback.onAdFailed`)

| Code | Meaning |
|------|---------|
| **204** | No ad fill (HTTP 204 from SSP) |
| **1001** | Ad server HTTP error |
| **1002** | Invalid / unparseable response |
| **1003** | Empty ad markup (ADM) |
| **1004** | Activity context required for display |
| **1005** | Network error (timeout, connection, I/O) |
| **1006** | Ad display error |
| **-1** | Unknown |

Full reference: **[docs/errors.md](docs/errors.md)**.

---

## Runtime behavior

Use your usual **MAX APIs** (`MaxInterstitialAd`, `MaxRewardedAd`, `MaxAdView`, native loaders, etc.). The adapter initializes the BidCube runtime inside **`BidscubeMediationAdapter.initialize`** when MAX starts the network; you do **not** need to call **`BidscubeSDK.initialize(...)`** in app code **for MAX mediation only**.

If you also use **BidscubeSDK directly** in the same app, coordinate initialization so you do not double-init or conflict with config.

---

## Sample app (testing)

The sibling publisher test app ([bidscube-testapp-android](../bidscube-testapp-android)) can point at a test SSP via **Gradle / properties**, for example:

- `bidcube.testSspAuthority` in `gradle.properties` (see that project’s docs).

Local mock SSP: `python3 scripts/mock_bidscube_ssp.py` (may print a tunnel hostname for `adRequestAuthority`).

---

## Standalone Bidscube SDK (without MAX)

Use this when you **do not** use AppLovin MAX.

### Maven

Pick **one** standalone SDK artifact at **1.2.9** (override with env `BidscubeVersion` when building from source):

```kotlin
dependencies {
    implementation("com.bidscube:sdk-full-video:1.2.9@aar")
}
```

Other modes: `sdk-lite-no-video`, `sdk-webview-video`, `sdk-legacy-media-video` — see [Android AAR modes](#android-aar-modes).

### Initialize and consent

```java
import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.config.SDKConfig;

SDKConfig config = new SDKConfig.Builder(this)
        .enableLogging(true)
        .enableDebugMode(false)
        // Optional: .adRequestAuthority("example.com")
        .build();

BidscubeSDK.initialize(this, config);
```

All image, video, and native requests use **`https://<authority>/sdk?…`**. Default authority: **`ssp-bcc-ads.com`**. Override with **`adRequestAuthority`** (host, `host:port`, or `https://host/…` — SDK normalizes; do not paste a full ad URL with query params).

Request consent before loading ads where required (`BidscubeSDK.requestConsentInfoUpdate`, etc.).

### Show ads

```java
BidscubeSDK.showImageAd("20212", callback);
View banner = BidscubeSDK.getImageAdView("20212", callback);
BidscubeSDK.showInterstitialVideoAd("20213", callback);
BidscubeSDK.showRewardedVideoAd("20214", callback);
BidscubeSDK.getNativeAdView("20215", callback);
```

Display mode follows server `position`; optional `BidscubeSDK.setAdPosition(...)`.

### Reference: `SDKConfig` and SSP query (summary)

| You set | Notes |
|--------|--------|
| **`adRequestAuthority`** | Optional; default `ssp-bcc-ads.com` |
| **`appId`**, **`appName`**, … | Optional; else from package / system |
| **Consent fields** | Optional; can tie to UMP |

Per ad: placement id (and native **w** / **h**). The SDK builds **GET** `https://<authority>/sdk?...` with `placementId` / `id`, device, `ifa`, etc.

### Local AAR / modules

- Build all SDK AARs: `./gradlew :sdk:stageReleaseAars` → `sdk/build/staged-aars/bidscube-sdk-*-1.2.9.aar`
- Build all adapter AARs: `./gradlew :applovin-adapter:stageReleaseAars` → `applovin-adapter/build/staged-aars/applovin-bidscube-max-adapter-*-1.2.9.aar`
- Build everything into one folder: `./gradlew stageAllReleaseAars` → `build/staged-aars/`
- Include `:sdk` or `:applovin-adapter` as Gradle modules with matching `videoMode` flavor, or `files("libs/...")` with `flatDir` if needed.

---

## Android AAR modes

The SDK and AppLovin MAX adapter are published in **four** variants. Pick **one** adapter artifact; its POM depends on the matching SDK artifact (not always `sdk-full-video`).

| Mode | Adapter Maven artifact | Core SDK artifact | Video | Media3 / IMA | Desugaring in host app |
|------|------------------------|-------------------|-------|--------------|------------------------|
| **LiteNoVideo** | `applovin-bidscube-max-adapter-lite-no-video` | `sdk-lite-no-video` | No | No | Not required |
| **WebViewVideoNoDesugar** | `applovin-bidscube-max-adapter-webview-video` | `sdk-webview-video` | WebView / HTML5 | No | Not required |
| **LegacyMediaVideoNoDesugar** | `applovin-bidscube-max-adapter-legacy-media-video` | `sdk-legacy-media-video` | VideoView / MediaPlayer | No | Not required |
| **FullWithVideo** | `applovin-bidscube-max-adapter-full-video` | `sdk-full-video` | Media3 / Google IMA | Yes | May be required |

**Gradle dependency examples** (version **1.2.9**):

```kotlin
// LiteNoVideo — no video; rewarded/interstitial video return unsupported / no fill
implementation("com.bidscube:applovin-bidscube-max-adapter-lite-no-video:1.2.9@aar")

// WebViewVideoNoDesugar — HTML5 video via WebView
implementation("com.bidscube:applovin-bidscube-max-adapter-webview-video:1.2.9@aar")

// LegacyMediaVideoNoDesugar — legacy Android media player path
implementation("com.bidscube:applovin-bidscube-max-adapter-legacy-media-video:1.2.9@aar")

// FullWithVideo — full Media3 / IMA stack
implementation("com.bidscube:applovin-bidscube-max-adapter-full-video:1.2.9@aar")
```

Notes:

- The first three modes should **not** require `coreLibraryDesugaring` in the host app.
- **FullWithVideo** may require `coreLibraryDesugaringEnabled true` and `coreLibraryDesugaring "com.android.tools:desugar_jdk_libs:…"` in the host app.
- **AppLovin SDK** is declared as a dependency of the adapter publication; you may also add `com.applovin:applovin-sdk` explicitly in the app if your setup requires it.
- **MAX Dashboard adapter class** remains `com.applovin.mediation.adapters.BidscubeMediationAdapter` for all four modes.

---

## Test a custom SSP host locally

1. Without a custom host, requests use **`ssp-bcc-ads.com`**.
2. For HTTPS tunnel to local mock: run **`python3 scripts/mock_bidscube_ssp.py`** (port **8787**; may start **cloudflared** / **ngrok**). Pass **only the hostname** (e.g. `xxxx.trycloudflare.com`) into **`adRequestAuthority`** or MAX **`request_authority`**.
3. Enable **`.enableLogging(true)`** and check logcat for **`HttpProvider`** — expect `GET https://<host>/sdk?...`.

---

## Override rendering (standalone)

Implement **`onAdRenderOverride(AdRenderContext)`** in **`AdCallback`** and return **`true`** to render ADM / native yourself. See [docs/guide.md](docs/guide.md).

---

## Build and publish (maintainers)

See **[RELEASE.md](RELEASE.md)** for the full checklist.

**Local release AARs:**

```bash
export BidscubeVersion=1.2.9
export BidscubeAdapterVersion=1.2.9
./gradlew clean stageAllReleaseAars -PskipSigning=true --no-daemon
# build/staged-aars/ — 8 AAR files (4 SDK + 4 adapter)
```

**Maven Central:** publish all four SDK variants, then all four adapter variants.

```bash
export BidscubeVersion=1.2.9
export BidscubeAdapterVersion=1.2.9
./gradlew :sdk:publishLiteNoVideoReleasePublicationToCentralRepository \
  :sdk:publishWebViewVideoReleasePublicationToCentralRepository \
  :sdk:publishLegacyMediaVideoReleasePublicationToCentralRepository \
  :sdk:publishFullVideoReleasePublicationToCentralRepository \
  :applovin-adapter:publishLiteNoVideoReleasePublicationToCentralRepository \
  :applovin-adapter:publishWebViewVideoReleasePublicationToCentralRepository \
  :applovin-adapter:publishLegacyMediaVideoReleasePublicationToCentralRepository \
  :applovin-adapter:publishFullVideoReleasePublicationToCentralRepository \
  --no-daemon
```

---

## Platform

- **minSdk** 24 · **Java** 11+ · **Kotlin** 2.0+ (where used)

**AndroidManifest.xml:**

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
