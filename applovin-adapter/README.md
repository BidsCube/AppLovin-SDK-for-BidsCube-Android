# Bidscube + AppLovin MAX Integration

AppLovin MAX mediation adapter for Bidscube SDK. Use Bidscube as a custom network in AppLovin MAX.

## Requirements

- **Android** minSdk 23+
- **AppLovin MAX SDK** 13.6.0+
- **Bidscube SDK** 1.0.2+
- **Google UMP SDK** 4.0.0+
- AppLovin **SDK Key** and **Ad Units**
- Bidscube **Application ID (`app_id`)**

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

1. Open the **AppLovin MAX Dashboard**.
2. Select your application (bundle ID must match your app’s **Bundle Identifier**).
3. Go to **Mediation → Manage Mediation**.
4. Add a **Custom network** named **Bidscube**.
5. Set the required server parameter:
   - **`app_id`** = your Bidscube Application ID
6. *(Optional)* For native ad units, set local parameter:
   - **`is_native`** = `true`
7. Enable **Bidscube** for the MAX ad units where you want Bidscube demand.

## Consent (GDPR/CCPA)

Run the Google UMP consent flow **before** initializing the AppLovin SDK and loading ads. Bidscube uses consent; without it, ads may not serve correctly.

## Supported Ad Formats

Banner, Interstitial, Rewarded, Native, MREC.

## Releasing via GitHub

**Version:** Adapter version is **1.0.0** (see `applovin-adapter/build.gradle.kts`; override with env `BidscubeAdapterVersion`).

**Artifact:** `com.bidscube:applovin-bidscube-adapter:1.0.0`

**To release:**

1. **Tag the repo** (e.g. for the adapter release):
   ```bash
   git tag applovin-adapter-v1.0.0
   git push origin applovin-adapter-v1.0.0
   ```
2. **GitHub Actions:** The workflow on tag `applovin-adapter-v*` builds the adapter AAR and uploads it to the GitHub Release. Optionally configure Maven Central credentials in repo secrets to publish to a Maven repository.
3. **Local publish:** To publish from your machine:
   ```bash
   export BidscubeAdapterVersion=1.0.0
   ./gradlew :applovin-adapter:assembleRelease :applovin-adapter:publishReleasePublicationToCentralRepository
   ```
   Set `mavenCentralUsername` and `mavenCentralPassword` in `gradle.properties` or environment; signing uses GPG (e.g. `useGpgCmd()` in the build).
