# Bidscube SDK for Android

Android SDK for image, video, and native ads with GDPR/CCPA consent. Can be used standalone or via the **AppLovin MAX** adapter.

## Requirements

- **Android** minSdk 24+
- **Bidscube SDK** (`com.bidscube:bidscube-sdk`) — для прямої інтеграції або транзитивно через адаптер MAX
- **AppLovin MAX** (optional): SDK 13.0.0+ і **лише** `com.bidscube:applovin-bidscube-adapter` (окремий рядок `bidscube-sdk` не потрібен — див. нижче)
- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`
- For MAX mediation: Bidscube init value **`app_id`** and a MAX **Placement ID** per ad unit

---

## Add the SDK

### From Maven (direct SDK, без MAX)

Координата артефакту: **`com.bidscube:bidscube-sdk`** (версія з `sdk/build.gradle.kts` / env `BidscubeVersion`, наприклад `1.2.2`).

```kotlin
dependencies {
    implementation("com.bidscube:bidscube-sdk:1.2.2@aar")
}
```

```groovy
dependencies {
    implementation 'com.bidscube:bidscube-sdk:1.2.2@aar'
}
```

### Local AAR

Build this repo: `./gradlew :sdk:assembleRelease`, then copy `sdk/build/outputs/aar/sdk-release.aar` into your app `libs/` and add:

```kotlin
repositories { flatDir { dirs("libs") } }
dependencies { implementation(files("libs/sdk-release.aar")) }
```

### Publisher test app (`bidscube-testapp-android`)

The sibling folder [bidscube-testapp-android](../bidscube-testapp-android) depends on this `:sdk` module. It is included from this repo as **`:bidscube-testapp-android`**.

- Build & install: `./gradlew :bidscube-testapp-android:installDebug` (application id **`com.bidscube.publisher.testapp`** — not the Play Store package name; uninstall any old `com.bidscube.sdk` debug build if you had one).
- Custom SSP / mock: run `python3 scripts/mock_bidscube_ssp.py` (може сам відкрити публічний URL через cloudflared/ngrok). Для **debug** задайте `bidcube.testSspAuthority=<host>` у [bidscube-testapp-android/gradle.properties](../bidscube-testapp-android/gradle.properties) і перезіберіть. Деталі: **Test a custom SSP host locally**.

You can also open only the test app folder and run `./gradlew assembleDebug`. За замовчанням `bidscube.useLocalAdapter=true` (якщо поруч є `../bidscube-sdk-android`) підключаються модулі `:applovin-adapter` + `:sdk` — **один** `implementation project(':applovin-adapter')`, без окремого рядка на SDK.

### As a module

In `settings.gradle.kts`: `include(":app", ":sdk")` (and point `:sdk` to this repo’s `sdk` folder if needed).  
In app `build.gradle.kts`: `implementation(project(":sdk"))`.

---

## AppLovin MAX integration

To use Bidscube as a **Custom network** in AppLovin MAX:

### 1. Add dependencies

Для MAX достатньо **двох** рядків: AppLovin + адаптер. **`bidscube-sdk` не додавайте вручну** — він підтягується транзитивно з POM адаптера (`api` + залежність у Maven metadata).

```kotlin
dependencies {
    implementation("com.applovin:applovin-sdk:13.0.0@aar")
    implementation("com.bidscube:applovin-bidscube-adapter:1.0.2.2@aar")
}
```

Якщо ви копіюєте лише «голий» `applovin-adapter-release.aar` у `libs/` без Gradle/Maven, додайте ще **`sdk-release.aar`** (або `com.bidscube:bidscube-sdk`) — один AAR адаптера не містить класів SDK.

### 2. MAX Dashboard setup

Follow [AppLovin’s guide for custom SDK networks](https://support.axon.ai/en/max/mediated-network-guides/integrating-custom-sdk-networks/):

1. Open [AppLovin MAX Dashboard](https://dash.applovin.com) and select your app (bundle ID must match).
2. Go to **MAX → Mediation → Manage → Networks**.
3. Click **Click here to add a Custom Network** and create the network:
   - **Network Type**: **SDK**
   - **Name**: `Bidscube`
   - **Android Adapter Class Name**: `com.applovin.mediation.adapters.BidscubeMediationAdapter`
4. Go to **MAX → Mediation → Manage → Ad Units**, select each ad unit where you want Bidscube, enable **Bidscube** and set the values for that placement.

### 3. MAX parameters

- **Android Adapter Class Name**: `com.applovin.mediation.adapters.BidscubeMediationAdapter`
- **`app_id`**: Bidscube init identifier used by the adapter during SDK initialization
- **Placement ID**: the Bidscube placement used for the specific MAX ad unit request
- **`request_authority`** / **`ssp_host`** (optional): override the HTTPS host for Bidscube ad requests (see [applovin-adapter/README.md](applovin-adapter/README.md))

The adapter reads `app_id` from **Server Parameters** and the ad-specific value from the MAX **Placement ID** field.

**Domain / traffic endpoint:** MAX has no separate domain field; use optional **`request_authority`** or **`ssp_host`** in server parameters, or `SDKConfig.Builder.adRequestAuthority(...)` for direct SDK use. Default: `ssp-bcc-ads.com`. Details: **Endpoint / domain (traffic)** in [applovin-adapter/README.md](applovin-adapter/README.md).

### 4. Supported ad formats

Banner, MREC, Interstitial, Rewarded, Native.

Full adapter steps: [applovin-adapter/README.md](applovin-adapter/README.md).

### 5. Troubleshooting

- If the network initializes but ads do not load, verify both **`app_id`** and the MAX **Placement ID**.
- If MAX does not recognize the custom network, verify the Android adapter class name is `com.applovin.mediation.adapters.BidscubeMediationAdapter`.
- Run consent before initializing the SDK and loading ads.

---

## Initialize and consent

```java
import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.config.SDKConfig;

SDKConfig config = new SDKConfig.Builder(this)
        .enableLogging(true)
        .enableDebugMode(false)
        // Optional: HTTPS host[:port] for ad URLs (default ssp-bcc-ads.com), e.g.:
        // .adRequestAuthority("example.com")
        .build();

BidscubeSDK.initialize(this, config);
```

### Ad request endpoint (custom SSP host / pasted link)

All image, video, and native ad requests use **`https://<authority>/sdk?…`**. By default `authority` is `ssp-bcc-ads.com`. Override it **only via the SDK** with `SDKConfig.Builder.adRequestAuthority(String)` before `BidscubeSDK.initialize`.

**What you can pass** (the SDK normalizes input and builds `/sdk` itself):

| Input example | Resulting authority |
|----------------|---------------------|
| `example.com` | `example.com` (HTTPS, default port **443**) |
| `edge.example.com` | `edge.example.com` (HTTPS, default port 443) |
| `127.0.0.1:8787` | host `127.0.0.1`, port `8787` (local dev + tunnel) |
| `https://my-host.trycloudflare.com/` | `my-host.trycloudflare.com` (scheme and path stripped) |
| Value with `%3A` etc. | Percent-decoded first, then parsed |

Example in code:

```java
new SDKConfig.Builder(context)
        .adRequestAuthority("example.com")   // GET https://example.com/sdk?...
        .build();
```

**Do not** put the full ad URL with query string into `adRequestAuthority` — only **host**, optional **port**, or a **short URL prefix**; query parameters are added by the SDK (`ImageAdUrlBuilder`, `VideoAdUrlBuilder`, `NativeAdUrlBuilder`).

**AppLovin MAX:** set **`request_authority`** or **`ssp_host`** in server parameters (host only, same rules); the adapter forwards the value into `SDKConfig.Builder.adRequestAuthority(...)`. See [applovin-adapter/README.md](applovin-adapter/README.md).

**Publisher test app:** for debug builds you can set `bidcube.testSspAuthority=<host>` in `bidscube-testapp-android/gradle.properties` (copied from `python3 scripts/mock_bidscube_ssp.py` after cloudflared prints the trycloudflare URL), then rebuild.

### Reference: data you pass and wire format

Two layers: **what you set in code / MAX**, and **what the SDK adds** to the HTTP request to the SSP.

#### 1) Before `BidscubeSDK.initialize` (`SDKConfig.Builder`)

| Input | Format | Notes |
|-------|--------|--------|
| **`adRequestAuthority`** (optional) | Single UTF-8 string: `host`, `host:port`, `https://host/…`, optional percent-encoding (`%3A`, etc.) | SDK normalizes it and builds **`https://<authority>/sdk`**. Default authority is `ssp-bcc-ads.com`. Example host: **`example.com`**. |
| **`appId`**, **`appName`**, **`appVersion`**, **`language`**, **`userAgent`** (optional) | Strings | If omitted, values come from the app package / system. They appear in the request as `bundle`, `name`, etc. |
| **`gdpr`**, **`gdprConsent`**, **`usPrivacy`**, **`coppa`** (optional) | `Integer` / `String` / `Boolean` or `null` | `null` → for some fields the SDK uses **UMP / ConsentManager** after consent is collected. |
| **`enableLogging`**, **`enableDebugMode`**, **`defaultAdTimeout`**, **`defaultAdPosition`** | boolean / int / String | Not sent in the SSP request body; control SDK behavior only. |

#### 2) On each ad load (your input)

| SDK API | Parameter | Format |
|---------|-----------|--------|
| Image (banner, etc.) | **placement id** | String, e.g. `"20212"` → query **`placementId`** |
| Video | placement **id** | String → query **`id`** |
| Native | **placement id** + **w**, **h** | String + two `double` values (logical ad size) → query **`id`**, **`w`**, **`h`** |

**AppLovin MAX** (dashboard): **`app_id`** (string, server parameters), **Placement ID** (string), optional **`request_authority`** / **`ssp_host`** — same format rules as `adRequestAuthority` (e.g. `example.com` or `https://example.com/` stripped to host).

#### 3) What the SDK sends to the SSP (automatic)

- **Method:** `GET`
- **URL:** `https://<authority>/sdk?<query>` — query built with `Uri.appendQueryParameter` (standard **percent-encoding** for UTF-8 and special characters).

**Image** (`c=b`, `m=api`, `res=js`, `app=1` plus `DeviceInfo` fields):

| Query key | Value (as string) | Source |
|-----------|-------------------|--------|
| `placementId` | string | your API call |
| `c`, `m`, `res`, `app` | fixed literals | SDK |
| `bundle`, `name`, `app_store_url`, `language` | strings | `DeviceInfo` / config |
| `deviceWidth`, `deviceHeight` | integers via `String.valueOf` | display |
| `ua` | string | WebView / system user-agent |
| `ifa` | string or empty | advertising ID |
| `dnt` | `0` / `1` | limit ad tracking |

**Video** (`c=v`, `m=xml`, `app=1`): same idea — `id`, `w`, `h` (screen), `bundle`, `name`, `app_version`, `ifa`, `dnt`, `app_store_url`, `ua`, `language`, `deviceWidth`, `deviceHeight`.

**Native** (`c=n`, `m=s`, `app=1`): `id`, `bundle`, `name`, `app_version`, `ifa`, `dnt`, `app_store_url`, `ua`, **`gdpr`**, **`gdpr_consent`**, **`us_privacy`**, **`ccpa`**, **`coppa`**, `language`, `deviceWidth`, `deviceHeight`, **`w`**, **`h`** (sizes from the native call).

#### 4) SSP response (expected by the SDK from mock or production)

- **Body:** JSON, UTF-8  
- **Fields:** **`adm`** (string — usually HTML/JS creative), **`position`** (integer, layout hint)  
- Parser: `BidscubeResponseParser` (see `sdk` module).

Before loading ads, request consent and show the form if required:

```java
import com.bidscube.sdk.interfaces.ConsentCallback;

BidscubeSDK.requestConsentInfoUpdate(new ConsentCallback() {
    @Override
    public void onConsentInfoUpdated() {
        if (BidscubeSDK.isConsentRequired()) {
            BidscubeSDK.showConsentForm(this);
        } else {
            showAds();
        }
    }
    @Override
    public void onConsentGranted() { showAds(); }
    @Override
    public void onConsentDenied() { showAlternativeContent(); }
});
```

---

## Test a custom SSP host locally

1. **Build the SDK:** `./gradlew :sdk:assembleRelease` → AAR at `sdk/build/outputs/aar/sdk-release.aar`. (The `:applovin-adapter` module needs the AppLovin MAX SDK on the Gradle classpath; build it from an app project that already depends on MAX, or publish the adapter from CI.)
2. **Baseline without a custom host:** leave `adRequestAuthority` unset; requests go to the default production host (`ssp-bcc-ads.com`).
3. **Custom host + mock SSP:** the SDK always uses **HTTPS**, so a plain `http://10.0.2.2:8787` URL will not work from the app without extra TLS setup. Practical approach:
   - Run `python3 scripts/mock_bidscube_ssp.py` — starts the local mock on port **8787** and by default tries a public HTTPS tunnel (**cloudflared**, then **ngrok**) if they are on `PATH`. The script prints a ready line for `bidcube.testSspAuthority` / `adRequestAuthority`.
   - If the tunnel does not start: run **`cloudflared tunnel --url http://127.0.0.1:8787`** or **`ngrok http 8787`** in another terminal, or `python3 scripts/mock_bidscube_ssp.py --no-tunnel` and start a tunnel manually.
   - Pass **only the tunnel hostname** (no `https://`, no path), e.g. `abcd123.trycloudflare.com` or `abcd123.ngrok-free.app`, via `SDKConfig.Builder(context).adRequestAuthority("…")`, MAX **`request_authority`**, or **`bidcube.testSspAuthority`** in the publisher test app. For a staging hostname such as **`example.com`**, use `.adRequestAuthority("example.com")` the same way.
4. **Verify:** enable logging (`.enableLogging(true)`) and check logcat for `HttpProvider` — you should see a GET to `https://<your-host>/sdk?...`.

---

## Show ads

```java
import com.bidscube.sdk.interfaces.AdCallback;

AdCallback callback = new AdCallback() {
    @Override public void onAdLoaded(String placementId) {}
    @Override public void onAdFailed(String placementId, int errorCode, String errorMessage) {}
};

// Image
BidscubeSDK.showImageAd("20212", callback);
View imageView = BidscubeSDK.getImageAdView("20212", callback);

// Video (showSkippableVideoAd is deprecated, use showVideoAd)
BidscubeSDK.showVideoAd("20213", callback);

// Native
BidscubeSDK.showNativeAd("20214", callback);
View nativeView = BidscubeSDK.getNativeAdView("20214", callback);
```

Display mode (full-screen, header, footer, sidebar) is driven by the server `position`; you can override with `BidscubeSDK.setAdPosition(AdPosition.HEADER)` (etc.).

---

## Override rendering

Implement `onAdRenderOverride(AdRenderContext)` in your `AdCallback` and return `true` to handle rendering yourself (e.g. custom native card or WebView for ADM):

```kotlin
override fun onAdRenderOverride(context: AdRenderContext): Boolean {
    if (context.nativeAd != null) {
        renderMyNative(context.nativeAd)
        return true
    }
    if (!context.adm.isNullOrEmpty()) {
        myWebView.loadData(context.adm, "text/html", "utf-8")
        return true
    }
    return false
}
```

Use `NativeAdBinder.bindToView(...)` / `NativeAdBinder.createBannerView(...)` for native ads; `BannerViewFactory.createBanner(context, adm)` for HTML/ADM banners.

---

## Ad position values

- `0` Unknown · `1` Above the fold · `2` Depends on screen size · `3` Below the fold  
- `4` Header · `5` Footer · `6` Sidebar · `7` Full screen

`BidscubeSDK.getEffectiveAdPosition()`, `getCurrentAdPosition()`, `getResponseAdPosition()`.

---

## Release to Maven Central (MAX «автономно» для клієнтів)

Щоб інтегратори додавали **тільки** `applovin-bidscube-adapter`, а `BidscubeSDK` з’являвся автоматично:

1. **Спочатку** опублікуйте **`com.bidscube:bidscube-sdk`** (той самий `BidscubeVersion`, що піде в POM адаптера).
2. **Потім** опублікуйте **`com.bidscube:applovin-bidscube-adapter`** (`BidscubeAdapterVersion`).

Локально (потрібні `mavenCentralUsername` / `mavenCentralPassword` у `gradle.properties` або `-P`, підпис GPG за `signing`):

```bash
export BidscubeVersion=1.2.2
export BidscubeAdapterVersion=1.0.2.2
./gradlew :sdk:publishReleasePublicationToCentralRepository --no-daemon
./gradlew :applovin-adapter:publishReleasePublicationToCentralRepository --no-daemon
```

У POM адаптера є залежність **`com.bidscube:bidscube-sdk`** з версією з модуля `:sdk` на момент публікації. Модуль адаптера оголошує `api(project(":sdk"))`, тож API SDK доступний у застосунку з одним рядком `implementation` на адаптер.

---

## Local build (same as CI)

From project root (wrapper is already in repo):

```bash
./gradlew clean :sdk:assembleRelease :applovin-adapter:assembleRelease --no-daemon
# SDK AAR: sdk/build/outputs/aar/sdk-release.aar
# Adapter AAR: applovin-adapter/build/outputs/aar/applovin-adapter-release.aar
```

Requires JDK 17 and Android SDK (`ANDROID_HOME`).

---

## Platform

- **minSdk** 24 · **Target** 35 · **Java** 11+ · **Kotlin** 2.0+

**Permissions** in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## License

MIT. See [LICENSE](LICENSE).

**Versions:** `bidscube-sdk` — env `BidscubeVersion` / `sdk/build.gradle.kts`; `applovin-bidscube-adapter` — env `BidscubeAdapterVersion` / `applovin-adapter/build.gradle.kts`.
