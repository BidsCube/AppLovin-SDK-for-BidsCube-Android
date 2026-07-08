# Changelog

All notable changes to the Bidscube Android SDK and AppLovin MAX adapter are documented here.

## [Unreleased]

---

## [1.2.10] - 2026-07-08

### Added

- AppLovin MAX adapter: **preload on load** for interstitial and rewarded; show uses cached creative (`INVALID_LOAD_STATE` when cache missing).
- AppLovin MAX adapter: structured **`BidscubeSDK.collectSignal()`** (replaces test placeholder).
- SDK: `preloadInterstitialVideoAd`, `preloadRewardedVideoAd`, `preloadImageAd`, video ad cache helpers.
- SDK: consent helpers (`isConsentRequired`, `hasAdsConsent`, `hasAnalyticsConsent`) and `InitializationCallback` for async init completion.
- Release CI: tag-ref guard and hard-fail Android SDK platform 36 install in `publish.yml`.
- Test app wired into SDK repo via `:bidscube-testapp-android` Gradle module.

### Changed

- AppLovin MAX adapter: **Native MAX removed** for this release (use standalone SDK for native).
- AppLovin MAX adapter: async SDK init with listener queue; banner click forwarded to MAX.
- Docs: explicit **OpenRTB 2.6 not implemented**; FullWithVideo described as Google IMA-based VAST playback.

### Fixed

- Interstitial/rewarded MAX flow: load performs network fetch; show no longer reports loaded without cached creative.

---

## [1.2.9] - 2026-06-15

### Added

- VAST **companion preview** on post-video end card (`Companion` → `StaticResource`); existing fallback UI when preview is absent.
- `VideoSkipControlOverlay` — skip countdown from VAST `skipoffset` (`Skip in N` → `Skip`).
- `VideoEndCardOverlay` — companion image + click-through; fallback “Ad ended” + optional linear CTA.
- `VastParser.getCompanionImageUrl`, `getCompanionClickThroughUrl`, `getSkipOffsetMs`.
- `BidscubeSDK.showVideoAdFromVastMarkup` — inline VAST for QA (no SSP).
- `com.bidscube.sdk.qa.QaVastFixtures` — hardcoded no-preview / with-preview VAST payloads.
- Publisher test app: **VAST end card QA** buttons wired to the SDK path.

### Fixed

- AppLovin MAX adapter now passes MAX **`app_id`** server parameter into `SDKConfig.Builder.appId(...)`.

---

## [1.2.8] - 2026-06-04

### Added

- `ProgressiveMp4VastVideoPlayer` — plays inline VAST with progressive `MediaFile` (MP4) via `VideoView` without requiring Google IMA on the host app classpath.
- `AdmSanitizer` — unwraps Bidscube JSON/`document.write` HTML envelopes so image banners render real creative instead of raw `{ "adm":` text.
- `VideoSkipCloseOverlay` — video close control with countdown (default **15** s, or VAST `skipoffset`) then small semi-transparent ✕.

### Fixed

- **Video (fullVideo):** `Failed resolution of: VideoAdPlayer` when the host app does not bundle Google IMA — progressive MP4 VAST (e.g. Remerge/DoorDash creatives) now uses `ProgressiveMp4VastVideoPlayer` first.
- **Image/banner:** malformed `adm` parsing (`admLen=8`), broken images, and narrow banner width in WebView (`BannerViewFactory`, `BidscubeResponseParser`).
- **Video embed:** `getVideoAdView` uses `MATCH_PARENT` when effective position is `FULL_SCREEN`.

### Changed

- `fullVideo` `DefaultVastVideoPlayerProvider` prefers progressive MP4 when VAST contains a `MediaFile` URL; falls back to IMA only when IMA is on the classpath and inline media is absent.

---

## [1.2.7] - 2026-06-04

### Added

- Structured ad error codes in `com.bidscube.sdk.errors.AdErrorCode` (delivered via `AdCallback.onAdFailed`).
- `BidscubeSDK.setDisplayActivity(Activity)` for MAX / Application-context integrations.
- [docs/errors.md](docs/errors.md) — error code reference for integrators and support.

### Fixed

- **Crash:** `ClassCastException` (`Application` cannot be cast to `Activity`) when an ad HTTP request failed after MAX init with Application context.
- **Crash:** unhandled failures on background HTTP threads no longer terminate the process; errors are reported through `onAdFailed` on the main thread.
- HTTP **204 No Content** mapped to `AdErrorCode.NO_FILL` (204) with a clear English message instead of a generic HTTP error.

### Changed

- `HttpProvider` returns `BidscubeRequestException` with stable codes for HTTP, parse, and network failures.
- `AdDisplayManager` uses safe main-thread dispatch (`Activity` or main `Handler`) for all success/error UI paths.
- AppLovin MAX adapter binds the show `Activity` via `BidscubeSDK.setDisplayActivity` before interstitial, rewarded, banner, and native calls.

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
