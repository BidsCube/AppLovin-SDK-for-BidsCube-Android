# Bidscube SDK for Android

A comprehensive Android SDK for displaying various types of ads including image ads, video ads, native ads, and skippable video ads with GDPR/CCPA compliance.

## Features

- **Multiple Ad Types**: Image, Video, Native, and Skippable Video ads
- **Display Modes**: Full-screen and windowed display options (auto-selected by server response, with manual override)
- **Ad Positioning**: Control ad placement (header, footer, sidebar, above/below fold)
- **Consent Management**: Built-in GDPR and CCPA compliance

## Integration guide

1. **Add dependency** — Maven: `implementation("com.bidscube:applovin-bidscube-sdk:1.0.0@aar")`, or use a local AAR in `libs/`, or include the `sdk` module (see Quick Start below).
2. **Initialize** — `BidscubeSDK.initialize(context, SDKConfig.Builder(context).build());`
3. **Consent** — Call `BidscubeSDK.requestConsentInfoUpdate(callback)` and, if required, `BidscubeSDK.showConsentForm(callback)` before loading ads.
4. **First ad** — After consent, call e.g. `BidscubeSDK.showImageAd("placementId", callback)` or `BidscubeSDK.getImageAdView("placementId", callback)` for a view.
5. **AppLovin MAX** — Add `com.bidscube:applovin-bidscube-adapter:1.0.0@aar` and configure Bidscube as a Custom network in the [MAX Dashboard](https://dash.applovin.com) with server param `app_id`. See [applovin-adapter/README.md](applovin-adapter/README.md).
6. **Requirements** — minSdk 24, permissions: `INTERNET`, `ACCESS_NETWORK_STATE`.

## Quick Start

### 1. Add Dependency

You can consume the SDK as an AAR in several ways — pick the one that matches your workflow.

A) From a Maven repository (preferred when published to a Maven repo)

- If the artifact is published to Maven Central or your Maven repository as an AAR you can add it directly. If you need to force Gradle to treat it as an AAR, append `@aar` to the coordinate.

Kotlin DSL (build.gradle.kts):

```kotlin
repositories {
    google()
    mavenCentral()
}

dependencies {
    // Request the AAR artifact explicitly. Use this when the repository exposes the SDK as an AAR
    implementation("com.bidscube:applovin-bidscube-sdk:1.0.0@aar")

    // If the artifact is published with proper AAR packaging Gradle will normally resolve it
    // implementation("com.bidscube:applovin-bidscube-sdk:1.0.0")
}
```

Groovy DSL (build.gradle):

```groovy
repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation 'com.bidscube:applovin-bidscube-sdk:1.0.0'
    // or force AAR: implementation 'com.bidscube:applovin-bidscube-sdk:1.0.0@aar'
}
```

B) Use the prebuilt AAR locally (useful for testing before publishing)

- Build the SDK in this repository (produces `sdk/build/outputs/aar/sdk-release.aar`) and copy it into your app module's `libs/` directory, or reference it directly.

Groovy DSL (app/build.gradle):

```groovy
// put sdk-release.aar into app/libs/
repositories {
    flatDir { dirs 'libs' }
}

dependencies {
    implementation(name: 'sdk-release', ext: 'aar')
    // or: implementation files('libs/sdk-release.aar')
}
```

Kotlin DSL (app/build.gradle.kts):

```kotlin
repositories {
    flatDir {
        dirs("libs")
    }
}

dependencies {
    implementation(files("libs/sdk-release.aar"))
    // or: implementation("sdk-release") { artifact { name = "sdk-release"; type = "aar" } }
}
```

C) Include the `sdk` module from this project (if you want to build it as part of your app)

- Add the module to your `settings.gradle` (or `settings.gradle.kts`) and add a project dependency.

settings.gradle (Groovy):

```groovy
include ':app', ':sdk'
```

settings.gradle.kts (Kotlin):

```kotlin
include(":app", ":sdk")
```

app/build.gradle (Groovy):

```groovy
dependencies {
    implementation project(':sdk')
}
```

app/build.gradle.kts (Kotlin):

```kotlin
dependencies {
    implementation(project(":sdk"))
}
```

Notes:
- If you use option B (local AAR) remember to rebuild the SDK (`./gradlew :sdk:assembleRelease`) whenever you change SDK code and refresh the AAR file in `app/libs/`.
- Option C lets you develop SDK and app together (recommended for iterative development).

#### D) Use SDK via AppLovin MAX (mediation)

To use Bidscube as a mediated network in AppLovin MAX, add the SDK and the Bidscube AppLovin adapter:

```kotlin
dependencies {
    implementation("com.applovin:applovin-sdk:13.6.0@aar")
    implementation("com.bidscube:applovin-bidscube-sdk:1.0.0@aar")
    implementation("com.bidscube:applovin-bidscube-adapter:1.0.0@aar")
}
```

Then configure Bidscube as a **Custom network** in the [AppLovin MAX Dashboard](https://dash.applovin.com) with server parameter **`app_id`** (your Bidscube Application ID). See `applovin-adapter/README.md` for full integration steps.

### 2. Initialize SDK

```java
import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.config.SDKConfig;

SDKConfig config = new SDKConfig.Builder(this)
        .enableLogging(true)
        .enableDebugMode(false)
        .defaultAdTimeout(30000)
        .defaultAdPosition("UNKNOWN")
        .build();

BidscubeSDK.initialize(this, config);
```

Automatic App Detection: The SDK automatically detects your app's ID, name, version, language, and user agent from the Android manifest and system.

### 3. Show Your First Ad

```java
import com.bidscube.sdk.interfaces.AdCallback;

AdCallback callback = new AdCallback() {
    @Override
    public void onAdLoaded(String placementId) {}

    @Override
    public void onAdFailed(String placementId, int errorCode, String errorMessage) {}
};

// Display mode is determined by server response position
BidscubeSDK.showImageAd("20212", callback);
```

## Ad Types

### Image Ads

```java
BidscubeSDK.showImageAd("20212", callback);

// Or get a View to embed in your layout
View imageView = BidscubeSDK.getImageAdView("19481", callback);
```

### Video Ads

```java
BidscubeSDK.showVideoAd("20213", callback);

// Or get a View to embed
View videoView = BidscubeSDK.getVideoAdView("20213", callback);
```

`showSkippableVideoAd(placementId, callback)` is deprecated and delegates to `showVideoAd`; use `showVideoAd` for new code.

### Native Ads

```java
BidscubeSDK.showNativeAd("20214", callback);

// Or get a View to embed
View nativeView = BidscubeSDK.getNativeAdView("20214", callback);
```

## Ad Positioning

The SDK supports automatic ad positioning based on server response, with manual override capability:

### Response-Based Positioning (Automatic)

Ads are automatically positioned based on the `position` field in the server response:

```java
BidscubeSDK.showImageAd("20212", callback);
BidscubeSDK.showVideoAd("20213", callback);
BidscubeSDK.showNativeAd("20214", callback);
```

Response Position Values:
- `0` - Unknown (natural display)
- `1` - Above the fold (top portion of screen)
- `2` - Maybe depending on screen size (smart positioning)
- `3` - Below the fold (bottom portion of screen)
- `4` - Header (top of screen)
- `5` - Footer (bottom of screen)
- `6` - Sidebar (left/right side)
- `7` - Full screen

### Manual Override (Optional)

You can manually override the response position:

```java
import com.bidscube.sdk.models.enums.AdPosition;

BidscubeSDK.setAdPosition(AdPosition.HEADER);
BidscubeSDK.setAdPosition(AdPosition.FOOTER);
BidscubeSDK.setAdPosition(AdPosition.SIDEBAR);
BidscubeSDK.setAdPosition(AdPosition.ABOVE_THE_FOLD);
BidscubeSDK.setAdPosition(AdPosition.BELOW_THE_FOLD);
BidscubeSDK.setAdPosition(AdPosition.UNKNOWN);

BidscubeSDK.showImageAd("20212", callback);
```

### Smart Positioning (Position 2)

When the response indicates position `2` (maybe depending on screen size):
- **Portrait mode**: Center positioning
- **Landscape mode**: Right side positioning

### Get Current Position

```java
import com.bidscube.sdk.models.enums.AdPosition;

AdPosition effectivePosition = BidscubeSDK.getEffectiveAdPosition();
AdPosition manualPosition = BidscubeSDK.getCurrentAdPosition();
AdPosition responsePosition = BidscubeSDK.getResponseAdPosition();
```

## Consent Management

### Basic Consent Handling

```java
import com.bidscube.sdk.interfaces.ConsentCallback;

BidscubeSDK.requestConsentInfoUpdate(new ConsentCallback() {
    @Override
    public void onConsentInfoUpdated() {
        if (BidscubeSDK.isConsentRequired()) {
            // Within this callback, `this` is a ConsentCallback
            BidscubeSDK.showConsentForm(this);
        } else {
            showAds();
        }
    }

    @Override
    public void onConsentGranted() {
        showAds();
    }

    @Override
    public void onConsentDenied() {
        showAlternativeContent();
    }
});
```

## Override SDK rendering

`AdCallback` exposes `onAdRenderOverride` which receives the ADM payload, resolved ad position, and the SDK render type. Return `true` to prevent the SDK from showing its default dialogs/views and handle rendering yourself:

```kotlin
val callback = object : AdCallback {
    override fun onAdRenderOverride(context: AdRenderContext): Boolean {
        if (context.renderType == AdRenderType.NATIVE) {
            renderMyNativeCard(context.adm, context.position)
            return true
        }
        return false
    }
}
```

### Check Consent Status

```java
boolean isRequired = BidscubeSDK.isConsentRequired();
boolean hasAdsConsent = BidscubeSDK.hasAdsConsent();
boolean hasAnalyticsConsent = BidscubeSDK.hasAnalyticsConsent();
String summary = BidscubeSDK.getConsentStatusSummary();
```

### Debug Mode

```java
BidscubeSDK.enableConsentDebugMode("your_test_device_id");
BidscubeSDK.resetConsent();
```

## Customization

### Native Ad Styling

```java
import com.bidscube.sdk.view.NativeAdView;

NativeAdView nativeAdView = new NativeAdView(context);

nativeAdView.setCTAText("Shop Now");

nativeAdView.setCustomStyle(
        Color.WHITE,
        Color.BLACK,
        Color.parseColor("#FF5722")
);

        nativeAdView.setCTAButton("Install Now", Color.BLUE, Color.WHITE);
```

## Complete Example

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SDKConfig config = new SDKConfig.Builder(this)
                .enableLogging(true)
                .enableDebugMode(false)
                .build();

        BidscubeSDK.initialize(this, config);
        setupConsent();
    }

    private void setupConsent() {
        if (!BidscubeSDK.isInitialized()) return;

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
            public void onConsentGranted() {
                showAds();
            }

            @Override
            public void onConsentDenied() {
                showAlternativeContent();
            }
        });
    }

    private void showAds() {
        if (!BidscubeSDK.hasAdsConsent()) return;

        AdCallback callback = new AdCallback() {
            @Override public void onAdLoaded(String placementId) {}
            @Override public void onAdFailed(String placementId, int errorCode, String errorMessage) {}
        };

        BidscubeSDK.showImageAd("20212", callback);
        BidscubeSDK.showVideoAd("20213", callback);
        BidscubeSDK.showNativeAd("20214", callback);
    }

    private void showAlternativeContent() {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BidscubeSDK.isInitialized()) {
            BidscubeSDK.cleanup();
        }
    }
}
```

## Configuration Options

### SDKConfig Builder

```java
SDKConfig config = new SDKConfig.Builder(this)
        .enableLogging(true)
        .enableDebugMode(false)
        .defaultAdTimeout(30000)
        .defaultAdPosition("UNKNOWN")
        .build();
```

Automatic Detection: The SDK automatically detects:
- **App ID**: Package name from manifest
- **App Name**: Application label from manifest
- **App Version**: Version name/code from manifest
- **Language**: Device language setting
- **User Agent**: SDK version + device info

## Ad Position Options

- `UNKNOWN` - No position regulation, natural display
- `ABOVE_THE_FOLD` - Position above the fold
- `DEPEND ON SCREEN SIZE` - Depending on the screen size
- `BELOW_THE_FOLD` - Position below the fold
- `HEADER` - Position at top of screen
- `FOOTER` - Position at bottom of screen
- `SIDEBAR` - Position on side of screen
- `FULL_SCREEN` - Full screen ad

## Callback Methods

### AdCallback Interface

```java
public interface AdCallback {
    void onAdLoading(String placementId);
    void onAdLoaded(String placementId);
    void onAdDisplayed(String placementId);
    void onAdClicked(String placementId);
    void onAdClosed(String placementId);
    void onAdFailed(String placementId, int errorCode, String errorMessage);

    default void onVideoAdStarted(String placementId) {}
    default void onVideoAdCompleted(String placementId) {}
    default void onVideoAdSkipped(String placementId) {}
    default void onVideoAdSkippable(String placementId) {}
    default void onInstallButtonClicked(String placementId, String buttonText) {}
}
```

### ConsentCallback Interface

```java
public interface ConsentCallback {
    void onConsentInfoUpdated();
    void onConsentInfoUpdateFailed(Exception error);
    void onConsentFormShown();
    void onConsentFormError(Exception formError);
    void onConsentGranted();
    void onConsentDenied();
    void onConsentNotRequired();
    void onConsentStatusChanged(boolean hasConsent);
}
```

## Testing

Use your own test screens or a separate test app to exercise SDK initialization, consent, and ad placement. Enable logging and debug mode during development.

### Debug Mode

```java
BidscubeSDK.enableConsentDebugMode("test_device_123");

        boolean isReady = BidscubeSDK.isInitialized();

import com.bidscube.sdk.models.enums.AdPosition;
AdPosition position = BidscubeSDK.getCurrentAdPosition();
```

## Error Handling

### Common Error Scenarios

```java
try {
        BidscubeSDK.showImageAd("19481", callback);
} catch (IllegalStateException e) {
        // SDK not initialized
        } catch (Exception e) {
        // Unexpected error
        }
```

### Error Codes

- `-1` - General failure
- `-2` - Network error
- `-3` - Parsing error
- `-4` - Invalid placement ID

## Platform Requirements

- **Minimum SDK**: API 24 (Android 7.0)
- **Target/Compile SDK**: API 35 (Android 15)
- **Java Version**: 11+
- **Kotlin**: 2.0+

## Permissions

Add these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

## Additional Resources

- Use the Integration guide and example code for development and debugging
- **VAST Support**: Full VAST XML parsing for video ads
- **Native Ad Models**: Complete OpenRTB Native Ads specification support

## Support

For support and questions:
- Check the example code and Integration guide
- Review the comprehensive logging output
- Ensure proper initialization and consent handling

## License

This SDK is protected under the MIT License. For full license terms, please refer to the LICENSE file included with this distribution.

## Version

This README and examples are updated for AppLovin Bidscube SDK version 1.0.0.

## What's new in 1.0.0 (user-facing)

- Unified rendering hook: a single API `AdCallback.onAdRenderOverride(AdRenderContext)` lets hosts fully control rendering for ALL ad types (image/HTML ADM, video, native) — return true to take over rendering and skip the SDK default UI.
- Native rendering compatibility: the SDK provides helpers so you can render OpenRTB-style native ads with either the provided banner layout or with your custom layout.
- Banner normalization: creatives returned as ADM are cleaned and rendered to fit device width, while maintaining aspect ratio; banners are shown as static overlays (not scrollable) and anchored to header/footer/sidebar according to the ad position.
- Native banner behavior: for compact banner placements the SDK skips loading very large main images to save bandwidth and memory; it will show title/subtitle and a small icon instead.
- Customization: native banner background color, disclosure icon, image scale behavior and text ellipsizing are configurable via binder helpers.
- Test activity UX: the sample `SDKTestActivity` initializes the SDK with a minimal onCreate; additional controls (placement input, position spinner, width/height) are created after initialization so you can experiment safely.

## User-facing features and how to use them

This section summarizes the APIs and behaviour relevant to SDK integrators. It intentionally excludes repository, build and CI details.

1) Unified render override

- API point: implement `AdCallback` and override `onAdRenderOverride(AdRenderContext context)`.
- Return value: return `true` if you handled rendering and want the SDK to skip its default UI.
- What `AdRenderContext` provides:
    - `getPlacementId()` — placement id string
    - `getAdm()` — sanitized ADM (wrapper like document.write removed when possible)
    - `getPosition()` — `AdPosition` enum (HEADER/FOOTER/SIDEBAR/FULL_SCREEN/UNKNOWN)
    - `getRenderType()` — ad type (IMAGE, VIDEO, NATIVE)
    - `getNativeAd()` — parsed native model (nullable) when SDK was able to parse OpenRTB native

- Recommended approach:
    - If `getNativeAd()` is non-null, render using that typed model (safer and easier).
    - Otherwise use `getAdm()` and load into a `WebView` or reuse the SDK `BannerViewFactory` to create a responsive banner view.

Example (pseudo):

```java
AdCallback cb = new AdCallback() {
    @Override
    public boolean onAdRenderOverride(AdRenderContext ctx) {
        if (ctx.getNativeAd() != null) {
            // render using typed model
            renderMyNative(ctx.getNativeAd());
            return true;
        }
        String adm = ctx.getAdm();
        if (adm != null && !adm.isEmpty()) {
            // render adm in your WebView or use SDK helper
            myWebView.loadData(adm, "text/html", "utf-8");
            return true;
        }
        return false; // let SDK fallback to default rendering
    }
};
```

2) Native ad rendering helpers

- `NativeAdBinder.bind(...)` and `NativeAdBinder.bindToView(...)` — populate simple custom layouts (title, body, cta, icon, main image).
    - Use these when you inflate your own layout and want the SDK to populate it without adopting the SDK's built-in banner view.
- `NativeAdBinder.createBannerView(...)` and `NativeAdBinder.attachBannerToContainer(...)` — convenient helpers to create a compact full-width banner view from a parsed `NativeAd` model. Accepts:
    - widthPx, heightPx: desired pixel size (the SDK will respect reasonable limits for header/footer)
    - backgroundColor: pass a color to change default white background
    - showAdDisclosure: toggle the small ad-info icon shown in the corner
    - main image scale type and text ellipsizing options

Examples (pseudo):

```java
// Bind to an already-inflated host layout
NativeAdBinder.bindToView(nativeAd, context, myRootView, R.id.title, R.id.body, R.id.cta, R.id.icon, R.id.mainImage);

// Attach SDK-provided banner into your container with white bg
NativeAdBinder.attachBannerToContainer(context, nativeAd, container, screenWidthPx, bannerHeightPx, Color.WHITE);

// Create a banner view with custom background
View banner = NativeAdBinder.createBannerView(context, nativeAd, screenWidthPx, bannerHeightPx, Color.parseColor("#FFFFFF"));
container.addView(banner);
```

Notes:
- For non-fullscreen native placements the binder will avoid loading extremely large main images. If the banner height is small it will show icon + title + subtitle instead.
- The binder does not substitute the Android app icon for missing native assets — it uses only assets present in the native response.

3) Banner rendering and sizing

- Use `BannerViewFactory.createBanner(context, adm)` to get a WebView configured for responsive banner rendering.
- The factory injects CSS/JS to normalize sizes, removes common tracking beacons and computes content height so the WebView is sized to the creative.
- Banners are displayed as overlays attached to the Activity root and are static (not scrollable). When placed in HEADER or FOOTER the overlay will be anchored to top/bottom respectively.

4) Placement selection and defaults

- Example placement ids: image 20212, video 20213, native 20214. Use any placement id your account supports.
- Programmatic override: use `BidscubeSDK.setAdPosition(AdPosition position)` to set a manual position; an explicit manual position is honored over response position when provided.

5) Showing ads (behavior summary)

- Call the SDK show methods with a placement id and an `AdCallback`:
    - `BidscubeSDK.showImageAd(String placementId, AdCallback callback)`
    - `BidscubeSDK.showVideoAd(String placementId, AdCallback callback)`
    - `BidscubeSDK.showNativeAd(String placementId, AdCallback callback)`

- If you implement `onAdRenderOverride` in your `AdCallback` and return `true` you are responsible for creating and attaching views and handling click behavior.

6) Integration testing

- In your test Activity, initialize the SDK in `onCreate`, then add controls that call `showImageAd` / `showVideoAd` / `showNativeAd` with your placement ids and an `AdCallback`. Use `onAdRenderOverride` in the callback to customize rendering if needed.

7) Troubleshooting & tips

- If a banner creative appears cropped or not full-width in your integration, make sure the container you attach it to allows MATCH_PARENT width and no extra outer padding/margin is constraining it.
- For native banners: if you want the full main image to appear, request a larger banner height (>= ~200dp). For small banners the binder will prefer icon + text.
- Prefer rendering parsed `NativeAd` models from `AdRenderContext.getNativeAd()` when available — it's more reliable than trying to parse ADM HTML in-host.

