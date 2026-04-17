package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
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

    /** Always-on logcat tag for MAX ↔ Bidscube wiring (use: {@code adb logcat -s BidscubeMaxAdapter}). */
    private static final String DIAG = "BidscubeMaxAdapter";

    private static void diag(String msg) {
        Log.i(DIAG, msg);
    }

    private static final AtomicBoolean initialized = new AtomicBoolean();
    private static MaxAdapter.InitializationStatus status;

    public BidscubeMediationAdapter(final AppLovinSdk sdk) {
        super(sdk);
        diag("constructed: AppLovinSdk attached=" + (sdk != null));
    }

    @Override
    public String getSdkVersion() {
        return BidscubeSDK.getSdkVersion();
    }

    @Override
    public String getAdapterVersion() {
        return getVersionString(BidscubeMediationAdapter.class, "1.0.3.1");
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity,
            final MaxSignalCollectionListener callback) {
        diag("collectSignal: returning test signal");
        callback.onSignalCollected("bidscube_test_signal");
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity,
            final MaxAdapter.OnCompletionListener onCompletionListener) {
        diag("initialize: MAX called adapter init; alreadyRan=" + initialized.get()
                + " lastStatus=" + status
                + " bidscubeSdkInitializedNow=" + BidscubeSDK.isInitialized());
        if (initialized.compareAndSet(false, true)) {
            final String appId = parameters.getServerParameters().getString("app_id");
            log("Initializing Bidscube SDK with app id: " + appId + "...");
            diag("initialize: server app_id=" + appId);

            if (appId == null || appId.isEmpty()) {
                log("Bidscube SDK initialization failed: app_id is null or empty");
                diag("initialize: FAILURE app_id missing");
                status = MaxAdapter.InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion(status, "App id is null or empty");
                return;
            }

            Context context = activity != null ? activity.getApplicationContext() : getApplicationContext();
            if (context == null) {
                log("Bidscube SDK initialization failed: no context available");
                diag("initialize: FAILURE no Context");
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
                        .enableLogging(true)
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
                diag("initialize: calling BidscubeSDK.initialize; adAuthority="
                        + config.getAdRequestAuthority()
                        + " statsAuthority=" + config.getStatsRequestAuthority()
                        + " customVastFactory=" + (config.getVastVideoPlayerFactory() != null));
                BidscubeSDK.initialize(context, config);
                log("Bidscube SDK successfully initialized with app id: " + appId);
                diag("initialize: BidscubeSDK.initialize returned (async core init may still be finishing); "
                        + "BidscubeSDK.isInitialized()=" + BidscubeSDK.isInitialized()
                        + " — watch tag BidscubeIntegration for \"ready for ads\"");
                status = MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS;
                onCompletionListener.onCompletion(status, null);
            } catch (Exception e) {
                log("Bidscube SDK initialization failed with error: " + e.getMessage());
                diag("initialize: EXCEPTION " + e.getMessage());
                status = MaxAdapter.InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion(status, e.getMessage());
            }
        } else {
            diag("initialize: idempotent completion status=" + status
                    + " BidscubeSDK.isInitialized()=" + BidscubeSDK.isInitialized());
            onCompletionListener.onCompletion(status, null);
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat,
            @Nullable final Activity activity, final MaxAdViewAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log("Loading " + adFormat.getLabel() + " ad for placement: " + placementId + "...");
        diag("loadAdViewAd: format=" + adFormat.getLabel() + " placement=" + placementId
                + " maxInitStatus=" + status + " bidscubeReady=" + BidscubeSDK.isInitialized());

        if (status != MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS) {
            log("Bidscube SDK not successfully initialized: failing " + adFormat.getLabel() + " ad load...");
            diag("loadAdViewAd: FAIL NOT_INITIALIZED (adapter status)");
            listener.onAdViewAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        if (!BidscubeSDK.isInitialized()) {
            diag("loadAdViewAd: FAIL NOT_INITIALIZED (Bidscube async init not finished yet)");
            listener.onAdViewAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        final String pid = placementId != null ? placementId : "";

        try {
            final View[] adViewHolder = new View[1];
            adViewHolder[0] = BidscubeSDK.getImageAdView(pid, new com.bidscube.sdk.interfaces.AdCallback() {
                @Override
                public void onAdLoading(String placementId) {
                    diag("loadAdViewAd callback: onAdLoading placement=" + placementId);
                }

                @Override
                public void onAdLoaded(String placementId) {
                    log("Bidscube " + adFormat.getLabel() + " ad loaded successfully");
                    diag("loadAdViewAd callback: onAdLoaded placement=" + placementId);
                    listener.onAdViewAdLoaded(adViewHolder[0]);
                }

                @Override
                public void onAdDisplayed(String placementId) {
                    diag("loadAdViewAd callback: onAdDisplayed placement=" + placementId);
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
                    diag("loadAdViewAd callback: onAdFailed placement=" + placementId
                            + " code=" + errorCode + " msg=" + errorMessage);
                    listener.onAdViewAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
                }
            });
            if (adViewHolder[0] == null) {
                diag("loadAdViewAd: getImageAdView returned null");
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
        diag("loadInterstitialAd: placement=" + placementId + " maxInitStatus=" + status
                + " bidscubeReady=" + BidscubeSDK.isInitialized());

        if (status != MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS || !BidscubeSDK.isInitialized()) {
            log("Bidscube SDK not successfully initialized: failing interstitial ad load...");
            diag("loadInterstitialAd: FAIL NOT_INITIALIZED");
            listener.onInterstitialAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }
        log("Bidscube interstitial ad loaded successfully");
        diag("loadInterstitialAd: reported loaded (creative fetched on show)");
        listener.onInterstitialAdLoaded();
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxInterstitialAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";
        log("Showing Bidscube interstitial ad for placement: " + pid + "...");
        diag("showInterstitialAd: placement=" + pid);

        BidscubeSDK.showImageAd(pid, new com.bidscube.sdk.interfaces.AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
                diag("showInterstitialAd: onAdLoading " + placementId);
            }

            @Override
            public void onAdLoaded(String placementId) {
                diag("showInterstitialAd: onAdLoaded " + placementId);
            }

            @Override
            public void onAdDisplayed(String placementId) {
                diag("showInterstitialAd: onAdDisplayed " + placementId);
                listener.onInterstitialAdDisplayed();
            }

            @Override
            public void onAdClicked(String placementId) {
                listener.onInterstitialAdClicked();
            }

            @Override
            public void onAdClosed(String placementId) {
                diag("showInterstitialAd: onAdClosed " + placementId);
                listener.onInterstitialAdHidden();
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                diag("showInterstitialAd: onAdFailed " + placementId + " code=" + errorCode + " " + errorMessage);
                listener.onInterstitialAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        });
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxRewardedAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log("Loading rewarded ad for placement: " + placementId + "...");
        diag("loadRewardedAd: placement=" + placementId + " maxInitStatus=" + status
                + " bidscubeReady=" + BidscubeSDK.isInitialized());

        if (status != MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS || !BidscubeSDK.isInitialized()) {
            log("Bidscube SDK not successfully initialized: failing rewarded ad load...");
            diag("loadRewardedAd: FAIL NOT_INITIALIZED");
            listener.onRewardedAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }
        log("Bidscube rewarded ad loaded successfully");
        diag("loadRewardedAd: reported loaded (video loads on show)");
        listener.onRewardedAdLoaded();
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxRewardedAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";
        log("Showing Bidscube rewarded ad for placement: " + pid + "...");
        diag("showRewardedAd: placement=" + pid + " (video / VAST pipeline)");
        configureReward(parameters);

        BidscubeSDK.showVideoAd(pid, new com.bidscube.sdk.interfaces.AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
                diag("showRewardedAd: onAdLoading " + placementId);
            }

            @Override
            public void onAdLoaded(String placementId) {
                diag("showRewardedAd: onAdLoaded " + placementId);
            }

            @Override
            public void onAdDisplayed(String placementId) {
                diag("showRewardedAd: onAdDisplayed " + placementId);
                listener.onRewardedAdDisplayed();
            }

            @Override
            public void onAdClicked(String placementId) {
                listener.onRewardedAdClicked();
            }

            @Override
            public void onAdClosed(String placementId) {
                diag("showRewardedAd: onAdClosed " + placementId);
                listener.onRewardedAdHidden();
            }

            @Override
            public void onVideoAdStarted(String placementId) {
                diag("showRewardedAd: onVideoAdStarted " + placementId);
            }

            @Override
            public void onVideoAdCompleted(String placementId) {
                diag("showRewardedAd: onVideoAdCompleted " + placementId);
                listener.onUserRewarded(getReward());
            }

            @Override
            public void onVideoAdSkipped(String placementId) {
                diag("showRewardedAd: onVideoAdSkipped " + placementId);
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                diag("showRewardedAd: onAdFailed " + placementId + " code=" + errorCode + " " + errorMessage);
                listener.onRewardedAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        });
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxNativeAdAdapterListener listener) {
        log("Loading Bidscube native ad...");
        diag("loadNativeAd: bidscubeReady=" + BidscubeSDK.isInitialized() + " maxInitStatus=" + status);

        if (status != MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS || !BidscubeSDK.isInitialized()) {
            log("Bidscube SDK not successfully initialized: failing native ad load...");
            diag("loadNativeAd: FAIL NOT_INITIALIZED");
            listener.onNativeAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";

        BidscubeSDK.getNativeAdView(pid, new com.bidscube.sdk.interfaces.AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
                diag("loadNativeAd: onAdLoading " + placementId);
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
                diag("loadNativeAd: onAdLoaded " + placementId);
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
                diag("loadNativeAd: onAdFailed " + placementId + " code=" + errorCode + " " + errorMessage);
                listener.onNativeAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        });
    }
}
