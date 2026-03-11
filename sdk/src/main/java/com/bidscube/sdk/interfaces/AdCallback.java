package com.bidscube.sdk.interfaces;

import com.bidscube.sdk.models.AdRenderContext;

/**
 * Callback interface for ad events
 * Implement this interface to handle ad loading, display, and user interaction events
 */
public interface AdCallback {
    
    /**
     * Called when ad starts loading
     * @param placementId The placement ID of the ad being loaded
     */
    void onAdLoading(String placementId);
    
    /**
     * Called when ad is successfully loaded and ready to display
     * @param placementId The placement ID of the ad that was loaded
     */
    void onAdLoaded(String placementId);
    
    /**
     * Called when ad is displayed to the user
     * @param placementId The placement ID of the ad being displayed
     */
    void onAdDisplayed(String placementId);
    
    /**
     * Called when ad is clicked by the user
     * @param placementId The placement ID of the ad that was clicked
     */
    void onAdClicked(String placementId);
    
    /**
     * Called when ad is closed by the user
     * @param placementId The placement ID of the ad that was closed
     */
    void onAdClosed(String placementId);
    
    /**
     * Called when ad fails to load
     * @param placementId The placement ID of the ad that failed
     * @param errorCode Error code indicating the type of failure
     * @param errorMessage Human-readable error message
     */
    void onAdFailed(String placementId, int errorCode, String errorMessage);
    
    /**
     * Called after an ad response is received but before the SDK renders it.
     * Return {@code true} to indicate that you have handled rendering yourself;
     * the SDK will skip its default UI in that case.
     * Use this method for all ad types (HTML ADM or parsed native model). For native
     * ads the parsed model (if available) will be available via
     * {@link AdRenderContext#getNativeAd()}.
     *
     * @param context Metadata describing the response (placement, ADM, position, type)
     * @return {@code true} if the host app handled rendering, otherwise {@code false}
     */
    default boolean onAdRenderOverride(AdRenderContext context) { return false; }

    /**
     * Called when video ad starts playing
     * @param placementId The placement ID of the video ad
     */
    default void onVideoAdStarted(String placementId) {}
    
    /**
     * Called when video ad completes playing
     * @param placementId The placement ID of the video ad
     */
    default void onVideoAdCompleted(String placementId) {}
    
    /**
     * Called when video ad is skipped by the user
     * @param placementId The placement ID of the video ad
     */
    default void onVideoAdSkipped(String placementId) {}
    
    /**
     * Called when skippable video ad becomes skippable (after 5 seconds)
     * @param placementId The placement ID of the video ad
     */
    default void onVideoAdSkippable(String placementId) {}
    
    /**
     * Called when install button is clicked in skippable video ad
     * @param placementId The placement ID of the video ad
     * @param buttonText The text of the button that was clicked
     */
    default void onInstallButtonClicked(String placementId, String buttonText) {}
}
