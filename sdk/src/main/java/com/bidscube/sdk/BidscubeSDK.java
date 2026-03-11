package com.bidscube.sdk;

import android.content.Context;
import android.view.View;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.interfaces.ConsentCallback;
import com.bidscube.sdk.interfaces.IBidscubeSDK;
import com.bidscube.sdk.models.enums.AdPosition;
import com.bidscube.sdk.utils.SDKLogger;

/**
 * Main entry point for Bidscube SDK
 * This class provides a simple static interface for external applications
 * to initialize and use the SDK
 */
public class BidscubeSDK {

    private static final String TAG = "BidscubeSDK";
    private static IBidscubeSDK sdkInstance;

    /**
     * Initialize the SDK with required parameters
     *
     * @param context Application context
     * @param config  SDK configuration object
     */
    @OptIn(markerClass = UnstableApi.class)
    public static void initialize(Context context, SDKConfig config) {
        if (sdkInstance != null) {
            SDKLogger.w(TAG, "SDK already initialized");
            return;
        }

        try {
            sdkInstance = new BidscubeSDKImpl();
            sdkInstance.initialize(context, config);
            SDKLogger.d(TAG, "SDK initialized successfully");
        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to initialize SDK: " + e.getMessage(), e);
            throw new RuntimeException("SDK initialization failed", e);
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

    private static void checkInitialization() {
        if (sdkInstance == null || !sdkInstance.isInitialized()) {
            throw new IllegalStateException("SDK not initialized. Call BidscubeSDK.initialize() first.");
        }
    }
}
