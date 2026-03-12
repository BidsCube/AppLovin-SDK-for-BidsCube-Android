# Bidscube + AppLovin MAX Integration

AppLovin MAX mediation adapter for Bidscube SDK. Use Bidscube as a custom network in AppLovin MAX.

## Requirements

- **Android** minSdk 24+
- **AppLovin MAX SDK** 13.6.0+
- **Bidscube adapter** 1.0.0+ with Bidscube SDK pulled transitively
- AppLovin **SDK Key** and **Ad Units**
- Bidscube init value **`app_id`** and a MAX **Placement ID** per ad unit

## Add the Adapter

**Option A — Maven (recommended)**

Add the AppLovin MAX SDK and the Bidscube adapter. The adapter brings in the Bidscube SDK automatically.

**`app/build.gradle`** or **`app/build.gradle.kts`**:

```groovy
repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation 'com.applovin:applovin-sdk:13.6.0@aar'
    implementation 'com.bidscube:applovin-bidscube-adapter:1.0.0@aar'
}
```

**Option B — Copy the adapter module**

Copy the `applovin-adapter` module from this repository (or the `Bidscube` folder from the AppLovin MAX SDK repo) into your project, then include it in **`settings.gradle`**:

```groovy
include ':app'
include ':Bidscube'
```

If the adapter folder is not at the project root, set the path:

```groovy
include ':Bidscube'
project(':Bidscube').projectDir = new File('path/to/Bidscube')
```

Add dependencies in **`app/build.gradle`**:

```groovy
dependencies {
    implementation 'com.applovin:applovin-sdk:13.6.0@aar'
    implementation 'androidx.browser:browser:1.6.0'
    implementation project(':Bidscube')
}
```

Sync the project and build.

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
- **Custom Parameters**: not used by the current adapter implementation

The adapter reads `app_id` from **Server Parameters** and the ad-specific value from the MAX **Placement ID** field.

## Consent (GDPR/CCPA)

Run the consent flow before initializing the AppLovin SDK and loading ads. Bidscube uses consent; without it, ads may not serve correctly.

## Supported Ad Formats

Banner, MREC, Interstitial, Rewarded, Native.

## Troubleshooting

- If the network initializes but ads do not load, verify both **`app_id`** and the MAX **Placement ID**.
- If MAX does not recognize the custom network, verify the Android adapter class name is `com.applovin.mediation.adapters.BidscubeMediationAdapter`.
- Run consent before initializing AppLovin MAX and loading ads.

## Release

- **Artifact:** `com.bidscube:applovin-bidscube-adapter:1.0.0`
- **Version source:** `BidscubeAdapterVersion` env var, default `1.0.0`
- **GitHub release:** push a tag such as `applovin-adapter-v1.0.0`

```bash
git tag applovin-adapter-v1.0.0
git push origin applovin-adapter-v1.0.0
```

- **Local publish:**

```bash
export BidscubeAdapterVersion=1.0.0
./gradlew :applovin-adapter:assembleRelease :applovin-adapter:publishReleasePublicationToCentralRepository
```

For Maven publishing, provide `mavenCentralUsername` and `mavenCentralPassword`; signing uses GPG (`useGpgCmd()`).
