
package com.bidscube.sdk;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.errors.AdErrorCode;
import com.bidscube.sdk.consent.ConsentManager;
import com.bidscube.sdk.device.providers.DeviceInfoProvider;
import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.interfaces.ConsentCallback;
import com.bidscube.sdk.interfaces.IBidscubeSDK;
import com.bidscube.sdk.interfaces.InitializationCallback;
import com.bidscube.sdk.models.DeviceInfo;
import com.bidscube.sdk.models.enums.AdPosition;
import com.bidscube.sdk.ads.ImageAdType;
import com.bidscube.sdk.ads.NativeAdType;
import com.bidscube.sdk.ads.VideoAdFormat;
import com.bidscube.sdk.ads.VideoAdType;
import com.bidscube.sdk.stats.SdkStatsReporter;
import com.bidscube.sdk.utils.SDKLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Main implementation of Bidscube SDK
 * This class handles all ad operations and provides a clean interface for
 * external applications
 */
public class BidscubeSDKImpl implements IBidscubeSDK {

    private static final String TAG = "BidscubeSDKImpl";
    /** Logcat filter for integration health (init, loads, player); independent of {@link SDKLogger} flag. */
    private static final String INTEGRATION = "BidscubeIntegration";

    private Context context;
    private SDKConfig config;
    private DeviceInfo deviceInfo;
    private AdDisplayManager adDisplayManager;
    private DeviceInfoProvider deviceInfoProvider;
    private ConsentManager consentManager;
    private boolean isInitialized = false;
    private final List<InitializationCallback> pendingInitCallbacks = new ArrayList<>();

    @Override
    public void initialize(Context context, SDKConfig config) {
        initialize(context, config, null);
    }

    @Override
    public void initialize(Context context, SDKConfig config, InitializationCallback callback) {
        if (isInitialized) {
            SDKLogger.w(TAG, "SDK already initialized");
            Log.i(INTEGRATION, "BidscubeSDKImpl: initialize skipped (already initialized)");
            if (callback != null) {
                callback.onInitialized();
            }
            return;
        }

        if (callback != null) {
            synchronized (pendingInitCallbacks) {
                pendingInitCallbacks.add(callback);
            }
        }

        if (deviceInfoProvider != null) {
            SDKLogger.w(TAG, "SDK initialization already in progress");
            return;
        }

        this.context = context;
        this.config = config;

        Log.i(INTEGRATION, "BidscubeSDKImpl: initialize started (async DeviceInfo next); "
                + "adRequestAuthority=" + config.getAdRequestAuthority()
                + " vastFactory=" + (config.getVastVideoPlayerFactory() != null));

        SDKLogger.d(TAG, "adRequestAuthority from SDKConfig: " + config.getAdRequestAuthority());

        // Configure logging based on SDKConfig
        SDKLogger.setLoggingEnabled(config.isEnableLogging());
        SDKLogger.setDefaultTag(TAG);

        try {

            deviceInfoProvider = new DeviceInfoProvider(context, config);
            consentManager = deviceInfoProvider.getConsentManager();

            deviceInfoProvider.getDeviceInfoAsync(deviceInfo -> {
                this.deviceInfo = deviceInfo;
                SDKLogger.d(TAG, "DeviceInfo adRequestAuthority: " + deviceInfo.getAdRequestAuthority());

                this.adDisplayManager = new AdDisplayManager(context, deviceInfo, config);

                this.isInitialized = true;
                SDKLogger.d(TAG, "SDK initialized successfully");
                Log.i(INTEGRATION, "BidscubeSDKImpl: ready for ads — isInitialized=true; "
                        + "adRequestAuthority=" + deviceInfo.getAdRequestAuthority()
                        + " sdkVersion=" + com.bidscube.sdk.BuildConfig.SDK_VERSION_NAME);
                SdkStatsReporter.reportSdkInit(config, deviceInfo);

                if (config.getDefaultAdPosition() != null) {
                    AdPosition position = AdPosition.fromString(config.getDefaultAdPosition());
                    setAdPosition(position);
                }

                notifyInitializationSuccess();
            });

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to initialize SDK: " + e.getMessage(), e);
            Log.e(INTEGRATION, "BidscubeSDKImpl: initialize failed: " + e.getMessage(), e);
            notifyInitializationFailure(e.getMessage() != null ? e.getMessage() : "SDK initialization failed");
            throw new RuntimeException("SDK initialization failed", e);
        }
    }

    @Override
    public void registerInitializationCallback(InitializationCallback callback) {
        if (callback == null) {
            return;
        }
        if (isInitialized) {
            callback.onInitialized();
            return;
        }
        synchronized (pendingInitCallbacks) {
            pendingInitCallbacks.add(callback);
        }
    }

    private void notifyInitializationSuccess() {
        final List<InitializationCallback> callbacks;
        synchronized (pendingInitCallbacks) {
            callbacks = new ArrayList<>(pendingInitCallbacks);
            pendingInitCallbacks.clear();
        }
        for (InitializationCallback callback : callbacks) {
            try {
                callback.onInitialized();
            } catch (Throwable ignored) {
            }
        }
    }

    private void notifyInitializationFailure(String message) {
        final List<InitializationCallback> callbacks;
        synchronized (pendingInitCallbacks) {
            callbacks = new ArrayList<>(pendingInitCallbacks);
            pendingInitCallbacks.clear();
        }
        for (InitializationCallback callback : callbacks) {
            try {
                callback.onInitializationFailed(message);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void setDisplayActivity(Activity activity) {
        if (adDisplayManager != null) {
            adDisplayManager.setDisplayActivity(activity);
        }
    }

    @Override
    public void setUserId(String userId) {
        if (config != null) {
            config = config.withUserId(userId);
        }
        if (deviceInfo != null) {
            deviceInfo = deviceInfo.withUserId(userId);
        }
        SDKLogger.d(TAG, "Publisher user id updated for ad requests");
    }

    @Override
    public void showImageAd(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null) {
            callback.onAdLoading(placementId);
        }

        try {
            if (adDisplayManager.consumeCachedImageAd(placementId)) {
                adDisplayManager.showCachedImageAd(placementId, callback);
                return;
            }

            ImageAdType imageAdType = new ImageAdType(placementId);
            String url = imageAdType.buildRequestUrl(deviceInfo).toString();
            Log.i(INTEGRATION, "showImageAd: placement=" + placementId + " requestUrl=" + url);

            adDisplayManager.showImageAdWithResponsePosition(placementId, url, callback);

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to show image ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to show image ad: " + AdErrorCode.messageFor(e));
            }
        }
    }

    @Override
    public void showVideoAd(String placementId, AdCallback callback) {
        showInterstitialVideoAd(placementId, callback);
    }

    @Override
    public void showVideoAdFromVastMarkup(String placementId, String vastXml, AdCallback callback) {
        checkInitialization();
        if (callback != null) {
            callback.onAdLoading(placementId);
        }
        try {
            Log.i(INTEGRATION, "showVideoAdFromVastMarkup: placement=" + placementId
                    + " vastChars=" + (vastXml != null ? vastXml.length() : 0));
            adDisplayManager.showVideoAdFromVastMarkup(placementId, vastXml, callback);
        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to show video from VAST markup: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to show video from VAST markup: " + AdErrorCode.messageFor(e));
            }
        }
    }

    @Override
    public void showInterstitialVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null) {
            callback.onAdLoading(placementId);
        }

        try {
            if (adDisplayManager.consumeCachedVideoAd(placementId, VideoAdFormat.INTERSTITIAL)) {
                adDisplayManager.showCachedVideoAd(placementId, VideoAdFormat.INTERSTITIAL, callback);
                return;
            }

            VideoAdType videoAdType = new VideoAdType(placementId);
            String url = videoAdType.buildRequestUrl(deviceInfo).toString();
            Log.i(INTEGRATION, "showInterstitialVideoAd: placement=" + placementId + " requestUrl=" + url);

            adDisplayManager.showVideoAdWithResponsePosition(placementId, url, VideoAdFormat.INTERSTITIAL, callback);

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to show interstitial video ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to show interstitial video ad: " + AdErrorCode.messageFor(e));
            }
        }
    }

    @Override
    public void showRewardedVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null) {
            callback.onAdLoading(placementId);
        }

        try {
            if (adDisplayManager.consumeCachedVideoAd(placementId, VideoAdFormat.REWARDED)) {
                adDisplayManager.showCachedVideoAd(placementId, VideoAdFormat.REWARDED, callback);
                return;
            }

            VideoAdType videoAdType = new VideoAdType(placementId);
            String url = videoAdType.buildRequestUrl(deviceInfo).toString();
            Log.i(INTEGRATION, "showRewardedVideoAd: placement=" + placementId + " requestUrl=" + url);

            adDisplayManager.showVideoAdWithResponsePosition(placementId, url, VideoAdFormat.REWARDED, callback);

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to show rewarded video ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to show rewarded video ad: " + AdErrorCode.messageFor(e));
            }
        }
    }

    @Override
    public void preloadInterstitialVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null) {
            callback.onAdLoading(placementId);
        }
        try {
            VideoAdType videoAdType = new VideoAdType(placementId);
            String url = videoAdType.buildRequestUrl(deviceInfo).toString();
            adDisplayManager.preloadVideoAd(placementId, url, VideoAdFormat.INTERSTITIAL, callback);
        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to preload interstitial video ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to preload interstitial video ad: " + AdErrorCode.messageFor(e));
            }
        }
    }

    @Override
    public void preloadRewardedVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null) {
            callback.onAdLoading(placementId);
        }
        try {
            VideoAdType videoAdType = new VideoAdType(placementId);
            String url = videoAdType.buildRequestUrl(deviceInfo).toString();
            adDisplayManager.preloadVideoAd(placementId, url, VideoAdFormat.REWARDED, callback);
        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to preload rewarded video ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to preload rewarded video ad: " + AdErrorCode.messageFor(e));
            }
        }
    }

    @Override
    public void preloadImageAd(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null) {
            callback.onAdLoading(placementId);
        }
        try {
            ImageAdType imageAdType = new ImageAdType(placementId);
            String url = imageAdType.buildRequestUrl(deviceInfo).toString();
            adDisplayManager.preloadImageAd(placementId, url, callback);
        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to preload image ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to preload image ad: " + AdErrorCode.messageFor(e));
            }
        }
    }

    @Override
    public void clearPreloadCache() {
        if (adDisplayManager != null) {
            adDisplayManager.clearPreloadCache();
        }
    }

    @Override
    @Deprecated
    public void showSkippableVideoAd(String placementId, AdCallback callback) {
        // Delegates to standard video ad; use showVideoAd for new code.
        showVideoAd(placementId, callback);
    }

    @Override
    public void showNativeAd(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null)
            callback.onAdLoading(placementId);

        try {
            NativeAdType nativeAdType = new NativeAdType(placementId);
            String url = nativeAdType.buildRequestUrl(deviceInfo).toString();

            // Determine display mode based on response position
            AdPosition effectivePosition = adDisplayManager.getEffectiveAdPosition();
            boolean shouldShowFullScreen = shouldShowFullScreen(effectivePosition);

            if (shouldShowFullScreen) {
                adDisplayManager.showAdNativeFullScreenFromUrl(placementId, url, callback);
            } else {
                adDisplayManager.showAdNativeWindowedFromUrl(placementId, url, callback);
            }

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to show native ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to show native ad: " + AdErrorCode.messageFor(e));
            }
        }
    }

    /**
     * Determine if ad should be shown in full screen based on position
     *
     * @param position Ad position from response
     * @return true if should show full screen, false for windowed
     */
    private boolean shouldShowFullScreen(AdPosition position) {
        switch (position) {
            case FULL_SCREEN:
                return true;
            case UNKNOWN:
            case ABOVE_THE_FOLD:
            case BELOW_THE_FOLD:
            case HEADER:
            case FOOTER:
            case SIDEBAR:
            case MAYBE_DEPENDING_ON_SCREEN_SIZE:
            default:
                return false;
        }
    }

    @Override
    public View getImageAdView(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null)
            callback.onAdLoading(placementId);

        try {
            ImageAdType imageAdType = new ImageAdType(placementId);
            String url = imageAdType.buildRequestUrl(deviceInfo).toString();

            return adDisplayManager.getImageAdView(placementId, url, callback);

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to get image ad view: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to get image ad view: " + AdErrorCode.messageFor(e));
            }
            return createErrorView("Failed to load image ad: " + e.getMessage());
        }
    }

    @Override
    public View getVideoAdView(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null)
            callback.onAdLoading(placementId);

        try {
            VideoAdType videoAdType = new VideoAdType(placementId);
            String url = videoAdType.buildRequestUrl(deviceInfo).toString();

            return adDisplayManager.getVideoAdView(placementId, url, callback);

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to get video ad view: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to get video ad view: " + AdErrorCode.messageFor(e));
            }
            return createErrorView("Failed to load video ad: " + e.getMessage());
        }
    }

    @Override
    public View getNativeAdView(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null)
            callback.onAdLoading(placementId);

        try {
            NativeAdType nativeAdType = new NativeAdType(placementId);
            String url = nativeAdType.buildRequestUrl(deviceInfo).toString();

            return adDisplayManager.getNativeAdView(placementId, url, callback);

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to get native ad view: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, AdErrorCode.fromException(e),
                        "Failed to get native ad view: " + AdErrorCode.messageFor(e));
            }
            return createErrorView("Failed to load native ad: " + e.getMessage());
        }
    }

    @Override
    public void setAdPosition(AdPosition position) {
        checkInitialization();
        if (adDisplayManager != null) {
            adDisplayManager.setAdPosition(position);
            SDKLogger.d(TAG, "Ad position set to: " + position.getDisplayName());
        }
    }

    @Override
    public AdPosition getCurrentAdPosition() {
        checkInitialization();
        if (adDisplayManager != null) {
            return adDisplayManager.getCurrentAdPosition();
        }
        return AdPosition.UNKNOWN;
    }

    @Override
    public AdPosition getEffectiveAdPosition() {
        if (adDisplayManager != null) {
            return adDisplayManager.getEffectiveAdPosition();
        }
        return AdPosition.UNKNOWN;
    }

    @Override
    public AdPosition getResponseAdPosition() {
        if (adDisplayManager != null) {
            return adDisplayManager.getResponseAdPosition();
        }
        return AdPosition.UNKNOWN;
    }

    @Override
    public void cleanup() {
        if (adDisplayManager != null) {
            adDisplayManager.clearPreloadCache();
            adDisplayManager.cleanup();
        }
        isInitialized = false;
        deviceInfoProvider = null;
        synchronized (pendingInitCallbacks) {
            pendingInitCallbacks.clear();
        }
        SDKLogger.d(TAG, "SDK cleaned up");
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void requestConsentInfoUpdate(ConsentCallback callback) {
        checkInitialization();
        Activity activity = resolveConsentActivity();
        if (activity != null) {
            consentManager.requestConsentInfoUpdate(
                    activity,
                    () -> deviceInfoProvider.getDeviceInfoWithCurrentConsent(newDeviceInfo -> {
                        deviceInfo = newDeviceInfo;
                        if (callback != null) {
                            callback.onConsentInfoUpdated();
                        }
                    }));
        } else {
            SDKLogger.e(TAG, "No Activity available for consent info update");
            if (callback != null) {
                callback.onConsentInfoUpdateFailed(new Exception("No Activity available for consent"));
            }
        }
    }

    @Override
    public void showConsentForm(ConsentCallback callback) {
        checkInitialization();
        Activity activity = resolveConsentActivity();
        if (activity != null) {
            consentManager.loadAndShowConsentForm(
                    activity,
                    formError -> {
                        SDKLogger.e(TAG, "Consent form error: " + formError.getMessage());
                        if (callback != null) {
                            callback.onConsentFormError(new Exception(formError.getMessage()));
                        }
                    });
        } else {
            SDKLogger.e(TAG, "No Activity available for consent form");
            if (callback != null) {
                callback.onConsentFormError(new Exception("No Activity available for consent form"));
            }
        }
    }

    private Activity resolveConsentActivity() {
        if (adDisplayManager != null) {
            Activity displayActivity = adDisplayManager.getDisplayActivity();
            if (displayActivity != null) {
                return displayActivity;
            }
        }
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }

    @Override
    public boolean isConsentRequired() {
        return consentManager != null && consentManager.isConsentRequired();
    }

    @Override
    public boolean hasAdsConsent() {
        return consentManager != null && consentManager.hasAdsConsent();
    }

    @Override
    public boolean hasAnalyticsConsent() {
        return consentManager != null && consentManager.hasAnalyticsConsent();
    }

    @Override
    public String getConsentStatusSummary() {
        checkInitialization();
        return consentManager.getConsentSummary();
    }

    @Override
    public void resetConsent() {
        checkInitialization();
        consentManager.resetConsent();
        SDKLogger.d(TAG, "Consent information reset");
    }

    @Override
    public void enableConsentDebugMode(String deviceId) {
        checkInitialization();
        SDKLogger.d(TAG, "Consent debug mode enabled for device: " + deviceId);
    }

    /**
     * Set mock consent data for testing purposes
     * This is useful for testing Polish region scenarios
     */
    public void setMockConsentData(boolean gdprApplies, String gdprConsent, String additionalConsent, String gppString,
                                   String usPrivacy) {
        checkInitialization();
        consentManager.setMockConsentData(gdprApplies, gdprConsent, additionalConsent, gppString, usPrivacy);
    }

    /**
     * Set Polish region test consent data
     */
    public void setPolishTestConsentData(String testCase) {
        checkInitialization();
        consentManager.setPolishTestConsentData(testCase);
    }

    private void checkInitialization() {
        if (!isInitialized) {
            throw new IllegalStateException("SDK not initialized. Call initialize() first.");
        }
    }

    private View createErrorView(String errorMessage) {
        TextView errorView = new TextView(context);
        errorView.setText(errorMessage);
        errorView.setTextColor(0xFFFF0000);
        errorView.setPadding(16, 16, 16, 16);
        errorView.setBackgroundColor(0xFFFFE0E0);
        return errorView;
    }

    public SDKConfig getConfig() {
        return config;
    }

    public void setConfig(SDKConfig config) {
        this.config = config;
    }
}
