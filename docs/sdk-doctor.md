# Bidscube SDK Doctor

SDK Doctor is a two-layer diagnostic system for **AppLovin-SDK-for-BidsCube-Android**. It helps catch integration and release problems **before publishing** and **during app QA**.

SDK Doctor **does not**:

- Send reports to Bidscube
- Collect PII (no advertising ID, no user identifiers)
- Replace real ad QA or MAX dashboard validation
- Parse OpenRTB bid responses unless the native parser is implemented in this build

---

## What it checks

| Layer | When to run | What it validates |
|-------|-------------|-------------------|
| **Build/Release Doctor** | CI, pre-release, archive QA | Repo hygiene, version consistency, Gradle/POM wiring, adapter source, staged AAR names, forbidden strings, SDK↔adapter API compatibility (`javap`), OpenRTB doc honesty, GitHub workflow guards |
| **Runtime Doctor** | Test app, publisher debug builds | Manifest permissions, SDK init, MAX classes (optional), video mode classpath, preload APIs, consent summary, OpenRTB status |

Report statuses: **PASS**, **WARN**, **FAIL**, **INFO**. Critical **FAIL** checks exit with code `1`.

---

## Build/Release Doctor

### CLI

From the repository root:

```bash
python3 scripts/sdk_doctor.py --repo .
python3 scripts/sdk_doctor.py --repo . --staged-aars build/staged-aars
python3 scripts/sdk_doctor.py --repo . --json build/reports/sdk-doctor.json
python3 scripts/sdk_doctor.py --repo . --markdown build/reports/sdk-doctor.md
python3 scripts/sdk_doctor.py --repo . --strict
python3 scripts/sdk_doctor.py --archive ../AppLovin-SDK-for-BidsCube-Android-release.zip
python3 scripts/sdk_doctor.py --self-test
```

- **Source mode** (`--repo .`): structure, versions, Gradle deps, adapter source, workflows, docs.
- **Staged AAR mode** (`--staged-aars`): all source checks plus 8 AAR filenames, forbidden bytecode strings, and `javap` SDK method compatibility for every video flavor.
- **Archive mode** (`--archive`): inspects a release ZIP without extracting; fails on `.git/`, `build/`, `local.properties`, private keys, macOS junk, etc.

### Critical release failures

- Private GPG keys under `keys/`
- `local.properties` or build artifacts **inside a release archive**
- `bidscube_test_signal`, dummy native ad strings, or Native MAX stubs in adapter source/AARs
- Missing or misnamed staged AARs
- SDK AAR missing methods required by the adapter (`NoSuchMethodError` risk)
- Docs claiming OpenRTB 2.6 support without `sdk/src/main/java/com/bidscube/sdk/openrtb/` parser classes

### Four Android video modes

Doctor expects these artifact families (version parsed from Gradle, not hardcoded):

| SDK AAR | Adapter AAR |
|---------|-------------|
| `bidscube-sdk-lite-no-video-<version>.aar` | `applovin-bidscube-max-adapter-lite-no-video-<version>.aar` |
| `bidscube-sdk-webview-video-<version>.aar` | `applovin-bidscube-max-adapter-webview-video-<version>.aar` |
| `bidscube-sdk-legacy-media-video-<version>.aar` | `applovin-bidscube-max-adapter-legacy-media-video-<version>.aar` |
| `bidscube-sdk-full-video-<version>.aar` | `applovin-bidscube-max-adapter-full-video-<version>.aar` |

---

## Runtime Doctor

Call from a test or publisher app (no network, no PII):

```java
import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.doctor.SdkDoctorOptions;
import com.bidscube.sdk.doctor.SdkDoctorReport;

// Direct SDK integration
SdkDoctorReport report = BidscubeSDK.runSdkDoctor(context);
Log.d("BidscubeDoctor", report.toHumanReadableString());
Log.d("BidscubeDoctor", report.toJson());

// AppLovin MAX mediation — stricter class checks
SdkDoctorReport maxReport = BidscubeSDK.runSdkDoctor(
        context,
        SdkDoctorOptions.maxMediation()
);
```

Runtime checks include:

- Android API level, SDK version, optional adapter version
- `INTERNET` permission (FAIL if missing)
- `BidscubeSDK.isInitialized()` and display Activity binding
- Reflection for `AppLovinSdk`, `MaxAd`, `BidscubeMediationAdapter` (MAX mode: adapter missing = FAIL)
- Video mode detection (`liteNoVideo`, `webViewVideo`, `legacyMediaVideo`, `fullVideo`)
- Preload/cache API presence via reflection
- OpenRTB: reports **not implemented** unless parser classes exist
- Consent API summary when SDK is initialized

---

## Gradle tasks

```bash
./gradlew sdkDoctor
./gradlew clean sdkDoctorRelease -PskipSigning=true
```

- `sdkDoctor` — source checks only (`--strict`)
- `sdkDoctorRelease` — builds staged AARs, runs full release checks, writes `build/reports/sdk-doctor.json` and `sdk-doctor.md`

---

## CI integration

GitHub Actions workflows **Fast Release** and **Release AppLovin Bidscube Adapter** run:

```bash
./gradlew clean sdkDoctorRelease -PskipSigning=true --no-daemon --stacktrace
```

Reports are uploaded as workflow artifacts. Release fails if SDK Doctor reports any **FAIL** status.

---

## Common failures and fixes

| Failure | Fix |
|---------|-----|
| Missing staged AAR | Run `./gradlew clean sdkDoctorRelease -PskipSigning=true` |
| Forbidden string in adapter | Remove test/native placeholder strings from `BidscubeMediationAdapter.java` |
| SDK method missing in AAR | Rebuild SDK and adapter with matching `BidscubeVersion` / `BidscubeAdapterVersion` |
| OpenRTB doc mismatch | Update README/RELEASE to say OpenRTB is not implemented, or add parser under `sdk/.../openrtb/` |
| `local.properties` in release ZIP | Regenerate archive excluding local paths |
| Runtime: SDK not initialized | Call `BidscubeSDK.initialize(context, config)` before ads |
| Runtime: MAX adapter missing | Add one `applovin-bidscube-max-adapter-*` artifact for your video mode |

---

## What SDK Doctor does not do

- Validate MAX Dashboard network configuration or live bidding
- Guarantee fill rate or creative quality
- Verify Gradle desugaring configuration (runtime may WARN for full video)
- Perform network reachability or SSP endpoint tests (unless you add that separately)

For mediation setup, see [AppLovin MAX adapter README](../applovin-adapter/README.md) and [docs/mediation/](mediation/).
