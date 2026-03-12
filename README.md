# Bidscube SDK for Android

Android SDK for image, video, and native ads with GDPR/CCPA consent. Can be used standalone or via the **AppLovin MAX** adapter.

## Requirements

- **Android** minSdk 24+
- **Bidscube SDK** 1.0.0+
- **AppLovin MAX** (optional) SDK 13.6.0+ and adapter `applovin-bidscube-adapter` 1.0.0+
- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`
- For MAX mediation: Bidscube init value **`app_id`** and a MAX **Placement ID** per ad unit

---

## Add the SDK

### From Maven

```kotlin
// build.gradle.kts
repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.bidscube:applovin-bidscube-sdk:1.0.0@aar")
}
```

```groovy
// build.gradle
repositories {
    google()
    mavenCentral()
}
dependencies {
    implementation 'com.bidscube:applovin-bidscube-sdk:1.0.0@aar'
}
```

### Local AAR

Build this repo: `./gradlew :sdk:assembleRelease`, then copy `sdk/build/outputs/aar/sdk-release.aar` into your app `libs/` and add:

```kotlin
repositories { flatDir { dirs("libs") } }
dependencies { implementation(files("libs/sdk-release.aar")) }
```

### As a module

In `settings.gradle.kts`: `include(":app", ":sdk")` (and point `:sdk` to this repo’s `sdk` folder if needed).  
In app `build.gradle.kts`: `implementation(project(":sdk"))`.

---

## AppLovin MAX integration

To use Bidscube as a **Custom network** in AppLovin MAX:

### 1. Add dependencies

```kotlin
dependencies {
    implementation("com.applovin:applovin-sdk:13.6.0@aar")
    implementation("com.bidscube:applovin-bidscube-adapter:1.0.0@aar")
}
```

The Bidscube SDK is pulled in transitively by the adapter. Only add `com.bidscube:applovin-bidscube-sdk` explicitly if your repository setup does not resolve transitive AAR dependencies correctly.

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
- **Custom Parameters**: not used by the current adapter implementation

The adapter reads `app_id` from **Server Parameters** and the ad-specific value from the MAX **Placement ID** field.

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
        .build();

BidscubeSDK.initialize(this, config);
```

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

## Local build (same as CI)

From project root (wrapper is already in repo):

```bash
./gradlew clean :sdk:assembleRelease --no-daemon
# AAR: sdk/build/outputs/aar/sdk-release.aar
```

Adapter:

```bash
./gradlew clean :applovin-adapter:assembleRelease --no-daemon
```

Requires JDK 17 and, for the adapter, Android SDK (`ANDROID_HOME`).

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

**Version:** AppLovin Bidscube SDK 1.0.0.
