package com.bidscube.sdk;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.interfaces.ConsentCallback;
import com.bidscube.sdk.interfaces.InitializationCallback;
import com.bidscube.sdk.interfaces.IBidscubeSDK;
import com.bidscube.sdk.models.enums.AdPosition;
import com.bidscube.sdk.utils.SDKLogger;
import com.bidscube.sdk.utils.SignalCollector;

/**
 * Main entry point for Bidscube SDK
 * This class provides a simple static interface for external applications
 * to initialize and use the SDK
 */
public class BidscubeSDK {

    private static final String TAG = "BidscubeSDK";
    private static IBidscubeSDK sdkInstance;
    private static volatile String mediationAdapterVersion = "";

    public static String getMediationAdapterVersion() {
        return mediationAdapterVersion != null ? mediationAdapterVersion : "";
    }

    /**
     * Local runtime diagnostics (no network, no PII). See {@link com.bidscube.sdk.doctor.SdkDoctorReport}.
     */
    public static com.bidscube.sdk.doctor.SdkDoctorReport runSdkDoctor(Context context) {
        return com.bidscube.sdk.doctor.SdkDoctor.run(context,
                com.bidscube.sdk.doctor.SdkDoctorOptions.defaults());
    }

    /**
     * Local runtime diagnostics with options (e.g. MAX mediation expected).
     */
    public static com.bidscube.sdk.doctor.SdkDoctorReport runSdkDoctor(Context context,
            com.bidscube.sdk.doctor.SdkDoctorOptions options) {
        return com.bidscube.sdk.doctor.SdkDoctor.run(context, options);
    }

    /**
     * Sets the mediation adapter version included in {@link #collectSignal()}.
     * MAX adapters should call this during initialization.
     */
    public static void setMediationAdapterVersion(String adapterVersion) {
        mediationAdapterVersion = adapterVersion != null ? adapterVersion : "";
    }

    /**
     * Minimal integration: default production SSP host, auto-detected app id/name/version/UA from the host app.
     * For custom SSP host, logging, or consent overrides use {@link #initialize(Context, SDKConfig)}.
     */
    public static void initialize(Context context) {
        initialize(context, new SDKConfig.Builder(context.getApplicationContext()).build());
    }

    /**
     * Initialize the SDK with required parameters
     *
     * @param context Application context
     * @param config  SDK configuration object
     */
    public static void initialize(Context context, SDKConfig config) {
        initialize(context, config, null);
    }

    /**
     * Initialize the SDK with required parameters and an optional readiness callback.
     */
    public static void initialize(Context context, SDKConfig config, InitializationCallback callback) {
        if (sdkInstance != null) {
            if (sdkInstance.isInitialized()) {
                SDKLogger.w(TAG, "SDK already initialized");
                if (callback != null) {
                    callback.onInitialized();
                }
            } else {
                sdkInstance.registerInitializationCallback(callback);
            }
            return;
        }

        try {
            sdkInstance = new BidscubeSDKImpl();
            sdkInstance.initialize(context, config, callback);
            SDKLogger.d(TAG, "SDK initialize invoked");
        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to initialize SDK: " + e.getMessage(), e);
            if (callback != null) {
                callback.onInitializationFailed(e.getMessage() != null ? e.getMessage() : "SDK initialization failed");
            }
            throw new RuntimeException("SDK initialization failed", e);
        }
    }

    /**
     * Binds the Activity used for dialog / full-screen ad presentation.
     * Required when init used Application context (typical for MAX mediation).
     */
    public static void setDisplayActivity(Activity activity) {
        if (sdkInstance != null) {
            sdkInstance.setDisplayActivity(activity);
        }
    }

    /**
     * Sets the publisher user id sent as {@code user_id} on SSP ad requests.
     * May be set in {@link SDKConfig.Builder#userId(String)} at init or updated later (e.g. after login).
     */
    public static void setUserId(String userId) {
        if (sdkInstance != null) {
            sdkInstance.setUserId(userId);
        }
    }

    /**
     * Show image ad - display mode determined by response position
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     */
    public static void showImageAd(String placementId, AdCallback callback) {
        checkInitialization();
        sdkInstance.showImageAd(placementId, callback);
    }

    /**
     * Show video ad - display mode determined by response position
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     */
    public static void showVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        sdkInstance.showVideoAd(placementId, callback);
    }

    /**
     * Show a fullscreen video ad directly from inline VAST markup (no network). For QA / debug use.
     */
    public static void showVideoAdFromVastMarkup(String placementId, String vastXml, AdCallback callback) {
        checkInitialization();
        sdkInstance.showVideoAdFromVastMarkup(placementId, vastXml, callback);
    }

    /**
     * Interstitial VAST (no reward).
     */
    public static void showInterstitialVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        sdkInstance.showInterstitialVideoAd(placementId, callback);
    }

    /**
     * Rewarded VAST; reward only after full completion.
     */
    public static void showRewardedVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        sdkInstance.showRewardedVideoAd(placementId, callback);
    }

    /**
     * Preload an interstitial video ad. Call from mediation load; show consumes the cache.
     */
    public static void preloadInterstitialVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        sdkInstance.preloadInterstitialVideoAd(placementId, callback);
    }

    /**
     * Preload a rewarded video ad. Call from mediation load; show consumes the cache.
     */
    public static void preloadRewardedVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        sdkInstance.preloadRewardedVideoAd(placementId, callback);
    }

    /**
     * Preload a static image ad. Call from mediation load; show consumes the cache.
     */
    public static void preloadImageAd(String placementId, AdCallback callback) {
        checkInitialization();
        sdkInstance.preloadImageAd(placementId, callback);
    }

    /**
     * Clears cached preload responses.
     */
    public static void clearPreloadCache() {
        if (sdkInstance != null) {
            sdkInstance.clearPreloadCache();
        }
    }

    /**
     * Collect a structured mediation signal without PII or raw IFA.
     */
    public static String collectSignal() {
        return SignalCollector.collectSignal(getSdkVersion(), mediationAdapterVersion);
    }

    /**
     * Collect a structured mediation signal with an explicit adapter version.
     */
    public static String collectSignal(String adapterVersion) {
        return SignalCollector.collectSignal(getSdkVersion(), adapterVersion);
    }

    /**
     * Show skippable video ad - display mode determined by response position
     *
     * @param placementId       Ad placement ID
     * @param callback          Callback for ad events
     */
    @Deprecated
    public static void showSkippableVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        sdkInstance.showSkippableVideoAd(placementId, callback);
    }

    /**
     * Show native ad - display mode determined by response position
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     */
    public static void showNativeAd(String placementId, AdCallback callback) {
        checkInitialization();
        sdkInstance.showNativeAd(placementId, callback);
    }

    /**
     * Get image ad view for integration into layouts (no dialog)
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     * @return View that can be added to any layout
     */
    public static View getImageAdView(String placementId, AdCallback callback) {
        checkInitialization();
        return sdkInstance.getImageAdView(placementId, callback);
    }

    /**
     * Get video ad view for integration into layouts (no dialog)
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     * @return View that can be added to any layout
     */
    public static View getVideoAdView(String placementId, AdCallback callback) {
        checkInitialization();
        return sdkInstance.getVideoAdView(placementId, callback);
    }

    /**
     * Get native ad view for integration into layouts (no dialog)
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     * @return View that can be added to any layout
     */
    public static View getNativeAdView(String placementId, AdCallback callback) {
        checkInitialization();
        return sdkInstance.getNativeAdView(placementId, callback);
    }

    /**
     * Set ad position for windowed ads
     *
     * @param position Ad position (ABOVE_THE_FOLD, BELOW_THE_FOLD, HEADER, FOOTER,
     *                 SIDEBAR, UNKNOWN)
     */
    public static void setAdPosition(AdPosition position) {
        checkInitialization();
        sdkInstance.setAdPosition(position);
    }

    /**
     * Get current ad position (manual override)
     *
     * @return Current ad position
     */
    public static AdPosition getCurrentAdPosition() {
        checkInitialization();
        return sdkInstance.getCurrentAdPosition();
    }

    /**
     * Get effective ad position (response position takes precedence)
     *
     * @return Effective ad position
     */
    public static AdPosition getEffectiveAdPosition() {
        checkInitialization();
        return sdkInstance.getEffectiveAdPosition();
    }

    /**
     * Get response ad position
     *
     * @return Response ad position
     */
    public static AdPosition getResponseAdPosition() {
        checkInitialization();
        return sdkInstance.getResponseAdPosition();
    }

    /**
     * Clean up SDK resources
     */
    public static void cleanup() {
        if (sdkInstance != null) {
            sdkInstance.cleanup();
            sdkInstance = null;
        }
    }

    /**
     * Check if SDK is initialized
     *
     * @return true if SDK is initialized, false otherwise
     */
    public static boolean isInitialized() {
        return sdkInstance != null && sdkInstance.isInitialized();
    }

    /**
     * Request consent information update
     * This should be called before showing ads to ensure compliance
     *
     * @param callback Callback for consent information update events
     */
    public static void requestConsentInfoUpdate(ConsentCallback callback) {
        checkInitialization();
        sdkInstance.requestConsentInfoUpdate(callback);
    }

    /**
     * Show consent form to user
     * This should be called when consent is required
     *
     * @param callback Callback for consent form events
     */
    public static void showConsentForm(ConsentCallback callback) {
        checkInitialization();
        sdkInstance.showConsentForm(callback);
    }

    /**
     * Check if consent is required for the current user
     *
     * @return true if consent is required, false otherwise
     */
    public static boolean isConsentRequired() {
        checkInitialization();
        return sdkInstance.isConsentRequired();
    }

    /**
     * Check if user has given consent for ads
     *
     * @return true if user has consented to ads, false otherwise
     */
    public static boolean hasAdsConsent() {
        checkInitialization();
        return sdkInstance.hasAdsConsent();
    }

    /**
     * Check if user has given consent for analytics
     *
     * @return true if user has consented to analytics, false otherwise
     */
    public static boolean hasAnalyticsConsent() {
        checkInitialization();
        return sdkInstance.hasAnalyticsConsent();
    }

    /**
     * Get current consent status summary
     *
     * @return String representation of current consent status
     */
    public static String getConsentStatusSummary() {
        checkInitialization();
        return sdkInstance.getConsentStatusSummary();
    }

    /**
     * Reset consent information (for testing purposes)
     */
    public static void resetConsent() {
        checkInitialization();
        sdkInstance.resetConsent();
    }

    /**
     * Enable debug mode for consent testing
     *
     * @param deviceId Device ID for debug mode
     */
    public static void enableConsentDebugMode(String deviceId) {
        checkInitialization();
        sdkInstance.enableConsentDebugMode(deviceId);
    }

    /** Version of this Bidscube SDK artifact (from build; same as published Maven version). */
    public static String getSdkVersion() {
        return com.bidscube.sdk.BuildConfig.SDK_VERSION_NAME;
    }

    private static void checkInitialization() {
        if (sdkInstance == null || !sdkInstance.isInitialized()) {
            throw new IllegalStateException("SDK not initialized. Call BidscubeSDK.initialize() first.");
        }
    }
}
