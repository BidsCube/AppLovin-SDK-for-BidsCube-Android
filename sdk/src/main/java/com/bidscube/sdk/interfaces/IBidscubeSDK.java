package com.bidscube.sdk.interfaces;

import android.content.Context;

import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.models.enums.AdPosition;

import android.view.View;

/**
 * Main interface for Bidscube SDK
 * This interface provides all the methods needed by external applications
 * to initialize and use the SDK for displaying ads
 */
public interface IBidscubeSDK {

    /**
     * Initialize the SDK with required parameters
     *
     * @param context Application context
     * @param config  SDK configuration object
     */
    void initialize(Context context, SDKConfig config);

    /**
     * Show image ad - display mode determined by response position
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     */
    void showImageAd(String placementId, AdCallback callback);

    /**
     * Show video ad - display mode determined by response position
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     */
    void showVideoAd(String placementId, AdCallback callback);

    void showSkippableVideoAd(String placementId, AdCallback callback);

    /**
     * Show native ad - display mode determined by response position
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     */
    void showNativeAd(String placementId, AdCallback callback);

    /**
     * Get image ad view for integration into layouts (no dialog)
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     * @return View that can be added to any layout
     */
    View getImageAdView(String placementId, AdCallback callback);

    /**
     * Get video ad view for integration into layouts (no dialog)
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     * @return View that can be added to any layout
     */
    View getVideoAdView(String placementId, AdCallback callback);

    /**
     * Get native ad view for integration into layouts (no dialog)
     *
     * @param placementId Ad placement ID
     * @param callback    Callback for ad events
     * @return View that can be added to any layout
     */
    View getNativeAdView(String placementId, AdCallback callback);

    /**
     * Set ad position for windowed ads
     *
     * @param position Ad position (ABOVE_THE_FOLD, BELOW_THE_FOLD, HEADER, FOOTER,
     *                 SIDEBAR, UNKNOWN)
     */
    void setAdPosition(AdPosition position);

    /**
     * Get current ad position (manual override)
     *
     * @return Current ad position
     */
    AdPosition getCurrentAdPosition();

    /**
     * Get effective ad position (response position takes precedence)
     *
     * @return Effective ad position
     */
    AdPosition getEffectiveAdPosition();

    /**
     * Get response ad position
     *
     * @return Response ad position
     */
    AdPosition getResponseAdPosition();

    /**
     * Clean up SDK resources
     */
    void cleanup();

    /**
     * Check if SDK is initialized
     *
     * @return true if SDK is initialized, false otherwise
     */
    boolean isInitialized();

    /**
     * Request consent information update
     * This should be called before showing ads to ensure compliance
     *
     * @param callback Callback for consent information update events
     */
    void requestConsentInfoUpdate(ConsentCallback callback);

    /**
     * Show consent form to user
     * This should be called when consent is required
     *
     * @param callback Callback for consent form events
     */
    void showConsentForm(ConsentCallback callback);

    /**
     * Check if consent is required for the current user
     *
     * @return true if consent is required, false otherwise
     */
    boolean isConsentRequired();

    /**
     * Check if user has given consent for ads
     *
     * @return true if user has consented to ads, false otherwise
     */
    boolean hasAdsConsent();

    /**
     * Check if user has given consent for analytics
     *
     * @return true if user has consented to analytics, false otherwise
     */
    boolean hasAnalyticsConsent();

    /**
     * Get current consent status summary
     *
     * @return String representation of current consent status
     */
    String getConsentStatusSummary();

    /**
     * Reset consent information (for testing purposes)
     */
    void resetConsent();

    /**
     * Enable debug mode for consent testing
     *
     * @param deviceId Device ID for debug mode
     */
    void enableConsentDebugMode(String deviceId);
}
