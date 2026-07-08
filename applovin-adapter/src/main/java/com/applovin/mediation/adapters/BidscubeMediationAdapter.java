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
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.sdk.AppLovinSdk;
import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.interfaces.InitializationCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bidscube MAX Mediation Adapter.
 * Integrates Bidscube SDK with AppLovin MAX for Banner, Interstitial, and Rewarded ads.
 * <p>
 * Native MAX is not supported in this release. Interstitial and rewarded creatives are
 * preloaded during MAX load and shown from cache on show.
 * <p>
 * Built against AppLovin SDK 13.0.x ({@code com.applovin.mediation.adapter} package layout).
 */
public class BidscubeMediationAdapter
        extends MediationAdapterBase
        implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter, MaxSignalProvider {

    /** Always-on logcat tag for MAX ↔ Bidscube wiring (use: {@code adb logcat -s BidscubeMaxAdapter}). */
    private static final String DIAG = "BidscubeMaxAdapter";

    private static final Object INIT_LOCK = new Object();
    private static final List<MaxAdapter.OnCompletionListener> pendingInitListeners = new ArrayList<>();
    private static final AtomicBoolean sdkInitComplete = new AtomicBoolean(false);
    private static volatile MaxAdapter.InitializationStatus status = MaxAdapter.InitializationStatus.INITIALIZING;
    private static volatile String lastInitError;

    private volatile View cachedAdView;
    private volatile String cachedInterstitialPlacementId;
    private volatile boolean cachedInterstitialIsImage;
    private volatile String cachedRewardedPlacementId;

    private static void diag(String msg) {
        Log.i(DIAG, msg);
    }

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
        return getVersionString(BidscubeMediationAdapter.class, "1.2.10");
    }

    @Override
    public void onDestroy() {
        diag("onDestroy: clearing adapter caches and references");
        cachedAdView = null;
        cachedInterstitialPlacementId = null;
        cachedInterstitialIsImage = false;
        cachedRewardedPlacementId = null;
        BidscubeSDK.clearPreloadCache();
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity,
            final MaxSignalCollectionListener callback) {
        try {
            final String signal = BidscubeSDK.collectSignal();
            diag("collectSignal: returning structured signal");
            callback.onSignalCollected(signal);
        } catch (Exception e) {
            log("Bidscube signal collection failed: " + e.getMessage());
            diag("collectSignal: FAILURE " + e.getMessage());
            callback.onSignalCollectionFailed(e.getMessage() != null ? e.getMessage() : "Signal collection failed");
        }
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity,
            final MaxAdapter.OnCompletionListener onCompletionListener) {
        diag("initialize: MAX called adapter init; sdkInitComplete=" + sdkInitComplete.get()
                + " lastStatus=" + status
                + " bidscubeSdkInitializedNow=" + BidscubeSDK.isInitialized());

        if (sdkInitComplete.get()) {
            onCompletionListener.onCompletion(status, status == MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS
                    ? null
                    : lastInitError);
            return;
        }

        synchronized (INIT_LOCK) {
            pendingInitListeners.add(onCompletionListener);

            if (sdkInitComplete.get()) {
                completePendingInitializations();
                return;
            }

            if (pendingInitListeners.size() > 1) {
                diag("initialize: init already in progress; queued listener");
                return;
            }

            final String appId = parameters.getServerParameters().getString("app_id");
            log("Initializing Bidscube SDK with app id: " + appId + "...");
            diag("initialize: server app_id=" + appId);

            if (appId == null || appId.isEmpty()) {
                log("Bidscube SDK initialization failed: app_id is null or empty");
                diag("initialize: FAILURE app_id missing");
                status = MaxAdapter.InitializationStatus.INITIALIZED_FAILURE;
                lastInitError = "App id is null or empty";
                completePendingInitializations();
                return;
            }

            Context context = activity != null ? activity.getApplicationContext() : getApplicationContext();
            if (context == null) {
                log("Bidscube SDK initialization failed: no context available");
                diag("initialize: FAILURE no Context");
                status = MaxAdapter.InitializationStatus.INITIALIZED_FAILURE;
                lastInitError = "No context available";
                completePendingInitializations();
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
                configBuilder.appId(appId);
                if (requestAuthority != null && !requestAuthority.isEmpty()) {
                    configBuilder.adRequestAuthority(requestAuthority);
                }
                String statsAuthority = parameters.getServerParameters().getString("stats_authority");
                if (statsAuthority != null && !statsAuthority.isEmpty()) {
                    configBuilder.statsRequestAuthority(statsAuthority);
                }
                SDKConfig config = configBuilder.build();
                BidscubeSDK.setMediationAdapterVersion(getAdapterVersion());
                diag("initialize: calling BidscubeSDK.initialize; adAuthority="
                        + config.getAdRequestAuthority()
                        + " statsAuthority=" + config.getStatsRequestAuthority());

                BidscubeSDK.initialize(context, config, new InitializationCallback() {
                    @Override
                    public void onInitialized() {
                        log("Bidscube SDK successfully initialized with app id: " + appId);
                        diag("initialize: BidscubeSDK ready; isInitialized=" + BidscubeSDK.isInitialized());
                        sdkInitComplete.set(true);
                        status = MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS;
                        lastInitError = null;
                        completePendingInitializations();
                    }

                    @Override
                    public void onInitializationFailed(String message) {
                        log("Bidscube SDK initialization failed with error: " + message);
                        diag("initialize: FAILURE " + message);
                        status = MaxAdapter.InitializationStatus.INITIALIZED_FAILURE;
                        lastInitError = message;
                        completePendingInitializations();
                    }
                });
            } catch (Exception e) {
                log("Bidscube SDK initialization failed with error: " + e.getMessage());
                diag("initialize: EXCEPTION " + e.getMessage());
                status = MaxAdapter.InitializationStatus.INITIALIZED_FAILURE;
                lastInitError = e.getMessage();
                completePendingInitializations();
            }
        }
    }

    private static void completePendingInitializations() {
        final List<MaxAdapter.OnCompletionListener> listeners;
        synchronized (INIT_LOCK) {
            listeners = new ArrayList<>(pendingInitListeners);
            pendingInitListeners.clear();
        }
        final String error = status == MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS ? null : lastInitError;
        for (MaxAdapter.OnCompletionListener listener : listeners) {
            listener.onCompletion(status, error);
        }
    }

    private boolean isSdkReady() {
        return status == MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS && BidscubeSDK.isInitialized();
    }

    /**
     * Returns true when MAX/server params explicitly request a static/image interstitial.
     */
    private static boolean isStaticImageInterstitial(final MaxAdapterResponseParameters parameters) {
        final String[] keys = {"creative_type", "ad_format", "format", "bidscube_creative_type"};
        for (final String key : keys) {
            final String value = parameters.getServerParameters().getString(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            final String normalized = value.trim().toLowerCase();
            if ("image".equals(normalized) || "static".equals(normalized) || "banner".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat,
            @Nullable final Activity activity, final MaxAdViewAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log("Loading " + adFormat.getLabel() + " ad for placement: " + placementId + "...");
        diag("loadAdViewAd: format=" + adFormat.getLabel() + " placement=" + placementId
                + " maxInitStatus=" + status + " bidscubeReady=" + BidscubeSDK.isInitialized());

        if (!isSdkReady()) {
            log("Bidscube SDK not successfully initialized: failing " + adFormat.getLabel() + " ad load...");
            diag("loadAdViewAd: FAIL NOT_INITIALIZED");
            listener.onAdViewAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        final String pid = placementId != null ? placementId : "";
        bindDisplayActivity(activity);

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
                    cachedAdView = adViewHolder[0];
                    listener.onAdViewAdLoaded(adViewHolder[0]);
                }

                @Override
                public void onAdDisplayed(String placementId) {
                    diag("loadAdViewAd callback: onAdDisplayed placement=" + placementId);
                }

                @Override
                public void onAdClicked(String placementId) {
                    listener.onAdViewAdClicked();
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
        final String pid = placementId != null ? placementId : "";
        final boolean staticImage = isStaticImageInterstitial(parameters);
        log("Loading interstitial ad for placement: " + pid + "...");
        diag("loadInterstitialAd: placement=" + pid + " staticImage=" + staticImage
                + " bidscubeReady=" + BidscubeSDK.isInitialized());

        if (!isSdkReady()) {
            log("Bidscube SDK not successfully initialized: failing interstitial ad load...");
            diag("loadInterstitialAd: FAIL NOT_INITIALIZED");
            listener.onInterstitialAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        bindDisplayActivity(activity);

        com.bidscube.sdk.interfaces.AdCallback preloadCallback = new com.bidscube.sdk.interfaces.AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
                diag("loadInterstitialAd: onAdLoading " + placementId);
            }

            @Override
            public void onAdLoaded(String placementId) {
                cachedInterstitialPlacementId = pid;
                cachedInterstitialIsImage = staticImage;
                log("Bidscube interstitial ad loaded successfully");
                diag("loadInterstitialAd: cached creative for placement=" + pid);
                listener.onInterstitialAdLoaded();
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
                log("Bidscube interstitial ad load failed: " + errorMessage);
                diag("loadInterstitialAd: onAdFailed " + placementId + " code=" + errorCode + " " + errorMessage);
                listener.onInterstitialAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        };

        if (staticImage) {
            BidscubeSDK.preloadImageAd(pid, preloadCallback);
        } else {
            BidscubeSDK.preloadInterstitialVideoAd(pid, preloadCallback);
        }
    }

    private static void bindDisplayActivity(@Nullable final Activity activity) {
        if (activity != null) {
            BidscubeSDK.setDisplayActivity(activity);
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxInterstitialAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";
        final boolean staticImage = isStaticImageInterstitial(parameters);
        log("Showing Bidscube interstitial ad for placement: " + pid + "...");
        diag("showInterstitialAd: placement=" + pid + " staticImage=" + staticImage
                + " cachedPlacement=" + cachedInterstitialPlacementId);

        if (!isSdkReady()) {
            listener.onInterstitialAdDisplayFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        if (cachedInterstitialPlacementId == null || !cachedInterstitialPlacementId.equals(pid)) {
            diag("showInterstitialAd: no cached creative for placement");
            listener.onInterstitialAdDisplayFailed(MaxAdapterError.INVALID_LOAD_STATE);
            return;
        }

        final com.bidscube.sdk.interfaces.AdCallback callback = new com.bidscube.sdk.interfaces.AdCallback() {
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
                cachedInterstitialPlacementId = null;
                listener.onInterstitialAdHidden();
            }

            @Override
            public void onUserRewarded(String placementId) {
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                diag("showInterstitialAd: onAdFailed " + placementId + " code=" + errorCode + " " + errorMessage);
                cachedInterstitialPlacementId = null;
                listener.onInterstitialAdDisplayFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        };

        bindDisplayActivity(activity);

        if (cachedInterstitialIsImage || staticImage) {
            BidscubeSDK.showImageAd(pid, callback);
        } else {
            BidscubeSDK.showInterstitialVideoAd(pid, callback);
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxRewardedAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";
        log("Loading rewarded ad for placement: " + pid + "...");
        diag("loadRewardedAd: placement=" + pid + " bidscubeReady=" + BidscubeSDK.isInitialized());

        if (!isSdkReady()) {
            log("Bidscube SDK not successfully initialized: failing rewarded ad load...");
            diag("loadRewardedAd: FAIL NOT_INITIALIZED");
            listener.onRewardedAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        bindDisplayActivity(activity);

        BidscubeSDK.preloadRewardedVideoAd(pid, new com.bidscube.sdk.interfaces.AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
                diag("loadRewardedAd: onAdLoading " + placementId);
            }

            @Override
            public void onAdLoaded(String placementId) {
                cachedRewardedPlacementId = pid;
                log("Bidscube rewarded ad loaded successfully");
                diag("loadRewardedAd: cached creative for placement=" + pid);
                listener.onRewardedAdLoaded();
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
                log("Bidscube rewarded ad load failed: " + errorMessage);
                diag("loadRewardedAd: onAdFailed " + placementId + " code=" + errorCode + " " + errorMessage);
                listener.onRewardedAdLoadFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        });
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxRewardedAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String pid = placementId != null ? placementId : "";
        final AtomicBoolean rewarded = new AtomicBoolean(false);
        log("Showing Bidscube rewarded ad for placement: " + pid + "...");
        diag("showRewardedAd: placement=" + pid + " cachedPlacement=" + cachedRewardedPlacementId);

        if (!isSdkReady()) {
            listener.onRewardedAdDisplayFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        if (cachedRewardedPlacementId == null || !cachedRewardedPlacementId.equals(pid)) {
            diag("showRewardedAd: no cached creative for placement");
            listener.onRewardedAdDisplayFailed(MaxAdapterError.INVALID_LOAD_STATE);
            return;
        }

        configureReward(parameters);
        bindDisplayActivity(activity);

        BidscubeSDK.showRewardedVideoAd(pid, new com.bidscube.sdk.interfaces.AdCallback() {
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
            public void onUserRewarded(String placementId) {
                diag("showRewardedAd: onUserRewarded " + placementId);
                if (rewarded.compareAndSet(false, true)) {
                    listener.onUserRewarded(getReward());
                }
            }

            @Override
            public void onVideoAdCompleted(String placementId) {
                diag("showRewardedAd: onVideoAdCompleted " + placementId);
            }

            @Override
            public void onVideoAdSkipped(String placementId) {
                diag("showRewardedAd: onVideoAdSkipped " + placementId);
            }

            @Override
            public void onAdClosed(String placementId) {
                diag("showRewardedAd: onAdClosed " + placementId);
                cachedRewardedPlacementId = null;
                listener.onRewardedAdHidden();
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                diag("showRewardedAd: onAdFailed " + placementId + " code=" + errorCode + " " + errorMessage);
                cachedRewardedPlacementId = null;
                listener.onRewardedAdDisplayFailed(new MaxAdapterError(errorCode, errorMessage));
            }
        });
    }
}
