# Bidscube SDK for Android

Android SDK and **AppLovin MAX** adapter for BidCube demand. For mediation, use the Maven artifact **`com.bidscube:applovin-bidscube-adapter`**: it bundles the BidCube runtime (transitively) and the MAX adapter (`BidscubeMediationAdapter`).

**Repository:** https://github.com/BidsCube/bidscube-sdk-android

## Requirements

- **Android** minSdk **24+**
- **Gradle** with `google()` and `mavenCentral()`
- **AppLovin MAX:** `com.applovin:applovin-sdk` **13.0.x** (this repo’s adapter is built against 13.0.0; newer 13.x often works — see adapter source imports if you bump)
- **Adapter:** `com.bidscube:applovin-bidscube-adapter` (version below)
- **JDK 17** and **Android SDK** for building this repo
- **Permissions:** `INTERNET`, `ACCESS_NETWORK_STATE`

In MAX, set **Server Parameters** `app_id` and the MAX **Placement ID** per ad unit (see below).

---

## AppLovin MAX — installing the adapter

### Gradle

Add **AppLovin MAX** and **one** Bidscube line (runtime + adapter). **Do not** add a separate `com.bidscube:bidscube-sdk` / `applovin-bidscube-sdk` dependency for the same target unless you need it explicitly — the adapter POM pulls the core SDK transitively.

**Kotlin (`build.gradle.kts`):**

```kotlin
dependencies {
    implementation("com.applovin:applovin-sdk:13.0.0@aar")
    implementation("com.bidscube:applovin-bidscube-adapter:1.2.5@aar")
}
```

**Groovy (`build.gradle`):**

```groovy
dependencies {
    implementation 'com.applovin:applovin-sdk:13.0.0@aar'
    implementation 'com.bidscube:applovin-bidscube-adapter:1.2.5@aar'
}
```

Sync Gradle. If Maven Central does not resolve the adapter, use a local AAR (see [applovin-adapter/README.md](applovin-adapter/README.md)) or build from this repository.

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

Banner, MREC, Interstitial, Rewarded, Native.

---

## Troubleshooting

- **Ads do not load:** confirm **`app_id`** (server parameters) and the MAX **Placement ID** match your BidCube setup.
- **SSP override:** use only **host** or **host:port** in `request_authority` / `ssp_host` (not a full ad URL with query string).
- **Custom network not found:** class name must be exactly `com.applovin.mediation.adapters.BidscubeMediationAdapter`.
- **Consent:** run your UMP / consent flow before loading ads where required.

More detail: [applovin-adapter/README.md](applovin-adapter/README.md).

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

Artifact **`com.bidscube:bidscube-sdk`** (version from `sdk/build.gradle.kts` / env `BidscubeVersion`, e.g. **1.2.5**):

```kotlin
dependencies {
    implementation("com.bidscube:bidscube-sdk:1.2.5@aar")
}
```

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
BidscubeSDK.showVideoAd("20213", callback);
BidscubeSDK.getNativeAdView("20214", callback);
```

Display mode follows server `position`; optional `BidscubeSDK.setAdPosition(...)`.

### Reference: `SDKConfig` and SSP query (summary)

| You set | Notes |
|--------|--------|
| **`adRequestAuthority`** | Optional; default `ssp-bcc-ads.com` |
| **`appId`**, **`appName`**, … | Optional; else from package / system |
| **Consent fields** | Optional; can tie to UMP |

Per ad: placement id (and native **w** / **h**). The SDK builds **GET** `https://<authority>/sdk?...` with `placementId` / `id`, device, `ifa`, etc.

Full tables and wire details: previous sections of this file are condensed here; see source in `sdk/` (`ImageAdUrlBuilder`, `VideoAdUrlBuilder`, `NativeAdUrlBuilder`, `BidscubeResponseParser`).

### Local AAR / modules

- Build SDK: `./gradlew :sdk:stageReleaseAars` → `sdk/build/staged-aars/bidscube-sdk-*-<version>.aar`
- Include `:sdk` as a Gradle module, or `files("libs/...")` with `flatDir` if needed.

---

## Test a custom SSP host locally

1. Without a custom host, requests use **`ssp-bcc-ads.com`**.
2. For HTTPS tunnel to local mock: run **`python3 scripts/mock_bidscube_ssp.py`** (port **8787**; may start **cloudflared** / **ngrok**). Pass **only the hostname** (e.g. `xxxx.trycloudflare.com`) into **`adRequestAuthority`** or MAX **`request_authority`**.
3. Enable **`.enableLogging(true)`** and check logcat for **`HttpProvider`** — expect `GET https://<host>/sdk?...`.

---

## Override rendering (standalone)

Implement **`onAdRenderOverride(AdRenderContext)`** in **`AdCallback`** and return **`true`** to render ADM / native yourself. Use **`NativeAdBinder`**, **`BannerViewFactory`**, etc., from the SDK.

---

## Build and publish (maintainers)

**Local release AARs:**

```bash
./gradlew clean :sdk:stageReleaseAars :applovin-adapter:assembleRelease --no-daemon
# sdk/build/staged-aars/bidscube-sdk-lite-no-video-<version>.aar
# sdk/build/staged-aars/bidscube-sdk-webview-video-<version>.aar
# sdk/build/staged-aars/bidscube-sdk-legacy-media-video-<version>.aar
# sdk/build/staged-aars/bidscube-sdk-full-video-<version>.aar
# applovin-adapter/build/outputs/aar/applovin-adapter-release.aar
```

**Maven Central:** publish **`bidscube-sdk`** first, then **`applovin-bidscube-adapter`** (adapter POM depends on the SDK version used at publish time). Env: `BidscubeVersion`, `BidscubeAdapterVersion`; credentials and GPG per `gradle.properties`.

```bash
export BidscubeVersion=1.2.5
export BidscubeAdapterVersion=1.2.5
./gradlew :sdk:publishReleasePublicationToCentralRepository :applovin-adapter:publishReleasePublicationToCentralRepository --no-daemon
```

---

## Platform

- **minSdk** 24 · **Java** 11+ · **Kotlin** 2.0+ (where used)

**AndroidManifest.xml:**

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```