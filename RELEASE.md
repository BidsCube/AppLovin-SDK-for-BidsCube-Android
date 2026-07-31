# Releasing Bidscube Android SDK + AppLovin MAX adapter

## Version sources

| Item | Source | Example (1.2.12) |
|------|--------|-----------------|
| **SDK Maven version** | `BidscubeVersion` env or `sdk/build.gradle.kts` default | `1.2.12` |
| **Adapter Maven version** | `BidscubeAdapterVersion` env or `applovin-adapter/build.gradle.kts` default | `1.2.12` |
| **Runtime SDK version** | `BuildConfig.SDK_VERSION_NAME` / `BidscubeSDK.getSdkVersion()` | `1.2.12` |
| **Runtime adapter version** | `BidscubeMediationAdapter.getAdapterVersion()` fallback string | `1.2.12` |
| **Git tag (full stack)** | `v` + version | `v1.2.12` |
| **Git tag (adapter only)** | `applovin-adapter-v` + version | `applovin-adapter-v1.2.12` |

Keep SDK and adapter versions aligned unless you intentionally ship a hotfix for one artifact only.

## Pre-release checklist

- [ ] Source archive created via `git archive` (no `.git/`, `build/`, `local.properties`, keys)
- [ ] `publish.yml` cannot run release with empty version (tag ref required for manual runs)
- [ ] Android SDK platform 36 install fails hard in CI if unavailable
- [ ] `CHANGELOG.md` includes an entry for this version.
- [ ] `README.md`, `applovin-adapter/README.md`, `docs/guide.md`, and `docs/errors.md` reference the new version.
- [ ] README does **not** claim OpenRTB 2.6 podded video support
- [ ] README **FullWithVideo** wording matches Google IMA-based playback (not “Media3 primary”)
- [ ] Default versions in `sdk/build.gradle.kts`, `applovin-adapter/build.gradle.kts`, and `BidscubeMediationAdapter.java` match the release.
- [ ] `./gradlew clean stageAllReleaseAars -PskipSigning=true` succeeds.
- [ ] **8** AAR files exist under `build/staged-aars/`:
  - `bidscube-sdk-lite-no-video-{sdkVersion}.aar`
  - `bidscube-sdk-webview-video-{sdkVersion}.aar`
  - `bidscube-sdk-legacy-media-video-{sdkVersion}.aar`
  - `bidscube-sdk-full-video-{sdkVersion}.aar`
  - `applovin-bidscube-max-adapter-lite-no-video-{adapterVersion}.aar`
  - `applovin-bidscube-max-adapter-webview-video-{adapterVersion}.aar`
  - `applovin-bidscube-max-adapter-legacy-media-video-{adapterVersion}.aar`
  - `applovin-bidscube-max-adapter-full-video-{adapterVersion}.aar`
- [ ] `liteNoVideo` / `webViewVideo` / `legacyMediaVideo` SDK variants have **no** Media3 / IMA on compile classpath.
- [ ] `fullVideo` SDK variant includes Media3 / IMA and `IMAPlayerHandler`.
- [ ] Adapter POMs: `liteNoVideoRelease` → `sdk-lite-no-video`, `fullVideoRelease` → `sdk-full-video`, etc.
- [ ] MAX Dashboard class unchanged: `com.applovin.mediation.adapters.BidscubeMediationAdapter`.
- [ ] No `missingDimensionStrategy("videoMode", "fullVideo")` in the adapter module.
- [ ] Smoke test passed: banner load/display/click; interstitial preload+show; rewarded preload+show+single reward; LiteNoVideo no video; FullWithVideo IMA VAST plays

## Clean archive rules

Do **not** include in release archives:

- `.git/`, `.gradle/`, `build/`, `Pods/`, `DerivedData/`, `local.properties`, private keys, `__MACOSX/`, `.DS_Store`

## Build release AARs locally

```bash
export BidscubeVersion=1.2.12
export BidscubeAdapterVersion=1.2.12

./gradlew clean stageAllReleaseAars -PskipSigning=true --no-daemon
ls -la build/staged-aars/
```

## Publish to Maven Central

Requires `mavenCentralUsername`, `mavenCentralPassword`, and GPG signing (`useGpgCmd()`). Use `-PskipSigning=true` only for local dry runs.

```bash
export BidscubeVersion=1.2.12
export BidscubeAdapterVersion=1.2.12

./gradlew \
  :sdk:publishLiteNoVideoReleasePublicationToCentralRepository \
  :sdk:publishWebViewVideoReleasePublicationToCentralRepository \
  :sdk:publishLegacyMediaVideoReleasePublicationToCentralRepository \
  :sdk:publishFullVideoReleasePublicationToCentralRepository \
  :applovin-adapter:publishLiteNoVideoReleasePublicationToCentralRepository \
  :applovin-adapter:publishWebViewVideoReleasePublicationToCentralRepository \
  :applovin-adapter:publishLegacyMediaVideoReleasePublicationToCentralRepository \
  :applovin-adapter:publishFullVideoReleasePublicationToCentralRepository \
  --no-daemon
```

Publish SDK variants first, then adapter variants.

## Create a GitHub Release

### Full stack (`v*` tag)

Triggers `.github/workflows/publish.yml`:

```bash
git tag -a v1.2.12 -m "Bidscube Android SDK + AppLovin MAX adapter 1.2.12"
git push origin v1.2.12
```

Uploads all **8** staged AAR files from `build/staged-aars/`.

### Adapter-only (`applovin-adapter-v*` tag)

Triggers `.github/workflows/release-applovin-adapter.yml`:

```bash
git tag -a applovin-adapter-v1.2.12 -m "AppLovin Bidscube MAX adapter 1.2.12"
git push origin applovin-adapter-v1.2.12
```

## Maven coordinates (1.2.12)

**AppLovin MAX** — pick one adapter (SDK is transitive):

```kotlin
implementation("com.bidscube:applovin-bidscube-max-adapter-full-video:1.2.12@aar")
```

**Standalone SDK** — pick one core artifact:

```kotlin
implementation("com.bidscube:sdk-full-video:1.2.12@aar")
```

See [README.md](README.md#android-aar-modes) for all four modes.

## Unity parity

This release aligns with the Unity AppLovin MAX package bundled native AARs at **1.2.12** (adapter) and core SDK **1.2.5+** four-mode export. See [AppLovin-SDK-for-BidsCube-Unity](https://github.com/BidsCube/AppLovin-SDK-for-BidsCube-Unity).
