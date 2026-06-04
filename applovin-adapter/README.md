# Bidscube + AppLovin MAX Integration

**Adapter version 1.2.7** · Maven group `com.bidscube`

AppLovin MAX mediation adapter for Bidscube SDK. Use Bidscube as a custom network in AppLovin MAX.

**Related docs:** [Main README](../README.md) · [Error codes](../docs/errors.md) · [CHANGELOG](../CHANGELOG.md) · [RELEASE](../RELEASE.md)

## Requirements

- **Android** minSdk 24+
- **AppLovin MAX SDK** 13.0.0+
- **One** Bidscube adapter artifact at **1.2.7** for your video mode (see [Android AAR modes](#android-aar-modes))
- AppLovin **SDK Key** and **Ad Units**
- Bidscube init value **`app_id`** and a MAX **Placement ID** per ad unit

## Android AAR modes

Pick **one** adapter artifact. Each bundles the matching Bidscube SDK runtime transitively via Maven POM.

| Mode | Maven artifact | Video support |
|------|----------------|---------------|
| **LiteNoVideo** | `com.bidscube:applovin-bidscube-max-adapter-lite-no-video:1.2.7` | None (image/banner/native only) |
| **WebViewVideoNoDesugar** | `com.bidscube:applovin-bidscube-max-adapter-webview-video:1.2.7` | WebView / HTML5 |
| **LegacyMediaVideoNoDesugar** | `com.bidscube:applovin-bidscube-max-adapter-legacy-media-video:1.2.7` | VideoView / MediaPlayer |
| **FullWithVideo** | `com.bidscube:applovin-bidscube-max-adapter-full-video:1.2.7` | Media3 / Google IMA |

The first three modes should **not** require `coreLibraryDesugaring` in the host app. **FullWithVideo** may require it.

**MAX Dashboard adapter class** (all modes): `com.applovin.mediation.adapters.BidscubeMediationAdapter`

## Add the Adapter

**Option A — Maven (recommended)**

Add the AppLovin MAX SDK and **one** Bidscube adapter for your video mode:

**`app/build.gradle`** or **`app/build.gradle.kts`**:

```groovy
repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation 'com.applovin:applovin-sdk:13.0.0@aar'
    implementation 'com.bidscube:applovin-bidscube-max-adapter-full-video:1.2.7@aar'
}
```

**Option B — Local AAR**

Build from this repository:

```bash
export BidscubeVersion=1.2.7
export BidscubeAdapterVersion=1.2.7
./gradlew :applovin-adapter:stageReleaseAars -PskipSigning=true
# applovin-adapter/build/staged-aars/applovin-bidscube-max-adapter-*-1.2.7.aar
```

Copy the AAR for your mode into `app/libs/` and add:

```groovy
dependencies {
    implementation 'com.applovin:applovin-sdk:13.0.0@aar'
    implementation files('libs/applovin-bidscube-max-adapter-full-video-1.2.7.aar')
}
```

When using local AARs without Maven, also add the matching SDK AAR from `sdk/build/staged-aars/` unless you resolve it from Maven Central.

**Option C — Gradle module**

Copy the `applovin-adapter` and `sdk` modules from this repository into your project. Select the **same** `videoMode` flavor in both modules. Do **not** use `missingDimensionStrategy` to force `fullVideo`.

## AppLovin MAX Dashboard Setup

Use [AppLovin’s guide for custom SDK networks](https://support.axon.ai/en/max/mediated-network-guides/integrating-custom-sdk-networks/):

1. Open the **AppLovin MAX Dashboard** and select your app (bundle ID must match your **Bundle Identifier**).
2. Go to **MAX → Mediation → Manage → Networks**.
3. Click **Click here to add a Custom Network** and fill in:
   - **Network Type**: **SDK**
   - **Name**: `Bidscube`
   - **Android Adapter Class Name**: `com.applovin.mediation.adapters.BidscubeMediationAdapter`
4. Go to **MAX → Mediation → Manage → Ad Units**, select an ad unit, enable **Bidscube** and set the values for that placement.

## MAX Parameters

- **Android Adapter Class Name**: `com.applovin.mediation.adapters.BidscubeMediationAdapter`
- **`app_id`**: Bidscube init identifier used by the adapter during SDK initialization
- **Placement ID**: the Bidscube placement used for the specific MAX ad unit request
- **`request_authority`** (optional, **Server Parameters**): value passed to `SDKConfig.Builder.adRequestAuthority(...)`. Use **host** only (`my.trycloudflare.com`), **`host:port`** (`127.0.0.1:8787`), or a pasted prefix **`https://host/`** (scheme/path/query stripped by the SDK). If omitted, the SDK default host is used.
- **`ssp_host`** (optional): alias for `request_authority` if `request_authority` is empty

The adapter reads `app_id` from **Server Parameters** and the ad-specific value from the MAX **Placement ID** field.

## Adapter behavior (1.2.7)

| MAX API | Bidscube SDK call | Reward |
|---------|-------------------|--------|
| Interstitial show | `showInterstitialVideoAd` or `showImageAd` | Never |
| Rewarded show | `showRewardedVideoAd` | Only on `onUserRewarded` |

Show failures are reported as MAX **display failed**, not load failed.

## Consent (GDPR/CCPA)

Run the consent flow before initializing the AppLovin SDK and loading ads. Bidscube uses consent; without it, ads may not serve correctly.

## Supported Ad Formats

Banner, MREC, Interstitial, Rewarded, Native.

## Troubleshooting

- If the network initializes but ads do not load, verify both **`app_id`** and the MAX **Placement ID**.
- If MAX does not recognize the custom network, verify the Android adapter class name is `com.applovin.mediation.adapters.BidscubeMediationAdapter`.
- Run consent before initializing AppLovin MAX and loading ads.
- For **LiteNoVideo**, rewarded and interstitial video requests fail gracefully (unsupported / no fill) — use a video-capable adapter artifact if you need video.
- If reward never fires, confirm you are on **1.2.7+** — reward is only forwarded from `onUserRewarded`.
- **Show / request failures:** see [Error codes](../docs/errors.md) — e.g. code **204** = SSP no fill, **1004** = missing Activity (fixed in 1.2.7+ with official adapter).

## Release

- **Artifacts:** `com.bidscube:applovin-bidscube-max-adapter-*` (four variants at **1.2.7**)
- **Version source:** `BidscubeAdapterVersion` env var, default `1.2.7`
- **Full maintainer guide:** [RELEASE.md](../RELEASE.md)

```bash
export BidscubeVersion=1.2.7
export BidscubeAdapterVersion=1.2.7
./gradlew clean stageAllReleaseAars -PskipSigning=true
```

For Maven publishing, provide `mavenCentralUsername` and `mavenCentralPassword`; signing uses GPG (`useGpgCmd()`).
