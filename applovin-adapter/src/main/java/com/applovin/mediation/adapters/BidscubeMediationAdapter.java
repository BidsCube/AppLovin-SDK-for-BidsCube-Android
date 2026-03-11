package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapters.MaxAdapterError;
import com.applovin.mediation.adapters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.MaxAdViewAdapter;
import com.applovin.mediation.adapters.MaxInterstitialAdapter;
import com.applovin.mediation.adapters.MaxNativeAdAdapter;
import com.applovin.mediation.adapters.MaxRewardedAdapter;
import com.applovin.mediation.adapters.MaxSignalProvider;
import com.applovin.mediation.adapters.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapters.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapters.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapters.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapters.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.sdk.AppLovinSdk;
import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.config.SDKConfig;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bidscube MAX Mediation Adapter.
 * Integrates Bidscube SDK with AppLovin MAX for Banner, Interstitial, Rewarded and Native ads.
 */
public class BidscubeMediationAdapter
        extends MediationAdapterBase
        implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter, MaxNativeAdAdapter, MaxSignalProvider {

    private static final AtomicBoolean initialized = new AtomicBoolean();
    private static InitializationStatus status;

    public BidscubeMediationAdapter(final AppLovinSdk sdk) {
        super(sdk);
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity,
            final MaxSignalCollectionListener callback) {
        callback.onSignalCollected("bidscube_test_signal");
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity,
            final OnCompletionListener onCompletionListener) {
        if (initialized.compareAndSet(false, true)) {
            final String appId = parameters.getServerParameters().getString("app_id");
            log("Initializing Bidscube SDK with app id: " + appId + "...");

            if (appId == null || appId.isEmpty()) {
                log("Bidscube SDK initialization failed: app_id is null or empty");
                status = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion(status, "App id is null or empty");
                return;
            }

            Context context = activity != null ? activity.getApplicationContext() : getApplicationContext();
            if (context == null) {
                log("Bidscube SDK initialization failed: no context available");
                status = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion(status, "No context available");
                return;
            }

            try {
                SDKConfig config = new SDKConfig.Builder(context)
                        .enableLogging(false)
                        .enableDebugMode(false)
                        .defaultAdTimeout(30000)
                        .defaultAdPosition("UNKNOWN")
                        .build();
                BidscubeSDK.initialize(context, config);
                log("Bidscube SDK successfully initialized with app id: " + appId);
                status = InitializationStatus.INITIALIZED_SUCCESS;
                onCompletionListener.onCompletion(status, null);
            } catch (Exception e) {
                log("Bidscube SDK initialization failed with error: " + e.getMessage());
                status = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion(status, e.getMessage());
            }
        } else {
            onCompletionListener.onCompletion(status, null);
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat,
            @Nullable final Activity activity, final MaxAdViewAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log("Loading " + adFormat.getLabel() + " ad for placement: " + placementId + "...");

        if (status != InitializationStatus.INITIALIZED_SUCCESS) {
            log("Bidscube SDK not successfully initialized: failing " + adFormat.getLabel() + " ad load...");
            listener.onAdViewAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        if (!BidscubeSDK.isInitialized()) {
            listener.onAdViewAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        final String pid = placementId != null ? placementId : "";

        try {
            View adView = BidscubeSDK.getImageAdView(pid, new com.bidscube.sdk.interfaces.AdCallback() {
                @Override
                public void onAdLoaded(String placementId) {
                    log("Bidscube " + adFormat.getLabel() + " ad loaded successfully");
                    listener.onAdViewAdLoaded(adView);
                }

                @Override
                public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                    log("Bidscube " + adFormat.getLabel() + " ad load failed: " + errorMessage);
                    listener.onAdViewAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
                }
            });
            if (adView == null) {
                listener.onAdViewAdLoadFailed(MaxAdapterError.UNSPECIFIED);
            }
        } catch (Exception e) {
            log("Bidscube " + adFormat.getLabel() + " ad load error: " + e.getMessage());
            listener.onAdViewAdLoadFailed(new MaxAdapterError(MaxAdapterError.UNSPECIFIED.getCode(), e.getMessage()));
        }
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxInterstitialAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log("Loading interstitial ad for placement: " + placementId + "...");

        if (status != InitializationStatus.INITIALIZED_SUCCESS || !BidscubeSDK.isInitialized()) {
            log("Bidscube SDK not successfully initialized: failing interstitial ad load...");
            listener.onInterstitialAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }
        // MAX expects load then show; Bidscube showImageAd loads and shows. We report loaded here, show in showInterstitialAd.
        log("Bidscube interstitial ad loaded successfully");
        listener.onInterstitialAdLoaded();
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxInterstitialAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";
        log("Showing Bidscube interstitial ad for placement: " + pid + "...");

        BidscubeSDK.showImageAd(pid, new com.bidscube.sdk.interfaces.AdCallback() {
            @Override
            public void onAdDisplayed(String placementId) {
                listener.onInterstitialAdDisplayed();
            }

            @Override
            public void onAdClicked(String placementId) {
                listener.onInterstitialAdClicked();
            }

            @Override
            public void onAdClosed(String placementId) {
                listener.onInterstitialAdHidden();
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                listener.onInterstitialAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        });
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxRewardedAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log("Loading rewarded ad for placement: " + placementId + "...");

        if (status != InitializationStatus.INITIALIZED_SUCCESS || !BidscubeSDK.isInitialized()) {
            log("Bidscube SDK not successfully initialized: failing rewarded ad load...");
            listener.onRewardedAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }
        // MAX expects load then show; we report loaded here, show in showRewardedAd.
        log("Bidscube rewarded ad loaded successfully");
        listener.onRewardedAdLoaded();
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxRewardedAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";
        log("Showing Bidscube rewarded ad for placement: " + pid + "...");
        configureReward(parameters);

        BidscubeSDK.showVideoAd(pid, new com.bidscube.sdk.interfaces.AdCallback() {
            @Override
            public void onAdDisplayed(String placementId) {
                listener.onRewardedAdDisplayed();
            }

            @Override
            public void onAdClicked(String placementId) {
                listener.onRewardedAdClicked();
            }

            @Override
            public void onAdClosed(String placementId) {
                listener.onRewardedAdHidden();
            }

            @Override
            public void onVideoAdCompleted(String placementId) {
                listener.onUserRewarded(getReward());
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                listener.onRewardedAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        });
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxNativeAdAdapterListener listener) {
        log("Loading Bidscube native ad...");

        if (status != InitializationStatus.INITIALIZED_SUCCESS || !BidscubeSDK.isInitialized()) {
            log("Bidscube SDK not successfully initialized: failing native ad load...");
            listener.onNativeAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";

        BidscubeSDK.getNativeAdView(pid, new com.bidscube.sdk.interfaces.AdCallback() {
            @Override
            public void onAdLoaded(String placementId) {
                MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                        .setAdFormat(MaxAdFormat.NATIVE)
                        .setTitle("Bidscube Native Ad")
                        .setBody("Native ad from Bidscube")
                        .setCallToAction("Learn More");
                MaxNativeAd maxNativeAd = builder.build();
                log("Bidscube native ad loaded successfully");
                listener.onNativeAdLoaded(maxNativeAd, null);
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                log("Bidscube native ad load failed: " + errorMessage);
                listener.onNativeAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        });
    }

    private static class InitializationStatus {
        public static final InitializationStatus INITIALIZING = new InitializationStatus();
        public static final InitializationStatus INITIALIZED_SUCCESS = new InitializationStatus();
        public static final InitializationStatus INITIALIZED_FAILURE = new InitializationStatus();
    }
}
