# Custom Ad Rendering Guide

**Bidscube SDK 1.2.8+**

This guide shows how to intercept Bidscube SDK rendering and plug in your own UI using the `onAdRenderOverride` callback.

## Prerequisites

- Bidscube SDK **1.1.0+** added `AdCallback#onAdRenderOverride(AdRenderContext)` (available in all four [AAR modes](../README.md#android-aar-modes)).
- You already load ads via `BidscubeSDK` and register an `AdCallback` implementation.
- Your layout exposes a container (`ViewGroup`) where the custom ad view will be inserted.

## High-level flow

1. Call any of the public Bidscube entry points (e.g. `BidscubeSDK.showImageAd`, `showInterstitialVideoAd`, `showRewardedVideoAd`, `showNativeAd`, or the `get*AdView` helpers) with your `AdCallback` implementation. All of these eventually forward to `AdDisplayManager` inside the SDK.
2. After the network layer (`BidscubeResponse`) resolves and **before** the SDK inflates its UI, `AdDisplayManager` runs `handleRenderOverride`. This happens on the main thread via `Activity.runOnUiThread`.
3. `handleRenderOverride` only fires when both the callback and the ADM payload are non-null. It instantiates an `AdRenderContext` with `placementId`, the raw ADM, the negotiated `AdPosition`, and the planned `AdRenderType`.
4. If your callback returns `true`, `AdDisplayManager` logs "Render override accepted" and aborts its internal renderer. Returning `false` (default) lets the SDK finish displaying the asset.
5. Because the SDK skips its UI once you return `true`, you become responsible for creating/destroying the ad view, showing close controls, and firing any measurement beacons exposed through other SDK APIs.

## Inspecting `AdRenderContext`

`AdRenderContext` mirrors exactly what `AdDisplayManager#handleRenderOverride` receives:

- `getPlacementId()` matches the ID passed to `BidscubeSDK.show*`.
- `getAdm()` is the decoded response body; the SDK only enters override mode when this string is non-empty.
- `getPosition()` equals the effective `AdPosition` the SDK planned to use (response position overrides manual overrides).
- `getRenderType()` corresponds to the renderer (`AdRenderType.IMAGE`, `VIDEO`, `NATIVE`) the SDK was about to inflate. Use this to branch logic early—return `false` when you prefer the stock renderer.

## Sample implementation

```java
public final class CustomAdCallback implements AdCallback {
    private final ViewGroup placeholder;

    public CustomAdCallback(ViewGroup placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public boolean onAdRenderOverride(AdRenderContext context) {
        if (context.getRenderType() != AdRenderType.NATIVE) {
            return false; // let SDK handle banners/video
        }

        placeholder.removeAllViews();
        CustomNativeView nativeView = new CustomNativeView(placeholder.getContext());
        nativeView.bindAdmPayload(context.getAdm(), context.getPlacementId());
        placeholder.addView(nativeView);
        return true; // signal that we rendered everything
    }

    // other callbacks (loading, clicks, etc.) remain the same
    @Override
    public void onAdLoaded(String placementId) { /* track events */ }
}
```

Call site example:

```java
BidscubeSDK.showNativeAd("native_sample", new CustomAdCallback(nativeSlot));
```

When `BidscubeSDK.showNativeAd` reaches `AdDisplayManager`, it attempts to render a dialog. Our callback intercepts the ADM, renders into `nativeSlot`, and the SDK logs `Render override accepted` without spawning its default UI.

## Rendering checklist

- Parse the ADM payload according to your ad format (JSON native assets, HTML markup, VAST, etc.). Keep parsing synchronous or jump back to the UI thread before mutating views since the callback already runs on the main thread.
- Fire impressions/clicks using any Bidscube helper APIs or the trackers embedded in the ADM so analytics remains correct; the SDK will not auto-fire once you override rendering.
- Respect `AdPosition`: the SDK already calculated optimal placement. Mirror it in your layout or document why you diverge.
- Manage lifecycle parity—dismiss dialogs, stop video, and resume audio the same way the stock renderer would so other callbacks (`onAdClosed`, `onVideoAdCompleted`, `onUserRewarded`, etc.) still make sense.

## Video modes and override

Custom render override works across all four SDK AAR modes. Video playback backend differs by mode:

| Mode | Stock video renderer |
|------|----------------------|
| LiteNoVideo | No video — override rarely applies to video |
| WebViewVideoNoDesugar | WebView / HTML5 |
| LegacyMediaVideoNoDesugar | `VideoView` / `MediaPlayer` |
| FullWithVideo | Media3 / Google IMA |

Returning `false` from `onAdRenderOverride` delegates to the mode-appropriate stock renderer.

## Troubleshooting

- `onAdRenderOverride` is skipped when ADM is `null`/empty; log incoming context to confirm a valid creative.
- Throwing inside the callback is caught and logged by `AdDisplayManager`, then the SDK falls back to its renderer. Watch Logcat for `Render override handler threw` to diagnose issues.
- When you need a fallback, simply return `false` to delegate back to the SDK without tearing down your custom views.
