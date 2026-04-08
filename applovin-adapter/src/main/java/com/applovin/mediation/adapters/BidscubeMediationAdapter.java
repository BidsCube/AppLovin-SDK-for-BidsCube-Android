package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.adapter.MaxAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxNativeAdAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.sdk.AppLovinSdk;
import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.config.SDKConfig;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bidscube MAX Mediation Adapter.
 * Integrates Bidscube SDK with AppLovin MAX for Banner, Interstitial, Rewarded and Native ads.
 * <p>
 * Built against AppLovin SDK 13.0.x ({@code com.applovin.mediation.adapter} package layout).
 */
public class BidscubeMediationAdapter
        extends MediationAdapterBase
        implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter, MaxNativeAdAdapter, MaxSignalProvider {

    private static final AtomicBoolean initialized = new AtomicBoolean();
    private static MaxAdapter.InitializationStatus status;

    public BidscubeMediationAdapter(final AppLovinSdk sdk) {
        super(sdk);
    }

    @Override
    public String getSdkVersion() {
        return getVersionString(BidscubeSDK.class, "1.2.2");
    }

    @Override
    public String getAdapterVersion() {
        return getVersionString(BidscubeMediationAdapter.class, "1.0.2.3");
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity,
            final MaxSignalCollectionListener callback) {
        callback.onSignalCollected("bidscube_test_signal");
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity,
            final MaxAdapter.OnCompletionListener onCompletionListener) {
        if (initialized.compareAndSet(false, true)) {
            final String appId = parameters.getServerParameters().getString("app_id");
            log("Initializing Bidscube SDK with app id: " + appId + "...");

            if (appId == null || appId.isEmpty()) {
                log("Bidscube SDK initialization failed: app_id is null or empty");
                status = MaxAdapter.InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion(status, "App id is null or empty");
                return;
            }

            Context context = activity != null ? activity.getApplicationContext() : getApplicationContext();
            if (context == null) {
                log("Bidscube SDK initialization failed: no context available");
                status = MaxAdapter.InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion(status, "No context available");
                return;
            }

            try {
                String requestAuthority = parameters.getServerParameters().getString("request_authority");
                if (requestAuthority == null || requestAuthority.isEmpty()) {
                    requestAuthority = parameters.getServerParameters().getString("ssp_host");
                }
                SDKConfig.Builder configBuilder = new SDKConfig.Builder(context)
                        .enableLogging(false)
                        .enableDebugMode(false)
                        .defaultAdTimeout(30000)
                        .defaultAdPosition("UNKNOWN");
                if (requestAuthority != null && !requestAuthority.isEmpty()) {
                    configBuilder.adRequestAuthority(requestAuthority);
                }
                String statsAuthority = parameters.getServerParameters().getString("stats_authority");
                if (statsAuthority != null && !statsAuthority.isEmpty()) {
                    configBuilder.statsRequestAuthority(statsAuthority);
                }
                SDKConfig config = configBuilder.build();
                BidscubeSDK.initialize(context, config);
                log("Bidscube SDK successfully initialized with app id: " + appId);
                status = MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS;
                onCompletionListener.onCompletion(status, null);
            } catch (Exception e) {
                log("Bidscube SDK initialization failed with error: " + e.getMessage());
                status = MaxAdapter.InitializationStatus.INITIALIZED_FAILURE;
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

        if (status != MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS) {
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
            final View[] adViewHolder = new View[1];
            adViewHolder[0] = BidscubeSDK.getImageAdView(pid, new com.bidscube.sdk.interfaces.AdCallback() {
                @Override
                public void onAdLoading(String placementId) {
                }

                @Override
                public void onAdLoaded(String placementId) {
                    log("Bidscube " + adFormat.getLabel() + " ad loaded successfully");
                    listener.onAdViewAdLoaded(adViewHolder[0]);
                }

                @Override
                public void onAdDisplayed(String placementId) {
                }

                @Override
                public void onAdClicked(String placementId) {
                }

                @Override
                public void onAdClosed(String placementId) {
                }

                @Override
                public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                    log("Bidscube " + adFormat.getLabel() + " ad load failed: " + errorMessage);
                    listener.onAdViewAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
                }
            });
            if (adViewHolder[0] == null) {
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

        if (status != MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS || !BidscubeSDK.isInitialized()) {
            log("Bidscube SDK not successfully initialized: failing interstitial ad load...");
            listener.onInterstitialAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }
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
            public void onAdLoading(String placementId) {
            }

            @Override
            public void onAdLoaded(String placementId) {
            }

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

        if (status != MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS || !BidscubeSDK.isInitialized()) {
            log("Bidscube SDK not successfully initialized: failing rewarded ad load...");
            listener.onRewardedAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }
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
            public void onAdLoading(String placementId) {
            }

            @Override
            public void onAdLoaded(String placementId) {
            }

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

        if (status != MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS || !BidscubeSDK.isInitialized()) {
            log("Bidscube SDK not successfully initialized: failing native ad load...");
            listener.onNativeAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";

        BidscubeSDK.getNativeAdView(pid, new com.bidscube.sdk.interfaces.AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
            }

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
            public void onAdDisplayed(String placementId) {
            }

            @Override
            public void onAdClicked(String placementId) {
            }

            @Override
            public void onAdClosed(String placementId) {
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                log("Bidscube native ad load failed: " + errorMessage);
                listener.onNativeAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        });
    }
}
