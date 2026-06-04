package com.bidscube.sdk;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bidscube.sdk.R;
import com.bidscube.sdk.ads.AdType;
import com.bidscube.sdk.ads.VideoAdFormat;
import com.bidscube.sdk.ads.VideoAdType;
import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.models.AdRenderContext;
import com.bidscube.sdk.models.enums.AdPosition;
import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.errors.AdErrorCode;
import com.bidscube.sdk.errors.BidscubeRequestException;
import com.bidscube.sdk.httpProvider.HttpProvider;

import com.bidscube.sdk.models.DeviceInfo;
import com.bidscube.sdk.stats.SdkStatsReporter;
import com.bidscube.sdk.models.natives.NativeAd;
import com.bidscube.sdk.network.BidscubeCallback;
import com.bidscube.sdk.network.BidscubeResponse;
import com.bidscube.sdk.network.NativeAdParser;
import com.bidscube.sdk.utils.AdmSanitizer;
import com.bidscube.sdk.utils.VastParser;
import com.bidscube.sdk.utils.SDKLogger;
import com.bidscube.sdk.video.BidscubeVastVideoPlayer;
import com.bidscube.sdk.video.DefaultVastVideoPlayerProvider;
import com.bidscube.sdk.video.BidscubeVastVideoPlayerFactory;
import com.bidscube.sdk.view.BannerViewFactory;
import com.bidscube.sdk.view.NativeAdView;
import com.bidscube.sdk.view.NativeAdBinder;
import com.bidscube.sdk.view.VideoSkipCloseOverlay;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the display of different ad types in both full screen and windowed
 * modes
 * <p>
 * Windowed Ad Positioning Behavior:
 * - When a specific position is selected from the spinner (ABOVE_THE_FOLD,
 * BELOW_THE_FOLD,
 * HEADER, FOOTER, SIDEBAR), the ad will be positioned accordingly
 * - When "Unknown" or nothing is selected, ads will display without any
 * alignment or
 * position regulation (natural display)
 * <p>
 * Supported Ad Types:
 * - Image Ads: createImageAdViewWithCloseButton()
 * - Video Ads: showVideoAdWindowedInternal()
 * - Native Ads: showNativeAdWindowed()
 */
public class AdDisplayManager {

    private static final String TAG = "AdDisplayManager";
    private static final String INTEGRATION = "BidscubeIntegration";
    private final Context context;
    private final DeviceInfo deviceInfo;
    private final SDKConfig sdkConfig;

    private WebView currentBanner = null;
    private BidscubeVastVideoPlayer currentVideoPlayer = null;
    private NativeAdView currentNativeAd = null;

    private ViewGroup overlayContainer;

    private AdPosition currentAdPosition = AdPosition.UNKNOWN;
    private AdPosition responseAdPosition = AdPosition.UNKNOWN;
    private volatile Activity displayActivity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AdDisplayManager(Context context, DeviceInfo deviceInfo, SDKConfig sdkConfig) {
        this.context = context;
        this.deviceInfo = deviceInfo;
        this.sdkConfig = sdkConfig;
    }

    private void reportAdStatFail(String placementId, String format, String message) {
        SdkStatsReporter.reportAdFailure(sdkConfig, placementId, format, message);
    }

    private VideoSkipCloseOverlay attachVideoSkipCloseOverlay(
            FrameLayout frameContainer,
            String vastXml,
            BidscubeVastVideoPlayer videoPlayer,
            String placementId,
            AdCallback callback,
            AtomicBoolean completed,
            AtomicBoolean skipped,
            AtomicBoolean closed,
            Dialog dialog) {
        VideoSkipCloseOverlay overlay = new VideoSkipCloseOverlay(context, vastXml, () -> {
            try {
                if (!completed.get()) {
                    videoPlayer.skipVideo();
                }
            } catch (Throwable ignored) {
                fireVideoAdSkipped(placementId, callback, completed, skipped);
            }
            try {
                videoPlayer.release();
            } catch (Throwable ignored) {
            }
            if (currentVideoPlayer == videoPlayer) {
                currentVideoPlayer = null;
            }
            dialog.dismiss();
            fireAdClosedOnce(placementId, callback, closed);
        });
        overlay.attach(frameContainer);
        dialog.setOnDismissListener(d -> overlay.destroy());
        return overlay;
    }

    private BidscubeVastVideoPlayer createVastVideoPlayer(String adm, String vastRedirectUrl) {
        BidscubeVastVideoPlayerFactory factory = sdkConfig.getVastVideoPlayerFactory();
        if (factory != null) {
            try {
                BidscubeVastVideoPlayer custom = factory.create(context, adm, vastRedirectUrl);
                if (custom != null) {
                    Log.i(INTEGRATION, "VAST player: custom factory -> " + custom.getClass().getName());
                    return custom;
                }
                SDKLogger.w(TAG, "vastVideoPlayerFactory returned null; using default IMA player");
                Log.w(INTEGRATION, "VAST player: factory returned null; falling back to IMAPlayerHandler");
            } catch (Throwable t) {
                SDKLogger.e(TAG, "vastVideoPlayerFactory failed; using default IMA player: " + t.getMessage(), t);
                Log.e(INTEGRATION, "VAST player: factory threw; falling back to IMAPlayerHandler", t);
            }
        }
        Log.i(INTEGRATION, "VAST player: built-in flavor-specific provider");
        return DefaultVastVideoPlayerProvider.create(context, adm, vastRedirectUrl);
    }

    private static void fireAdLoadedAndDisplayed(String placementId, AdCallback callback) {
        if (callback == null || placementId == null) {
            return;
        }
        callback.onAdLoaded(placementId);
        callback.onAdDisplayed(placementId);
    }

    private static void fireVideoAdUiReady(String placementId, AdCallback callback) {
        if (callback == null || placementId == null) {
            return;
        }
        callback.onAdLoaded(placementId);
        callback.onAdDisplayed(placementId);
        callback.onVideoAdStarted(placementId);
    }

    private static void fireVideoAdCompleted(String placementId, AdCallback callback, AtomicBoolean completed,
            AtomicBoolean skipped, VideoAdFormat format) {
        if (callback == null || placementId == null || skipped.get()) {
            return;
        }
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        try {
            callback.onVideoAdCompleted(placementId);
            if (format == VideoAdFormat.REWARDED) {
                callback.onUserRewarded(placementId);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void fireVideoAdSkipped(String placementId, AdCallback callback, AtomicBoolean completed,
            AtomicBoolean skipped) {
        if (callback == null || placementId == null || completed.get()) {
            return;
        }
        if (!skipped.compareAndSet(false, true)) {
            return;
        }
        try {
            callback.onVideoAdSkipped(placementId);
        } catch (Throwable ignored) {
        }
    }

    private static void fireAdClosed(String placementId, AdCallback callback) {
        if (callback == null || placementId == null) {
            return;
        }
        try {
            callback.onAdClosed(placementId);
        } catch (Throwable ignored) {
        }
    }

    private static void fireAdClosedOnce(String placementId, AdCallback callback, AtomicBoolean closed) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        fireAdClosed(placementId, callback);
    }

    private static void attachVideoLifecycleCallbacks(BidscubeVastVideoPlayer videoPlayer, String placementId,
            AdCallback callback, AtomicBoolean completed, AtomicBoolean skipped, VideoAdFormat format) {
        if (videoPlayer == null) {
            return;
        }
        videoPlayer.setOnVideoCompletionListener(new BidscubeVastVideoPlayer.OnVideoCompletionListener() {
            @Override
            public void onVideoCompleted() {
                fireVideoAdCompleted(placementId, callback, completed, skipped, format);
            }

            @Override
            public void onVideoSkipped() {
                fireVideoAdSkipped(placementId, callback, completed, skipped);
            }
        });
    }

    /** Corner close control for overlays / embedded SDK ad slots. */
    private ImageButton buildCloseAdControl(Runnable onDismiss, String placementId, AdCallback callback) {
        ImageButton btn = new ImageButton(context);
        btn.setImageResource(R.drawable.close_small_24);
        btn.setBackgroundColor(0xE0F44336);
        float density = context.getResources().getDisplayMetrics().density;
        int pad = (int) (8 * density + 0.5f);
        btn.setPadding(pad, pad, pad, pad);
        btn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        btn.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        btn.setContentDescription(context.getString(R.string.bidscube_cd_close_ad));
        btn.setElevation(10f * density);
        btn.setOnClickListener(v -> {
            try {
                if (onDismiss != null) {
                    onDismiss.run();
                }
            } catch (Throwable t) {
                SDKLogger.d(TAG, "ad close dismiss: " + t.getMessage());
            }
            fireAdClosed(placementId, callback);
        });
        return btn;
    }

    private static void mountCloseOnFrame(FrameLayout host, ImageButton close) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        int m = (int) (4 * host.getResources().getDisplayMetrics().density + 0.5f);
        lp.setMargins(m, m, m, m);
        host.addView(close, lp);
        close.bringToFront();
    }

    /**
     * Optional Activity supplied before show/load (e.g. from MAX adapter show callbacks).
     */
    void setDisplayActivity(Activity activity) {
        this.displayActivity = activity;
    }

    // Try to resolve an Activity from display override or the stored Context.
    private Activity resolveActivityContext() {
        Activity display = displayActivity;
        if (display != null && !display.isFinishing()) {
            return display;
        }
        if (context == null) {
            return null;
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return activity.isFinishing() ? null : activity;
        }
        android.content.Context ctx = context;
        while (ctx instanceof android.content.ContextWrapper) {
            if (ctx instanceof Activity) {
                Activity activity = (Activity) ctx;
                return activity.isFinishing() ? null : activity;
            }
            ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
            if (ctx == null) {
                break;
            }
        }
        return null;
    }

    private void runOnUiThread(Runnable action) {
        if (action == null) {
            return;
        }
        Activity activity = resolveActivityContext();
        if (activity != null) {
            activity.runOnUiThread(action);
            return;
        }
        mainHandler.post(action);
    }

    private static void invokeAdFailed(AdCallback callback, String placementId, int errorCode, String message) {
        if (callback == null) {
            return;
        }
        try {
            callback.onAdFailed(placementId, errorCode, message);
        } catch (Throwable callbackError) {
            SDKLogger.e(TAG, "AdCallback.onAdFailed threw: " + callbackError.getMessage(), callbackError);
        }
    }

    private void deliverAdFailure(String placementId, String format, AdCallback callback, Throwable error) {
        final int code = AdErrorCode.fromException(error);
        final String message = AdErrorCode.messageFor(error);
        SDKLogger.e(TAG, "Ad request failed (" + format + ") placement=" + placementId
                + " code=" + code + " (" + AdErrorCode.describe(code) + "): " + message, error);
        Log.e(INTEGRATION, format + " ad: request failed placement=" + placementId
                + " code=" + code + " error=" + message, error);
        reportAdStatFail(placementId, format, message);
        runOnUiThread(() -> invokeAdFailed(callback, placementId, code, message));
    }

    private Activity requireActivityForDialog(String placementId, String format, AdCallback callback) {
        Activity activity = resolveActivityContext();
        if (activity == null) {
            deliverAdFailure(placementId, format, callback, new BidscubeRequestException(
                    AdErrorCode.NO_ACTIVITY_CONTEXT,
                    "Activity context is required to display ads. Pass an Activity when showing ads "
                            + "(for example from the MAX adapter show callback)."));
            return null;
        }
        return activity;
    }

    /**
     * Sets the current ad position for windowed ads (manual override)
     */
    public void setAdPosition(AdPosition position) {
        this.currentAdPosition = position;
        SDKLogger.d(TAG, "Manual ad position set to: " + position);
    }

    /**
     * Sets the ad position from response
     */
    public void setResponseAdPosition(int responsePosition) {
        this.responseAdPosition = AdPosition.fromResponseValue(responsePosition);
        SDKLogger.d(TAG,
                "Response ad position set to: " + this.responseAdPosition + " (value: " + responsePosition + ")");
    }

    /**
     * Gets the effective ad position (response position takes precedence)
     */
    public AdPosition getEffectiveAdPosition() {
        if (responseAdPosition != AdPosition.UNKNOWN) {
            return responseAdPosition;
        }
        return currentAdPosition;
    }

    /**
     * Gets the current ad position (manual override)
     */
    public AdPosition getCurrentAdPosition() {
        return currentAdPosition;
    }

    /**
     * Gets the response ad position
     */
    public AdPosition getResponseAdPosition() {
        return responseAdPosition;
    }

    /**
     * Allows host app to intercept rendering if their callback opts-in.
     */
    private boolean handleRenderOverride(String placementId,
                                         String adm,
                                         AdPosition position,
                                         AdType.Type renderType,
                                         AdCallback callback) {
        if (callback == null || adm == null || adm.isEmpty()) {
            return false;
        }
        try {
            // sanitize ADM before passing to the host app so wrappers like document.write(...) are removed
            String cleanAdm = sanitizeAdm(adm);
            AdRenderContext context = new AdRenderContext(placementId, cleanAdm, position, renderType);
            boolean handled = callback.onAdRenderOverride(context);
            if (handled) {
                SDKLogger.d(TAG, "Render override accepted for placement " + placementId + " (" + renderType + ")");
            }
            return handled;
        } catch (Exception e) {
            SDKLogger.e(TAG, "Render override handler threw for placement " + placementId + ": " + e.getMessage());
            return false;
        }
    }

    private String sanitizeAdm(String adm) {
        return AdmSanitizer.sanitize(adm);
    }


    /**
     * Helper method to send HTTP requests and parse responses into BidscubeResponse
     * Reduces code duplication across ad display methods
     */
    private void sendAdRequest(String url, BidscubeCallback callback) {
        HttpProvider.sendGetRequest(url, callback);
    }

    /**
     * Checks if positioning should be applied based on current selection
     *
     * @return true if positioning should be applied, false if no regulation needed
     */
    public boolean shouldApplyPositioning() {
        AdPosition effectivePosition = getEffectiveAdPosition();
        return effectivePosition != AdPosition.UNKNOWN && effectivePosition != AdPosition.FULL_SCREEN;
    }

    /**
     * Gets a human-readable description of the current positioning behavior
     *
     * @return String describing the current positioning behavior
     */
    public String getPositioningDescription() {
        AdPosition effectivePosition = getEffectiveAdPosition();
        if (shouldApplyPositioning()) {
            return "Positioning applied for: " + effectivePosition + " (from " +
                    (responseAdPosition != AdPosition.UNKNOWN ? "response" : "manual") + ")";
        } else {
            return "No position regulation - natural display";
        }
    }

    /**
     * Centers a full screen dialog content
     */
    private void centerFullScreenDialog(Dialog dialog, LinearLayout container) {

        container.setGravity(Gravity.CENTER);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER);
        }

        SDKLogger.d(TAG, "Centered full screen dialog content");
    }

    /**
     * Centers a full screen dialog content (FrameLayout version)
     */
    private void centerFullScreenDialog(Dialog dialog, FrameLayout container) {

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER);
        }

        SDKLogger.d(TAG, "Centered full screen dialog content (FrameLayout)");
    }

    /**
     * Gets the display name for the current position
     */
    private String getPositionDisplayName() {
        AdPosition effectivePosition = getEffectiveAdPosition();
        switch (effectivePosition) {
            case ABOVE_THE_FOLD:
                return "Above the Fold";
            case MAYBE_DEPENDING_ON_SCREEN_SIZE:
                return "Maybe Depending on Screen Size";
            case BELOW_THE_FOLD:
                return "Below the Fold";
            case HEADER:
                return "Header";
            case FOOTER:
                return "Footer";
            case SIDEBAR:
                return "Sidebar";
            case FULL_SCREEN:
                return "Full Screen";
            case UNKNOWN:
            default:
                return "Center";
        }
    }


    /**
     * Show image ad with display mode determined by response position
     * This method respects the position value from the ad response
     */
    void showImageAdWithResponsePosition(String placementId, String url, AdCallback callback) {
        sendAdRequest(url, new BidscubeCallback() {
            @Override
            public void onSuccess(int responseCode, BidscubeResponse response) {
                runOnUiThread(() -> {
                    try {
                        setResponseAdPosition(response.getPosition());
                        AdPosition effectivePosition = getEffectiveAdPosition();

                        SDKLogger.d(TAG, "Image ad response position: " + response.getPosition() + " -> " + effectivePosition);

                        // Always render internally (no host render override). Sanitize ADM.
                        final String adm = sanitizeAdm(response.getAdm());
                        if (adm == null || adm.isEmpty()) {
                            SDKLogger.e(TAG, "Empty ADM for placement " + placementId);
                            reportAdStatFail(placementId, "image", "empty_adm");
                            if (callback != null) callback.onAdFailed(placementId, -1, "Empty ADM");
                            return;
                        }

                        // Clean up previous banner / overlay
                        try {
                            // Clear any attached native banners built via NativeAdBinder
                            try {
                                NativeAdBinder.clearAttachedBanner();
                            } catch (Throwable ignored) {
                            }

                            if (currentBanner != null) {
                                currentBanner.destroy();
                                currentBanner = null;
                            }
                            if (overlayContainer != null && overlayContainer.getParent() instanceof ViewGroup) {
                                ((ViewGroup) overlayContainer.getParent()).removeView(overlayContainer);
                            }
                            overlayContainer = null;
                        } catch (Exception ex) {
                            SDKLogger.d(TAG, "Error cleaning previous banner overlay: " + ex.getMessage());
                        }

                        // Allow host to intercept rendering via the generic onAdRenderOverride; if they handle it,
                        // skip internal rendering. Use the sanitized adm we computed earlier.
                        if (handleRenderOverride(placementId, adm, effectivePosition, AdType.Type.IMAGE, callback)) {
                            SDKLogger.d(TAG, "Image ad rendering overridden by host for placement " + placementId);
                            fireAdLoadedAndDisplayed(placementId, callback);
                            return;
                        }

                        // Create banner WebView via factory
                        currentBanner = BannerViewFactory.createBanner(context, adm);

                        // Determine an initial banner height so the WebView is visible immediately. Use 250dp for
                        // non-fullscreen banners; full screen uses MATCH_PARENT.
                        int defaultHeightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, context.getResources().getDisplayMetrics());

                        // Prepare banner layout params and gravity according to position. Use explicit px height
                        // for non-fullscreen to avoid WRAP_CONTENT measuring to 0 before JS runs.
                        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);

                        if (effectivePosition == AdPosition.FULL_SCREEN) {
                            bannerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                            bannerParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                            bannerParams.gravity = Gravity.CENTER;
                        } else {
                            switch (effectivePosition) {
                                case HEADER:
                                    bannerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                                    break;
                                case FOOTER:
                                    bannerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                                    break;
                                case SIDEBAR:
                                    bannerParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                                    break;
                                default:
                                    bannerParams.gravity = Gravity.CENTER;
                                    break;
                            }
                        }

                        // Label + close in a strip at the TOP so they stay visible when the host clips the
                        // bottom (e.g. fixed-height banner slot) or the WebView is tall.
                        float density = context.getResources().getDisplayMetrics().density;
                        int closeBarPx = (int) (48 * density + 0.5f);

                        int overlayHeight = (bannerParams.height == ViewGroup.LayoutParams.WRAP_CONTENT)
                                ? defaultHeightPx
                                : bannerParams.height;

                        RelativeLayout overlayRoot = new RelativeLayout(context);

                        int totalOverlayHeight = (effectivePosition == AdPosition.FULL_SCREEN)
                                ? ViewGroup.LayoutParams.MATCH_PARENT
                                : ViewGroup.LayoutParams.WRAP_CONTENT;

                        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                                bannerParams.width,
                                totalOverlayHeight,
                                bannerParams.gravity);
                        overlayRoot.setLayoutParams(overlayParams);
                        overlayRoot.setBackgroundColor(Color.parseColor("#33000000"));
                        overlayRoot.setClickable(true);

                        FrameLayout bannerSlot = new FrameLayout(context);
                        bannerSlot.addView(currentBanner, new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                        TextView imageAdLabel = new TextView(context);
                        imageAdLabel.setText(context.getString(R.string.bidscube_label_image_ad));
                        imageAdLabel.setGravity(Gravity.CENTER);
                        imageAdLabel.setTextColor(Color.WHITE);
                        imageAdLabel.setBackgroundColor(0xDD2E7D32);
                        imageAdLabel.setTextSize(13);
                        int labelPadV = (int) (6 * density + 0.5f);
                        imageAdLabel.setPadding(0, labelPadV, 0, labelPadV);

                        Button closeRow = new Button(context);
                        closeRow.setText(context.getString(R.string.bidscube_close_ad_button));
                        closeRow.setTextColor(Color.WHITE);
                        closeRow.setTextSize(14);
                        closeRow.setAllCaps(false);
                        closeRow.setBackgroundColor(0xFFE53935);
                        closeRow.setMinHeight(closeBarPx);
                        int hPad = (int) (12 * density + 0.5f);
                        closeRow.setPadding(hPad, (int) (8 * density + 0.5f), hPad, (int) (8 * density + 0.5f));

                        Runnable dismissOverlay = () -> {
                            try {
                                if (currentBanner != null) {
                                    currentBanner.destroy();
                                    currentBanner = null;
                                }
                                if (overlayContainer != null
                                        && overlayContainer.getParent() instanceof ViewGroup) {
                                    ((ViewGroup) overlayContainer.getParent()).removeView(overlayContainer);
                                }
                            } catch (Throwable ignored) {
                            }
                            overlayContainer = null;
                        };
                        closeRow.setOnClickListener(v -> {
                            dismissOverlay.run();
                            fireAdClosed(placementId, callback);
                        });

                        LinearLayout strip = new LinearLayout(context);
                        strip.setOrientation(LinearLayout.VERTICAL);
                        strip.setId(View.generateViewId());
                        int stripId = strip.getId();
                        strip.setBackgroundColor(0xEE1B5E20);
                        strip.setElevation(18f * density);
                        strip.addView(imageAdLabel, new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                        strip.addView(closeRow, new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));

                        RelativeLayout.LayoutParams stripLp = new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        stripLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        overlayRoot.addView(strip, stripLp);

                        RelativeLayout.LayoutParams slotLp = new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
                        slotLp.addRule(RelativeLayout.BELOW, stripId);
                        slotLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        if (effectivePosition != AdPosition.FULL_SCREEN) {
                            int slotH = (overlayHeight == ViewGroup.LayoutParams.MATCH_PARENT)
                                    ? defaultHeightPx
                                    : overlayHeight;
                            slotLp.height = slotH;
                            slotLp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                        }
                        overlayRoot.addView(bannerSlot, slotLp);

                        overlayContainer = overlayRoot;

                        // Attach overlay to activity content root (the overlay only occupies the banner area)
                        Activity activity = resolveActivityContext();
                        if (activity != null) {
                            ViewGroup root = activity.findViewById(android.R.id.content);
                            if (root != null) {
                                // make overlay interactive and ensure it's on top
                                overlayContainer.setClickable(true);
                                root.addView(overlayContainer);
                                overlayContainer.bringToFront();
                                overlayContainer.requestLayout();
                                if (currentBanner != null) currentBanner.requestLayout();
                                SDKLogger.d(TAG, "Image ad overlay (sized) added to activity content for placement " + placementId);
                                fireAdLoadedAndDisplayed(placementId, callback);
                            } else {
                                SDKLogger.e(TAG, "Activity root (android.R.id.content) not found");
                                reportAdStatFail(placementId, "image", "activity_root_not_found");
                                if (callback != null)
                                    callback.onAdFailed(placementId, -1, "Activity root not found");
                            }
                        } else {
                            SDKLogger.e(TAG, "Could not resolve Activity from Context - ensure SDK initialized with an Activity context");
                            reportAdStatFail(placementId, "image", "no_activity_context");
                            if (callback != null)
                                callback.onAdFailed(placementId, -1, "Could not resolve Activity from Context");
                        }
                    } catch (Exception e) {
                        SDKLogger.e(TAG, "Error rendering image ad: " + e.getMessage());
                        reportAdStatFail(placementId, "image", e.getMessage());
                        if (callback != null) callback.onAdFailed(placementId, -1, e.getMessage());
                    }
                });
            }

            @Override
            public void onFail(Exception e) {
                deliverAdFailure(placementId, "image", callback, e);
            }
        });
    }


    /**
     * Creates image ad view
     */
    private View createImageAdView(String responseBody) {

        // sanitize ADM wrappers if present
        responseBody = sanitizeAdm(responseBody);

        if (currentBanner != null) {
            currentBanner.destroy();
            currentBanner = null;
        }

        currentBanner = BannerViewFactory.createBanner(context, responseBody);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 16, 16, 16);
        container.addView(currentBanner);

        return container;
    }

    /**
     * Creates image ad view with close button
     */
    private ViewGroup createImageAdViewWithCloseButton(String responseBody, Dialog dialog) {

        // sanitize ADM wrappers (document.write etc.) before creating banner view
        responseBody = sanitizeAdm(responseBody);

        if (currentBanner != null) {
            currentBanner.destroy();
            currentBanner = null;
        }

        currentBanner = BannerViewFactory.createBanner(context, responseBody);

        float density = context.getResources().getDisplayMetrics().density;
        int closeBarPx = (int) (48 * density + 0.5f);

        RelativeLayout root = new RelativeLayout(context);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView imageAdLabel = new TextView(context);
        imageAdLabel.setText(context.getString(R.string.bidscube_label_image_ad));
        imageAdLabel.setGravity(Gravity.CENTER);
        imageAdLabel.setTextColor(Color.WHITE);
        imageAdLabel.setBackgroundColor(0xDD2E7D32);
        imageAdLabel.setTextSize(13);
        int labelPadV = (int) (6 * density + 0.5f);
        imageAdLabel.setPadding(0, labelPadV, 0, labelPadV);

        Button closeRow = new Button(context);
        closeRow.setText(context.getString(R.string.bidscube_close_ad_button));
        closeRow.setTextColor(Color.WHITE);
        closeRow.setTextSize(14);
        closeRow.setAllCaps(false);
        closeRow.setBackgroundColor(0xFFE53935);
        closeRow.setMinHeight(closeBarPx);
        int hPad = (int) (12 * density + 0.5f);
        closeRow.setPadding(hPad, (int) (8 * density + 0.5f), hPad, (int) (8 * density + 0.5f));
        closeRow.setOnClickListener(v -> {
            try {
                if (currentBanner != null) {
                    currentBanner.destroy();
                    currentBanner = null;
                }
            } catch (Throwable ignored) {
            }
            try {
                if (dialog != null) {
                    dialog.dismiss();
                }
            } catch (Throwable ignored) {
            }
        });

        LinearLayout strip = new LinearLayout(context);
        strip.setOrientation(LinearLayout.VERTICAL);
        strip.setId(View.generateViewId());
        int stripId = strip.getId();
        strip.setBackgroundColor(0xEE2E7D32);
        strip.setElevation(14f * density);
        strip.addView(imageAdLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        strip.addView(closeRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        RelativeLayout.LayoutParams stripLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        stripLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        root.addView(strip, stripLp);

        final AdPosition position = getCurrentAdPosition();

        FrameLayout bannerSlot = new FrameLayout(context);
        RelativeLayout.LayoutParams slotLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        slotLp.addRule(RelativeLayout.BELOW, stripId);
        slotLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        root.addView(bannerSlot, slotLp);

        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        if (shouldApplyPositioning()) {
            switch (position) {
                case HEADER:
                    bannerParams.gravity = Gravity.TOP;
                    break;
                case FOOTER:
                    bannerParams.gravity = Gravity.BOTTOM;
                    break;
                default:
                    bannerParams.gravity = Gravity.CENTER;
                    break;
            }
        }

        bannerSlot.addView(currentBanner, bannerParams);

        SDKLogger.d(TAG, "Image ad positioned: " + getPositioningDescription());

        return root;
    }

    /**
     * Show video ad with display mode determined by response position
     * This method respects the position value from the ad response
     */
    void showVideoAdWithResponsePosition(String placementId, String url, AdCallback callback) {
        showVideoAdWithResponsePosition(placementId, url, VideoAdFormat.INTERSTITIAL, callback);
    }

    void showVideoAdWithResponsePosition(String placementId, String url, VideoAdFormat format, AdCallback callback) {
        HttpProvider.sendGetRequest(url, new BidscubeCallback() {
            @Override
            public void onSuccess(int responseCode, BidscubeResponse responseBody) {
                runOnUiThread(() -> {
                    Activity dialogActivity = requireActivityForDialog(placementId, "video", callback);
                    if (dialogActivity == null) {
                        return;
                    }
                    try {
                    setResponseAdPosition(responseBody.getPosition());
                    AdPosition effectivePosition = getEffectiveAdPosition();

                    SDKLogger.d(TAG, "Video ad response position: " + responseBody.getPosition() + " -> "
                            + effectivePosition);

                    final String adm = sanitizeAdm(responseBody.getAdm());
                    Log.i(INTEGRATION, "video ad: bid response OK placement=" + placementId + " http=" + responseCode
                            + " admChars=" + (adm != null ? adm.length() : 0)
                            + " responsePos=" + responseBody.getPosition() + " effective=" + effectivePosition);

                    if (handleRenderOverride(placementId, adm, effectivePosition, AdType.Type.VIDEO, callback)) {
                        SDKLogger.d(TAG, "Video ad rendering overridden by host app");
                        Log.i(INTEGRATION, "video ad: host onAdRenderOverride handled UI");
                        fireVideoAdUiReady(placementId, callback);
                        return;
                    }

                    SDKLogger.v("VastResponse", adm);
                    VastParser.analyzeVast(adm);
                    String vastRedirectUrl = VastParser.getClickThroughUrl(adm);

                    if (effectivePosition == AdPosition.FULL_SCREEN) {
                        SDKLogger.d(TAG, "Response indicates full screen display for video ad");


                        Dialog dialog = new Dialog(dialogActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

                        FrameLayout frameContainer = new FrameLayout(dialogActivity);
                        frameContainer.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                        BidscubeVastVideoPlayer videoPlayer = createVastVideoPlayer(adm, vastRedirectUrl);
                        if (!videoPlayer.isVideoSupported()) {
                            if (callback != null) {
                                callback.onAdFailed(placementId, videoPlayer.getUnsupportedErrorCode(),
                                        videoPlayer.getUnsupportedErrorMessage());
                            }
                            return;
                        }
                        AtomicBoolean completed = new AtomicBoolean(false);
                        AtomicBoolean skipped = new AtomicBoolean(false);
                        AtomicBoolean closed = new AtomicBoolean(false);
                        attachVideoLifecycleCallbacks(videoPlayer, placementId, callback, completed, skipped, format);
                        videoPlayer.setLayoutParams(new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                        frameContainer.addView(videoPlayer);
                        attachVideoSkipCloseOverlay(frameContainer, adm, videoPlayer, placementId,
                                callback, completed, skipped, closed, dialog);
                        dialog.setContentView(frameContainer);
                        centerFullScreenDialog(dialog, frameContainer);
                        dialog.show();

                        Log.i(INTEGRATION, "video ad: playVast (fullscreen dialog) player="
                                + videoPlayer.getClass().getSimpleName());
                        videoPlayer.playVast(adm, false);
                        currentVideoPlayer = videoPlayer;

                        fireVideoAdUiReady(placementId, callback);
                        SDKLogger.d(TAG,
                                "Video ad displayed fullscreen with position: " + responseBody.getPosition()
                                        + " -> " + effectivePosition);
                    } else {
                        SDKLogger.d(TAG, "Response indicates windowed display for video ad");


                        Dialog dialog = new Dialog(dialogActivity);
                        String positionName = getPositionDisplayName();
                        dialog.setTitle("Video Ad - " + positionName);

                        FrameLayout frameContainer = new FrameLayout(dialogActivity);
                        frameContainer.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                        BidscubeVastVideoPlayer videoPlayer = createVastVideoPlayer(adm, vastRedirectUrl);
                        if (!videoPlayer.isVideoSupported()) {
                            if (callback != null) {
                                callback.onAdFailed(placementId, videoPlayer.getUnsupportedErrorCode(),
                                        videoPlayer.getUnsupportedErrorMessage());
                            }
                            return;
                        }
                        AtomicBoolean completed = new AtomicBoolean(false);
                        AtomicBoolean skipped = new AtomicBoolean(false);
                        AtomicBoolean closed = new AtomicBoolean(false);
                        attachVideoLifecycleCallbacks(videoPlayer, placementId, callback, completed, skipped, format);
                        int heightPx = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 300, context.getResources().getDisplayMetrics());
                        videoPlayer.setLayoutParams(new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, heightPx));

                        frameContainer.addView(videoPlayer);
                        attachVideoSkipCloseOverlay(frameContainer, adm, videoPlayer, placementId,
                                callback, completed, skipped, closed, dialog);
                        dialog.setContentView(frameContainer);

                        Window window = dialog.getWindow();
                        if (window != null) {
                            int dialogWidth = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8);
                            int dialogHeight = (int) (context.getResources().getDisplayMetrics().heightPixels
                                    * 0.7);
                            positionWindowedDialog(window, dialogWidth, dialogHeight);
                        }

                        dialog.show();
                        Log.i(INTEGRATION, "video ad: playVast (windowed dialog) player="
                                + videoPlayer.getClass().getSimpleName());
                        videoPlayer.playVast(adm, false);
                        currentVideoPlayer = videoPlayer;

                        fireVideoAdUiReady(placementId, callback);
                        SDKLogger.d(TAG, "Video ad displayed windowed with position: " + responseBody.getPosition()
                                + " -> " + effectivePosition);
                    }
                    } catch (Throwable displayError) {
                        deliverAdFailure(placementId, "video", callback, displayError instanceof Exception
                                ? (Exception) displayError
                                : new BidscubeRequestException(AdErrorCode.DISPLAY_ERROR, displayError.getMessage()));
                    }
                });
            }

            @Override
            public void onFail(Exception e) {
                deliverAdFailure(placementId, "video", callback, e);
            }
        });
    }

    /**
     * Shows post-video buttons (exit and install) and hides skip button
     */
    private void showPostVideoButtons(Button exitBtn, Button installBtn, Button skipBtn) {
        exitBtn.setVisibility(View.VISIBLE);
        installBtn.setVisibility(View.VISIBLE);
        skipBtn.setVisibility(View.GONE);
        SDKLogger.d(TAG, "Post-video buttons displayed");
    }

    /**
     * Shows native ad in full screen
     */
    private void showNativeAdFullScreen(String url) {

        HttpProvider.sendGetRequest(url, new BidscubeCallback() {
            @Override
            public void onSuccess(int responseCode, BidscubeResponse responseBody) {
                runOnUiThread(() -> {
                    Activity dialogActivity = resolveActivityContext();
                    if (dialogActivity == null) {
                        SDKLogger.e(TAG, "Cannot show native ad: Activity context required");
                        return;
                    }

                    setResponseAdPosition(responseBody.getPosition());

                    if (getEffectiveAdPosition() == AdPosition.FULL_SCREEN) {
                        SDKLogger.d(TAG, "Response indicates full screen display for native ad");
                    }

                    SDKLogger.d(TAG, "Native ad response received: " + responseBody);

                    Dialog dialog = new Dialog(dialogActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

                    NativeAd nativeAd = NativeAdParser.parseFromAdm(responseBody.getAdm());
                    if (nativeAd != null) {

                        NativeAdView nativeAdView = new NativeAdView(context);

                        nativeAdView.setNativeAd(nativeAd);

                        nativeAdView.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));

                        Button closeBtn = new Button(context);
                        closeBtn.setText("Close");
                        closeBtn.setBackgroundColor(0xFFF44336);
                        closeBtn.setTextColor(Color.WHITE);
                        closeBtn.setOnClickListener(v -> dialog.dismiss());

                        LinearLayout container = new LinearLayout(context);
                        container.setOrientation(LinearLayout.VERTICAL);
                        container.setPadding(32, 32, 32, 32);
                        container.setGravity(Gravity.CENTER);
                        container.addView(nativeAdView);
                        container.addView(closeBtn);

                        dialog.setContentView(container);

                        Window window = dialog.getWindow();
                        if (window != null) {
                            window.setGravity(Gravity.CENTER);
                        }

                        dialog.show();

                        currentNativeAd = nativeAdView;

                        SDKLogger.d(TAG, "Native ad displayed successfully in full screen with " +
                                (nativeAd.assets != null ? nativeAd.assets.size() : 0) + " assets");
                    } else {

                        showNativeAdErrorDialog(dialog, "Failed to parse native ad from JSON response");
                    }
                });
            }

            @Override
            public void onFail(Exception e) {
                SDKLogger.e(TAG, "Error loading native ad: " + AdErrorCode.messageFor(e), e);
                reportAdStatFail("", "native", AdErrorCode.messageFor(e));
                runOnUiThread(() -> {
                    try {
                        Activity activity = resolveActivityContext();
                        if (activity != null) {
                            showNativeAdErrorDialog(new Dialog(activity),
                                    "Error loading native ad: " + AdErrorCode.messageFor(e));
                        }
                    } catch (Throwable dialogError) {
                        SDKLogger.e(TAG, "Could not show native ad error dialog: " + dialogError.getMessage());
                    }
                });
            }
        });
    }

    /**
     * Shows error dialog for native ad failures
     */
    private void showNativeAdErrorDialog(Dialog dialog, String errorMessage) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 32, 32, 32);
        container.setGravity(Gravity.CENTER);

        TextView errorText = new TextView(context);
        errorText.setText("Native Ad Error");
        errorText.setTextSize(20);
        errorText.setTextColor(0xFFF44336);
        errorText.setGravity(Gravity.CENTER);
        errorText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView messageText = new TextView(context);
        messageText.setText(errorMessage);
        messageText.setTextSize(16);
        messageText.setTextColor(Color.BLACK);
        messageText.setGravity(Gravity.CENTER);
        messageText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button closeBtn = new Button(context);
        closeBtn.setText("Close");
        closeBtn.setBackgroundColor(0xFFF44336);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        closeBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        container.addView(errorText);
        container.addView(messageText);
        container.addView(closeBtn);

        dialog.setContentView(container);
        dialog.show();
    }

    /**
     * Shows native ad in full screen mode from a URL
     */
    public void showAdNativeFullScreenFromUrl(String placementId, String url, AdCallback callback) {
        Log.d(TAG, "Loading native ad from URL: " + url);

        HttpProvider.sendGetRequest(url, new BidscubeCallback() {
            @Override
            public void onSuccess(int responseCode, BidscubeResponse responseBody) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Native ad response received from URL: " + responseBody);
                    setResponseAdPosition(responseBody.getPosition());
                    AdPosition effectivePosition = getEffectiveAdPosition();

                    // Parse native ad and give host a typed model to render (backwards-compatible)
                    String sanitized = sanitizeAdm(responseBody.getAdm());
                    com.bidscube.sdk.models.natives.NativeAd nativeAd = null;
                    try {
                        nativeAd = com.bidscube.sdk.network.NativeAdParser.parseFromAdm(sanitized);
                    } catch (Exception e) {
                        SDKLogger.d(TAG, "Failed to parse native ad model: " + e.getMessage());
                    }

                    // First allow host to render using the generic onAdRenderOverride (preferred)
                    boolean hostRendered = false;
                    try {
                        if (callback != null) {
                            com.bidscube.sdk.models.AdRenderContext ctx = new com.bidscube.sdk.models.AdRenderContext(placementId, sanitized, effectivePosition, AdType.Type.NATIVE, nativeAd);
                            hostRendered = callback.onAdRenderOverride(ctx);
                        }
                    } catch (Exception e) {
                        SDKLogger.e(TAG, "Host onAdRenderOverride threw: " + e.getMessage());
                    }

                    // If host implements the generic onAdRenderOverride, call it (pass parsed nativeAd in context)
                    if (!hostRendered) {
                        try {
                            if (callback != null) {
                                com.bidscube.sdk.models.AdRenderContext ctx2 = new com.bidscube.sdk.models.AdRenderContext(placementId, sanitized, effectivePosition, AdType.Type.NATIVE, nativeAd);
                                hostRendered = callback.onAdRenderOverride(ctx2);
                            }
                        } catch (Exception e) {
                            SDKLogger.e(TAG, "Host onAdRenderOverride (fallback) threw: " + e.getMessage());
                        }
                    }

                    if (hostRendered) {
                        com.bidscube.sdk.network.NativeImpressionTracker.fireIfNeeded(nativeAd, "host_render_override_fullscreen_url");
                        SDKLogger.d(TAG, "Native full screen ad rendering handled by host");
                        fireAdLoadedAndDisplayed(placementId, callback);
                        return;
                    }

                    // Fallback to the legacy generic render-override hook (HTML-based) as last resort
                    if (handleRenderOverride(placementId, responseBody.getAdm(), effectivePosition, AdType.Type.NATIVE, callback)) {
                        SDKLogger.d(TAG, "Native full screen ad rendering overridden by host app (legacy)");
                        fireAdLoadedAndDisplayed(placementId, callback);
                        return;
                    }

                    showNativeAdInDialog(sanitized, true, "URL", placementId, callback);
                });
            }

            @Override
            public void onFail(Exception e) {
                deliverAdFailure(placementId, "native", callback, e);
            }
        });
    }

    /**
     * Shows native ad in windowed mode from a URL
     */
    public void showAdNativeWindowedFromUrl(String placementId, String url, AdCallback callback) {
        Log.d(TAG, "Loading native ad from URL (windowed): " + url);

        HttpProvider.sendGetRequest(url, new BidscubeCallback() {
            @Override
            public void onSuccess(int responseCode, BidscubeResponse responseBody) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Native ad response received from URL (windowed): " + responseBody);
                    setResponseAdPosition(responseBody.getPosition());
                    AdPosition effectivePosition = getEffectiveAdPosition();

                    String sanitized = sanitizeAdm(responseBody.getAdm());
                    com.bidscube.sdk.models.natives.NativeAd nativeAd = null;
                    try {
                        nativeAd = com.bidscube.sdk.network.NativeAdParser.parseFromAdm(sanitized);
                    } catch (Exception e) {
                        SDKLogger.d(TAG, "Failed to parse native ad model: " + e.getMessage());
                    }

                    // Preferred: allow host to render via the generic onAdRenderOverride (pass parsed native model)
                    boolean hostRendered = false;
                    try {
                        if (callback != null) {
                            com.bidscube.sdk.models.AdRenderContext ctx = new com.bidscube.sdk.models.AdRenderContext(placementId, sanitized, effectivePosition, AdType.Type.NATIVE, nativeAd);
                            hostRendered = callback.onAdRenderOverride(ctx);
                        }
                    } catch (Exception e) {
                        SDKLogger.e(TAG, "Host onAdRenderOverride threw: " + e.getMessage());
                    }

                    // If host implements the generic onAdRenderOverride, call it (pass parsed nativeAd in context)
                    if (!hostRendered) {
                        try {
                            if (callback != null) {
                                com.bidscube.sdk.models.AdRenderContext ctx2 = new com.bidscube.sdk.models.AdRenderContext(placementId, sanitized, effectivePosition, AdType.Type.NATIVE, nativeAd);
                                hostRendered = callback.onAdRenderOverride(ctx2);
                            }
                        } catch (Exception e) {
                            SDKLogger.e(TAG, "Host onAdRenderOverride (fallback) threw: " + e.getMessage());
                        }
                    }

                    if (hostRendered) {
                        com.bidscube.sdk.network.NativeImpressionTracker.fireIfNeeded(nativeAd, "host_render_override_windowed_url");
                        SDKLogger.d(TAG, "Native windowed ad rendering handled by host");
                        fireAdLoadedAndDisplayed(placementId, callback);
                        return;
                    }

                    if (handleRenderOverride(placementId, responseBody.getAdm(), effectivePosition, AdType.Type.NATIVE, callback)) {
                        SDKLogger.d(TAG, "Native windowed ad rendering overridden by host app (legacy)");
                        fireAdLoadedAndDisplayed(placementId, callback);
                        return;
                    }

                    showNativeAdInDialog(sanitized, false, "URL", placementId, callback);
                });
            }

            @Override
            public void onFail(Exception e) {
                deliverAdFailure(placementId, "native", callback, e);
            }
        });
    }

    /**
     * Get image ad view for integration into layouts (no dialog)
     *
     * @param url      Ad request URL
     * @param callback Callback for ad events
     * @return View that can be added to any layout
     */
    public View getImageAdView(String placementId, String url, AdCallback callback) {
        SDKLogger.d(TAG, "Getting image ad view for integration: " + url);

        final RelativeLayout root = new RelativeLayout(context);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.setBackgroundColor(Color.parseColor("#4CAF50"));
        root.setPadding(16, 16, 16, 16);

        TextView loadingText = new TextView(context);
        loadingText.setText("Loading image ad...");
        loadingText.setTextColor(Color.WHITE);
        loadingText.setTextSize(14);
        loadingText.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams loadLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        loadLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        root.addView(loadingText, loadLp);

        sendAdRequest(url, new BidscubeCallback() {
            @Override
            public void onSuccess(int responseCode, BidscubeResponse response) {
                runOnUiThread(() -> {

                    setResponseAdPosition(response.getPosition());

                    root.removeView(loadingText);

                    if (handleRenderOverride(placementId, sanitizeAdm(response.getAdm()),
                            getEffectiveAdPosition(), AdType.Type.IMAGE, callback)) {
                        SDKLogger.d(TAG, "Image ad view rendering overridden by host app");
                        fireAdLoadedAndDisplayed(placementId, callback);
                        return;
                    }

                    float density = context.getResources().getDisplayMetrics().density;
                    int closeBarPx = (int) (48 * density + 0.5f);
                    int creativeMaxPx = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            280f,
                            context.getResources().getDisplayMetrics());

                    TextView imageAdLabel = new TextView(context);
                    imageAdLabel.setText(context.getString(R.string.bidscube_label_image_ad));
                    imageAdLabel.setGravity(Gravity.CENTER);
                    imageAdLabel.setTextColor(Color.WHITE);
                    imageAdLabel.setBackgroundColor(0xDD1B5E20);
                    imageAdLabel.setTextSize(13);
                    int labelPadV = (int) (6 * density + 0.5f);
                    imageAdLabel.setPadding(0, labelPadV, 0, labelPadV);

                    Button closeRow = new Button(context);
                    closeRow.setText(context.getString(R.string.bidscube_close_ad_button));
                    closeRow.setTextColor(Color.WHITE);
                    closeRow.setTextSize(14);
                    closeRow.setAllCaps(false);
                    closeRow.setBackgroundColor(0xFFE53935);
                    closeRow.setMinHeight(closeBarPx);
                    int hPad = (int) (12 * density + 0.5f);
                    closeRow.setPadding(hPad, (int) (8 * density + 0.5f), hPad, (int) (8 * density + 0.5f));
                    closeRow.setOnClickListener(v -> {
                        try {
                            if (currentBanner != null) {
                                currentBanner.destroy();
                                currentBanner = null;
                            }
                        } catch (Throwable ignored) {
                        }
                        root.removeAllViews();
                        root.setVisibility(View.GONE);
                        fireAdClosed(placementId, callback);
                    });

                    LinearLayout strip = new LinearLayout(context);
                    strip.setOrientation(LinearLayout.VERTICAL);
                    strip.setId(View.generateViewId());
                    int stripId = strip.getId();
                    strip.setBackgroundColor(0xEE1B5E20);
                    strip.setElevation(14f * density);
                    strip.addView(imageAdLabel, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    strip.addView(closeRow, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    RelativeLayout.LayoutParams stripLp = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    stripLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    root.addView(strip, stripLp);

                    View adView = createImageAdView(sanitizeAdm(response.getAdm()));
                    ScrollView creativeScroll = new ScrollView(context);
                    creativeScroll.setFillViewport(true);
                    creativeScroll.addView(adView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    RelativeLayout.LayoutParams scrollLp = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            creativeMaxPx);
                    scrollLp.addRule(RelativeLayout.BELOW, stripId);
                    root.addView(creativeScroll, scrollLp);

                    fireAdLoadedAndDisplayed(placementId, callback);

                    SDKLogger.d(TAG, "Image ad view created and integrated into container");
                });
            }

            @Override
            public void onFail(Exception e) {
                deliverAdFailure(placementId, "image", callback, e);
                final String msg = AdErrorCode.messageFor(e);
                runOnUiThread(() -> {
                    try {
                        root.removeView(loadingText);
                        TextView errorText = new TextView(context);
                        errorText.setText("Failed to load ad: " + msg);
                        errorText.setTextColor(Color.WHITE);
                        errorText.setTextSize(14);
                        errorText.setGravity(Gravity.CENTER);
                        RelativeLayout.LayoutParams errLp = new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        errLp.addRule(RelativeLayout.CENTER_IN_PARENT);
                        root.addView(errorText, errLp);
                    } catch (Throwable uiError) {
                        SDKLogger.e(TAG, "Failed to update image ad view error UI: " + uiError.getMessage());
                    }
                });
            }
        });

        return root;
    }

    /**
     * Get video ad view for integration into layouts (no dialog)
     *
     * @param url      Ad request URL
     * @param callback Callback for ad events
     * @return View that can be added to any layout
     */
    public View getVideoAdView(String placementId, String url, AdCallback callback) {
        SDKLogger.d(TAG, "Getting video ad view for integration: " + url);

        final FrameLayout root = new FrameLayout(context);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout adInner = new LinearLayout(context);
        adInner.setOrientation(LinearLayout.VERTICAL);
        adInner.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        adInner.setBackgroundColor(Color.parseColor("#1976D2"));
        adInner.setPadding(16, 16, 16, 16);
        root.addView(adInner);

        TextView loadingText = new TextView(context);
        loadingText.setText("Loading video ad...");
        loadingText.setTextColor(Color.WHITE);
        loadingText.setTextSize(14);
        loadingText.setGravity(Gravity.CENTER);
        adInner.addView(loadingText);

        sendAdRequest(url, new BidscubeCallback() {
            @Override
            public void onSuccess(int responseCode, BidscubeResponse responseBody) {
                runOnUiThread(() -> {

                    setResponseAdPosition(responseBody.getPosition());

                    adInner.removeView(loadingText);

                    try {
                        if (handleRenderOverride(placementId, responseBody.getAdm(), getEffectiveAdPosition(), AdType.Type.VIDEO, callback)) {
                            SDKLogger.d(TAG, "Video ad view rendering overridden by host app");
                            fireAdLoadedAndDisplayed(placementId, callback);
                            return;
                        }

                        final String adm = sanitizeAdm(responseBody.getAdm());
                        Log.i(INTEGRATION, "getVideoAdView: bid OK placement=" + placementId + " admChars="
                                + (adm != null ? adm.length() : 0));
                        SDKLogger.v("VastResponse", adm);
                        VastParser.analyzeVast(adm);
                        String vastRedirectUrl = VastParser.getClickThroughUrl(adm);

                        final BidscubeVastVideoPlayer[] playerHolder = new BidscubeVastVideoPlayer[1];
                        BidscubeVastVideoPlayer videoPlayer = createVastVideoPlayer(adm, vastRedirectUrl);
                        if (!videoPlayer.isVideoSupported()) {
                            if (callback != null) {
                                callback.onAdFailed(placementId, videoPlayer.getUnsupportedErrorCode(),
                                        videoPlayer.getUnsupportedErrorMessage());
                            }
                            return;
                        }
                        AtomicBoolean completed = new AtomicBoolean(false);
                        AtomicBoolean skipped = new AtomicBoolean(false);
                        attachVideoLifecycleCallbacks(videoPlayer, placementId, callback, completed, skipped,
                                VideoAdFormat.INTERSTITIAL);
                        playerHolder[0] = videoPlayer;
                        AdPosition embedPosition = getEffectiveAdPosition();
                        int heightPx;
                        if (embedPosition == AdPosition.FULL_SCREEN) {
                            heightPx = ViewGroup.LayoutParams.MATCH_PARENT;
                            root.setLayoutParams(new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT));
                            adInner.setLayoutParams(new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT));
                        } else {
                            heightPx = (int) TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP, 300, context.getResources().getDisplayMetrics());
                        }
                        videoPlayer.setLayoutParams(new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, heightPx));

                        Button playButton = new Button(context);
                        playButton.setText("▶ PLAY VIDEO AD");
                        playButton.setTextSize(16);
                        playButton.setBackgroundColor(Color.parseColor("#FF5722"));
                        playButton.setTextColor(Color.WHITE);
                        playButton.setPadding(16, 8, 16, 8);
                        playButton.setOnClickListener(v -> {
                            BidscubeVastVideoPlayer p = playerHolder[0];
                            if (p == null) {
                                return;
                            }
                            Log.i(INTEGRATION, "getVideoAdView: user tapped PLAY; playVast player="
                                    + p.getClass().getSimpleName());
                            p.playVast(adm, false);
                            playButton.setVisibility(View.GONE);
                            if (callback != null) {
                                callback.onVideoAdStarted(placementId);
                            }
                        });

                        adInner.addView(videoPlayer);
                        adInner.addView(playButton);

                        ImageButton close = buildCloseAdControl(() -> {
                            BidscubeVastVideoPlayer p = playerHolder[0];
                            if (p != null) {
                                try {
                                    if (!completed.get()) {
                                        p.skipVideo();
                                    }
                                } catch (Throwable ignored) {
                                    fireVideoAdSkipped(placementId, callback, completed, skipped);
                                }
                                try {
                                    p.release();
                                } catch (Throwable ignored) {
                                }
                                playerHolder[0] = null;
                                if (currentVideoPlayer == p) {
                                    currentVideoPlayer = null;
                                }
                            }
                            adInner.removeAllViews();
                            root.setVisibility(View.GONE);
                        }, placementId, callback);
                        mountCloseOnFrame(root, close);

                        fireAdLoadedAndDisplayed(placementId, callback);

                        SDKLogger.d(TAG, "Video ad view created and integrated into container");

                    } catch (Exception e) {
                        SDKLogger.e(TAG, "Error creating video ad view: " + e.getMessage());
                        Log.e(INTEGRATION, "getVideoAdView: build player/view failed placement=" + placementId, e);
                        reportAdStatFail(placementId, "video", e.getMessage());
                        TextView errorText = new TextView(context);
                        errorText.setText("Failed to create video ad: " + e.getMessage());
                        errorText.setTextColor(Color.WHITE);
                        errorText.setTextSize(14);
                        errorText.setGravity(Gravity.CENTER);
                        adInner.addView(errorText);

                        if (callback != null) {
                            callback.onAdFailed(placementId, -1, e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onFail(Exception e) {
                deliverAdFailure(placementId, "video", callback, e);
                final String msg = AdErrorCode.messageFor(e);
                runOnUiThread(() -> {
                    try {
                        adInner.removeView(loadingText);
                        TextView errorText = new TextView(context);
                        errorText.setText("Failed to load ad: " + msg);
                        errorText.setTextColor(Color.WHITE);
                        errorText.setTextSize(14);
                        errorText.setGravity(Gravity.CENTER);
                        adInner.addView(errorText);
                    } catch (Throwable uiError) {
                        SDKLogger.e(TAG, "Failed to update video ad view error UI: " + uiError.getMessage());
                    }
                });
            }
        });

        return root;
    }

    /**
     * Get native ad view for integration into layouts (no dialog)
     *
     * @param url      Ad request URL
     * @param callback Callback for ad events
     * @return View that can be added to any layout
     */
    public View getNativeAdView(String placementId, String url, AdCallback callback) {
        SDKLogger.d(TAG, "Getting native ad view for integration: " + url);

        final FrameLayout root = new FrameLayout(context);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout adInner = new LinearLayout(context);
        adInner.setOrientation(LinearLayout.VERTICAL);
        adInner.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        adInner.setBackgroundColor(Color.parseColor("#FF9800"));
        adInner.setPadding(16, 16, 16, 16);
        root.addView(adInner);

        TextView loadingText = new TextView(context);
        loadingText.setText("Loading native ad...");
        loadingText.setTextColor(Color.WHITE);
        loadingText.setTextSize(14);
        loadingText.setGravity(Gravity.CENTER);
        adInner.addView(loadingText);

        sendAdRequest(url, new BidscubeCallback() {
            @Override
            public void onSuccess(int responseCode, BidscubeResponse responseBody) {
                runOnUiThread(() -> {

                    setResponseAdPosition(responseBody.getPosition());

                    adInner.removeView(loadingText);

                    try {
                        SDKLogger.d(TAG, "Native ad response received: " + responseBody);

                        String sanitized = sanitizeAdm(responseBody.getAdm());
                        com.bidscube.sdk.models.natives.NativeAd nativeAd = null;
                        try {
                            nativeAd = NativeAdParser.parseFromAdm(sanitized);
                        } catch (Exception e) {
                            SDKLogger.d(TAG, "Failed to parse native ad model: " + e.getMessage());
                        }

                        // Preferred: allow host to render via the generic onAdRenderOverride (pass parsed native model)
                        boolean hostRendered = false;
                        try {
                            if (callback != null) {
                                AdRenderContext ctx = new AdRenderContext(placementId, sanitized, getEffectiveAdPosition(), AdType.Type.NATIVE, nativeAd);
                                hostRendered = callback.onAdRenderOverride(ctx);
                            }
                        } catch (Exception e) {
                            SDKLogger.e(TAG, "Host onAdRenderOverride threw: " + e.getMessage());
                        }

                        if (hostRendered) {
                            com.bidscube.sdk.network.NativeImpressionTracker.fireIfNeeded(nativeAd, "host_render_override_embedded_view");
                            SDKLogger.d(TAG, "Native ad view handled by host");
                            fireAdLoadedAndDisplayed(placementId, callback);
                            return;
                        }

                        if (handleRenderOverride(placementId, responseBody.getAdm(), getEffectiveAdPosition(), AdType.Type.NATIVE, callback)) {
                            SDKLogger.d(TAG, "Native ad view rendering overridden by host app (legacy)");
                            fireAdLoadedAndDisplayed(placementId, callback);
                            return;
                        }

                        if (nativeAd != null) {

                            NativeAd nativeAdLocal = nativeAd;
                            NativeAdView nativeAdView = new NativeAdView(context);
                            nativeAdView.setNativeAd(nativeAdLocal);
                            nativeAdView.setLayoutParams(new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));

                            adInner.addView(nativeAdView);

                            ImageButton close = buildCloseAdControl(() -> {
                                adInner.removeAllViews();
                                root.setVisibility(View.GONE);
                            }, placementId, callback);
                            mountCloseOnFrame(root, close);

                            fireAdLoadedAndDisplayed(placementId, callback);

                            SDKLogger.d(TAG, "Native ad view created and integrated into container with " +
                                    (nativeAdLocal.assets != null ? nativeAdLocal.assets.size() : 0) + " assets");
                        } else {
                            throw new Exception("Failed to parse native ad from response");
                        }

                    } catch (Exception e) {
                        SDKLogger.e(TAG, "Error creating native ad view: " + e.getMessage());
                        reportAdStatFail(placementId, "native", e.getMessage());
                        TextView errorText = new TextView(context);
                        errorText.setText("Failed to create native ad: " + e.getMessage());
                        errorText.setTextColor(Color.WHITE);
                        errorText.setTextSize(14);
                        errorText.setGravity(Gravity.CENTER);
                        adInner.addView(errorText);

                        if (callback != null) {
                            callback.onAdFailed(placementId, -1, e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onFail(Exception e) {
                deliverAdFailure(placementId, "native", callback, e);
                final String msg = AdErrorCode.messageFor(e);
                runOnUiThread(() -> {
                    try {
                        adInner.removeView(loadingText);
                        TextView errorText = new TextView(context);
                        errorText.setText("Failed to load ad: " + msg);
                        errorText.setTextColor(Color.WHITE);
                        errorText.setTextSize(14);
                        errorText.setGravity(Gravity.CENTER);
                        adInner.addView(errorText);
                    } catch (Throwable uiError) {
                        SDKLogger.e(TAG, "Failed to update native ad view error UI: " + uiError.getMessage());
                    }
                });
            }
        });

        return root;
    }

    /**
     * Positions a windowed dialog based on the current ad position setting
     */
    public void positionWindowedDialog(Window window, int dialogWidth, int dialogHeight) {
        if (window == null)
            return;

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;

        window.setLayout(dialogWidth, dialogHeight);

        AdPosition effectivePosition = getEffectiveAdPosition();

        switch (effectivePosition) {
            case ABOVE_THE_FOLD:

                window.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                SDKLogger.d(TAG, "Positioned dialog above the fold");
                break;

            case BELOW_THE_FOLD:

                window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                SDKLogger.d(TAG, "Positioned dialog below the fold");
                break;

            case HEADER:

                window.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                SDKLogger.d(TAG, "Positioned dialog at header");
                break;

            case FOOTER:

                window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                SDKLogger.d(TAG, "Positioned dialog at footer");
                break;

            case SIDEBAR:

                window.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                SDKLogger.d(TAG, "Positioned dialog at sidebar");
                break;

            case MAYBE_DEPENDING_ON_SCREEN_SIZE:

                if (screenHeight > screenWidth) {

                    window.setGravity(Gravity.CENTER);
                    SDKLogger.d(TAG, "Portrait mode - positioned dialog in center");
                } else {

                    window.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                    SDKLogger.d(TAG, "Landscape mode - positioned dialog on right side");
                }
                break;

            case FULL_SCREEN:

                SDKLogger.w(TAG, "Full screen position requested for windowed ad - using center");
                window.setGravity(Gravity.CENTER);
                break;

            case UNKNOWN:
            default:

                SDKLogger.d(TAG, "No position regulation applied - dialog will display naturally");
                break;
        }

        if (shouldApplyPositioning()) {
            SDKLogger.d(TAG, "Positioned windowed dialog with gravity for position: " + effectivePosition);
            SDKLogger.d(TAG, "Dialog size: " + dialogWidth + "x" + dialogHeight);
            SDKLogger.d(TAG, "Screen size: " + screenWidth + "x" + screenHeight);
            SDKLogger.d(TAG, "Gravity set to: " + window.getAttributes().gravity);
        } else {
            SDKLogger.d(TAG, "No positioning applied - dialog will display naturally");
        }
    }

    /**
     * Helper method to show native ad in dialog (full screen or windowed)
     */
    private void showNativeAdInDialog(String jsonData, boolean isFullScreen, String source,
                                      String placementId, AdCallback callback) {
        Activity dialogActivity = requireActivityForDialog(placementId, "native", callback);
        if (dialogActivity == null) {
            return;
        }
        try {

            NativeAd nativeAd = NativeAdParser.parseFromAdm(jsonData);
            if (nativeAd != null) {

                NativeAdView nativeAdView = new NativeAdView(context);

                nativeAdView.setNativeAd(nativeAd);

                Dialog dialog;
                if (isFullScreen) {
                    dialog = new Dialog(dialogActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                } else {
                    dialog = new Dialog(dialogActivity);
                }

                if (isFullScreen) {
                    nativeAdView.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                }

                Button closeBtn = new Button(context);
                closeBtn.setText("Close");
                closeBtn.setBackgroundColor(0xFFF44336);
                closeBtn.setTextColor(Color.WHITE);
                closeBtn.setOnClickListener(v -> dialog.dismiss());

                LinearLayout container = new LinearLayout(context);
                container.setOrientation(LinearLayout.VERTICAL);
                container.setPadding(isFullScreen ? 32 : 16, isFullScreen ? 32 : 16,
                        isFullScreen ? 32 : 16, isFullScreen ? 32 : 16);
                container.addView(nativeAdView);
                container.addView(closeBtn);

                dialog.setContentView(container);

                if (isFullScreen) {
                    centerFullScreenDialog(dialog, container);
                }

                if (!isFullScreen) {
                    Window window = dialog.getWindow();
                    if (window != null) {
                        int dialogWidth = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8);
                        int dialogHeight = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.6);

                        SDKLogger.d(TAG, "Native ad windowed (flexible) - Current position: " + currentAdPosition);

                    }
                }

                dialog.show();

                currentNativeAd = nativeAdView;

                fireAdLoadedAndDisplayed(placementId, callback);
                SDKLogger.d(TAG, "Native ad displayed successfully from " + source + " in " +
                        (isFullScreen ? "full screen" : "windowed") + " mode");

            } else {
                SDKLogger.e(TAG, "Failed to parse native ad from " + source);
                invokeAdFailed(callback, placementId, AdErrorCode.INVALID_RESPONSE,
                        "Failed to parse native ad from " + source);
                showNativeAdErrorDialog(new Dialog(dialogActivity),
                        "Failed to parse native ad from " + source);
            }
        } catch (Exception e) {
            SDKLogger.e(TAG, "Error showing native ad from " + source + ": " + e.getMessage());
            deliverAdFailure(placementId, "native", callback, e instanceof Exception
                    ? (Exception) e
                    : new BidscubeRequestException(AdErrorCode.DISPLAY_ERROR, e.getMessage()));
            showNativeAdErrorDialog(new Dialog(dialogActivity),
                    "Error showing native ad from " + source + ": " + e.getMessage());
        }
    }

    /**
     * Cleans up resources
     */
    public void cleanup() {
        // Clear banners attached by NativeAdBinder
        try {
            NativeAdBinder.clearAttachedBanner();
        } catch (Throwable ignored) {
        }

        if (currentBanner != null) {
            try {
                currentBanner.destroy();
            } catch (Throwable ignored) {
            }
            currentBanner = null;
        }

        if (currentVideoPlayer != null) {
            try {
                currentVideoPlayer.release();
            } catch (Throwable ignored) {
            }
            currentVideoPlayer = null;
        }

        if (currentNativeAd != null) {
            currentNativeAd = null;
        }

        // Remove any overlay container we added to the activity
        try {
            if (overlayContainer != null && overlayContainer.getParent() instanceof ViewGroup) {
                ((ViewGroup) overlayContainer.getParent()).removeView(overlayContainer);
            }
        } catch (Throwable ignored) {
        }
        overlayContainer = null;

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }
}
