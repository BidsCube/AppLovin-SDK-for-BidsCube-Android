
package com.bidscube.sdk;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.media3.common.util.UnstableApi;

import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.consent.ConsentManager;
import com.bidscube.sdk.device.providers.DeviceInfoProvider;
import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.interfaces.ConsentCallback;
import com.bidscube.sdk.interfaces.IBidscubeSDK;
import com.bidscube.sdk.models.DeviceInfo;
import com.bidscube.sdk.models.enums.AdPosition;
import com.bidscube.sdk.ads.ImageAdType;
import com.bidscube.sdk.ads.VideoAdType;
import com.bidscube.sdk.ads.NativeAdType;
import com.bidscube.sdk.utils.SDKLogger;

/**
 * Main implementation of Bidscube SDK
 * This class handles all ad operations and provides a clean interface for
 * external applications
 */
@UnstableApi
public class BidscubeSDKImpl implements IBidscubeSDK {

    private static final String TAG = "BidscubeSDKImpl";

    private Context context;
    private SDKConfig config;
    private DeviceInfo deviceInfo;
    private AdDisplayManager adDisplayManager;
    private DeviceInfoProvider deviceInfoProvider;
    private ConsentManager consentManager;
    private boolean isInitialized = false;

    @Override
    public void initialize(Context context, SDKConfig config) {
        if (isInitialized) {
            SDKLogger.w(TAG, "SDK already initialized");
            return;
        }

        this.context = context;
        this.config = config;

        // Configure logging based on SDKConfig
        SDKLogger.setLoggingEnabled(config.isEnableLogging());
        SDKLogger.setDefaultTag(TAG);

        try {

            deviceInfoProvider = new DeviceInfoProvider(context, config);
            consentManager = deviceInfoProvider.getConsentManager();

            deviceInfoProvider.getDeviceInfoAsync(deviceInfo -> {
                this.deviceInfo = deviceInfo;

                this.adDisplayManager = new AdDisplayManager(context, deviceInfo);

                this.isInitialized = true;
                SDKLogger.d(TAG, "SDK initialized successfully");

                if (config.getDefaultAdPosition() != null) {
                    AdPosition position = AdPosition.fromString(config.getDefaultAdPosition());
                    setAdPosition(position);
                }
            });

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to initialize SDK: " + e.getMessage(), e);
            throw new RuntimeException("SDK initialization failed", e);
        }
    }

    @Override
    public void showImageAd(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null)
            callback.onAdLoading(placementId);

        try {
            ImageAdType imageAdType = new ImageAdType(placementId);
            String url = imageAdType.buildRequestUrl(deviceInfo).toString();

            adDisplayManager.showImageAdWithResponsePosition(placementId, url, callback);

            if (callback != null) {
                callback.onAdLoaded(placementId);
                callback.onAdDisplayed(placementId);
            }

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to show image ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, -1, "Failed to show image ad: " + e.getMessage());
            }
        }
    }

    @Override
    public void showVideoAd(String placementId, AdCallback callback) {
        checkInitialization();
        if (callback != null)
            callback.onAdLoading(placementId);

        try {
            VideoAdType videoAdType = new VideoAdType(placementId);
            String url = videoAdType.buildRequestUrl(deviceInfo).toString();

            adDisplayManager.showVideoAdWithResponsePosition(placementId, url, callback);

            if (callback != null) {
                callback.onAdLoaded(placementId);
                callback.onAdDisplayed(placementId);
                callback.onVideoAdStarted(placementId);
            }

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to show video ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, -1, "Failed to show video ad: " + e.getMessage());
            }
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

            if (callback != null) {
                callback.onAdLoaded(placementId);
                callback.onAdDisplayed(placementId);
            }

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to show native ad: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, -1, "Failed to show native ad: " + e.getMessage());
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

            View adView = adDisplayManager.getImageAdView(placementId, url, callback);

            if (callback != null) {
                callback.onAdLoaded(placementId);
                callback.onAdDisplayed(placementId);
            }

            return adView;

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to get image ad view: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, -1, "Failed to get image ad view: " + e.getMessage());
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

            View adView = adDisplayManager.getVideoAdView(placementId, url, callback);

            if (callback != null) {
                callback.onAdLoaded(placementId);
                callback.onAdDisplayed(placementId);
                callback.onVideoAdStarted(placementId);
            }

            return adView;

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to get video ad view: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, -1, "Failed to get video ad view: " + e.getMessage());
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

            View adView = adDisplayManager.getNativeAdView(placementId, url, callback);

            if (callback != null) {
                callback.onAdLoaded(placementId);
                callback.onAdDisplayed(placementId);
            }

            return adView;

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to get native ad view: " + e.getMessage(), e);
            if (callback != null) {
                callback.onAdFailed(placementId, -1, "Failed to get native ad view: " + e.getMessage());
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
            adDisplayManager.cleanup();
        }
        isInitialized = false;
        SDKLogger.d(TAG, "SDK cleaned up");
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void requestConsentInfoUpdate(ConsentCallback callback) {
        checkInitialization();
        if (context instanceof Activity) {
            consentManager.requestConsentInfoUpdate(
                    (Activity) context,
                    () -> deviceInfoProvider.getDeviceInfoWithCurrentConsent(newDeviceInfo -> {
                        deviceInfo = newDeviceInfo;
                        if (callback != null) {
                            callback.onConsentInfoUpdated();
                        }
                    }));
        } else {
            SDKLogger.e(TAG, "Context is not an Activity, cannot request consent info update");
            if (callback != null) {
                callback.onConsentInfoUpdateFailed(new Exception("Context is not an Activity"));
            }
        }
    }

    @Override
    public void showConsentForm(ConsentCallback callback) {
        checkInitialization();
        if (context instanceof Activity) {
            consentManager.loadAndShowConsentForm(
                    (Activity) context,
                    formError -> {
                        SDKLogger.e(TAG, "Consent form error: " + formError.getMessage());
                        if (callback != null) {
                            callback.onConsentFormError(new Exception(formError.getMessage()));
                        }
                    });
        } else {
            SDKLogger.e(TAG, "Context is not an Activity, cannot show consent form");
            if (callback != null) {
                callback.onConsentFormError(new Exception("Context is not an Activity"));
            }
        }
    }

    @Override
    public boolean isConsentRequired() {
        return false;
    }

    @Override
    public boolean hasAdsConsent() {
        return false;
    }

    @Override
    public boolean hasAnalyticsConsent() {
        return false;
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
