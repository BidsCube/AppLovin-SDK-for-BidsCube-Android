# Changelog

All notable changes to the Bidscube Android SDK and AppLovin MAX adapter are documented here.

## [Unreleased]

---

## [1.2.6] - 2026-06-02

### Added

- **Four Android AAR modes** for both the core SDK and the AppLovin MAX adapter:
  - **LiteNoVideo** — image / banner / native only; no video stack
  - **WebViewVideoNoDesugar** — HTML5 video via WebView
  - **LegacyMediaVideoNoDesugar** — legacy `VideoView` / `MediaPlayer` path
  - **FullWithVideo** — Media3 + Google IMA
- Maven artifacts:
  - SDK: `com.bidscube:sdk-lite-no-video`, `sdk-webview-video`, `sdk-legacy-media-video`, `sdk-full-video`
  - Adapter: `com.bidscube:applovin-bidscube-max-adapter-lite-no-video`, `…-webview-video`, `…-legacy-media-video`, `…-full-video`
- Gradle tasks: `:sdk:stageReleaseAars`, `:applovin-adapter:stageReleaseAars`, `stageAllReleaseAars` (exports 8 versioned AAR files).
- Flavor-specific SDK source sets and video player providers per mode.
- Public SDK APIs: `showInterstitialVideoAd`, `showRewardedVideoAd`, `onUserRewarded` callback.

### Changed

- AppLovin MAX adapter now builds with matching `videoMode` flavors instead of a single release AAR forced to `fullVideo`.
- Each adapter publication POM depends on the **matching** SDK artifact (not all on `sdk-full-video`).
- MAX interstitial uses `showInterstitialVideoAd` (video) or `showImageAd` (static); MAX rewarded uses `showRewardedVideoAd`.
- Reward is forwarded to MAX only from `onUserRewarded` (not from close / skip / `onVideoAdCompleted`).
- Interstitial / rewarded show failures use MAX **display failed** callbacks.
- Documentation rewritten for multi-mode Maven coordinates and release checklist.

### Deprecated

- Single-artifact coordinates `com.bidscube:bidscube-sdk` and `com.bidscube:applovin-bidscube-adapter` — use the mode-specific artifacts above.

### Fixed

- `liteNoVideo` / `webViewVideo` / `legacyMediaVideo` builds no longer pull Media3 or Google IMA transitively.
- No `ClassNotFoundException` in lite mode when video APIs are called — SDK returns unsupported / no-fill errors instead.

---

## [1.0.3.1] - prior release

- AppLovin MAX adapter built against AppLovin SDK 13.0.x.
- Single `release` AAR for SDK and adapter.
