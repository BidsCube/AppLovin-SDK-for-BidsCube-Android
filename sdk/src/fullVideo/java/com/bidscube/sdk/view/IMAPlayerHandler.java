package com.bidscube.sdk.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.bidscube.sdk.adapters.VideoAdPlayerAdapter;
import com.bidscube.sdk.utils.SDKLogger;
import com.bidscube.sdk.video.BidscubeVastVideoPlayer;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("ViewConstructor")
public class IMAPlayerHandler extends BidscubeVastVideoPlayer {

    private ImaSdkFactory sdkFactory;
    private AdsLoader adsLoader;
    private AdsManager adsManager;
    private VideoView videoView;
    private Button skipButton;
    private final String eventsTag = "IMAevent";
    private final String vastUrl;
    private final String redirectUrl;
    private BidscubeVastVideoPlayer.OnVideoCompletionListener completionListener;
    private boolean isVideoPlaying = false;
    private final AtomicBoolean linearCompletedSent = new AtomicBoolean(false);
    private final AtomicBoolean skippedSent = new AtomicBoolean(false);
    private final AtomicBoolean sessionCompletedSent = new AtomicBoolean(false);
    private final AtomicBoolean playbackFailedSent = new AtomicBoolean(false);
    private final AtomicBoolean terminalEventSent = new AtomicBoolean(false);

    public IMAPlayerHandler(String vastUrl, String redirectUrl, Context context) {
        super(context);
        this.vastUrl = vastUrl;
        this.redirectUrl = redirectUrl;
        init(redirectUrl, context);
    }

    public IMAPlayerHandler(String vastUrl, String redirectUrl, Context context, AttributeSet attrs) {
        super(context, attrs);
        this.vastUrl = vastUrl;
        this.redirectUrl = redirectUrl;
        init(redirectUrl, context);
    }

    @Override
    public boolean managesPostVideoExperience() {
        return true;
    }

    private void init(String clickRedirectUrl, Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        videoView = new VideoView(context);

        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        videoParams.gravity = Gravity.CENTER;
        videoView.setLayoutParams(videoParams);

        setBackgroundColor(android.graphics.Color.BLACK);
        addView(videoView);

        VideoAdPlayer videoAdPlayerAdapter = new VideoAdPlayerAdapter(videoView, audioManager);
        AdDisplayContainer adDisplayContainer = ImaSdkFactory.createAdDisplayContainer(this, videoAdPlayerAdapter);
        setupClickToOpenUrl(clickRedirectUrl);

        sdkFactory = ImaSdkFactory.getInstance();
        ImaSdkSettings imaSdkSettings = sdkFactory.createImaSdkSettings();
        adsLoader = sdkFactory.createAdsLoader(context, imaSdkSettings, adDisplayContainer);
    }

    private void setupClickToOpenUrl(String clickRedirectUrl) {
        try {
            setOnClickListener(v -> openUrlInBrowser(clickRedirectUrl));
            if (videoView != null) {
                videoView.setOnClickListener(v -> openUrlInBrowser(clickRedirectUrl));
            }
        } catch (Exception e) {
            SDKLogger.e("IMAPlayerHandler", "Error setting up click listener: " + e.getMessage());
        }
    }

    private void openUrlInBrowser(String url) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            android.content.Context context = getContext();
            if (context != null) {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            SDKLogger.e("IMAPlayerHandler", "Error opening URL in browser: " + e.getMessage());
        }
    }

    @Override
    public void playVast(String vastTag, boolean isUrl) {
        int len = vastTag == null ? 0 : vastTag.length();
        Log.i("BidscubeIntegration", "IMAPlayerHandler.playVast: isUrl=" + isUrl + " payloadChars=" + len);
        AdsRequest request = sdkFactory.createAdsRequest();

        if (isUrl) {
            request.setAdTagUrl(vastTag);
        } else {
            request.setAdsResponse(vastTag);
        }

        adsLoader.addAdsLoadedListener(adsManagerLoadedEvent -> {
            adsManager = adsManagerLoadedEvent.getAdsManager();
            adsManager.addAdEventListener(this::handleAdEvent);
            adsManager.addAdErrorListener(this::handleAdError);
            adsManager.init();
            adsManager.start();
        });

        adsLoader.addAdErrorListener(adErrorEvent -> {
            SDKLogger.e("IMAPlayerHandler", "AdsLoader error: " + describeAdError(adErrorEvent));
            firePlaybackFailed();
        });

        adsLoader.requestAds(request);
    }

    private void handleAdEvent(AdEvent adEvent) {
        SDKLogger.d("IMAPlayerHandler", "Ad event: " + adEvent.getType());

        switch (adEvent.getType()) {
            case LOADED:
                SDKLogger.d(eventsTag, "Ad loaded");
                isVideoPlaying = true;
                postDelayed(() -> {
                    if (skipButton != null) {
                        skipButton.setVisibility(View.VISIBLE);
                    }
                }, 2000);
                break;

            case STARTED:
                SDKLogger.d(eventsTag, "Ad started");
                isVideoPlaying = true;
                break;

            case COMPLETED:
                SDKLogger.d(eventsTag, "Ad completed (linear)");
                isVideoPlaying = false;
                hideSkipButton();
                fireLinearCompleted();
                break;

            case SKIPPED:
                SDKLogger.d(eventsTag, "Ad skipped");
                isVideoPlaying = false;
                hideSkipButton();
                fireSkipped();
                break;

            case ALL_ADS_COMPLETED:
                SDKLogger.d(eventsTag, "All ads completed (session)");
                isVideoPlaying = false;
                hideSkipButton();
                fireSessionCompleted();
                break;

            default:
                SDKLogger.d(eventsTag, "Other ad event: " + adEvent.getType());
                break;
        }
    }

    private void handleAdError(AdErrorEvent adErrorEvent) {
        SDKLogger.e("IMAPlayerHandler", "Ad error: " + describeAdError(adErrorEvent));
        isVideoPlaying = false;
        hideSkipButton();
        firePlaybackFailed();
    }

    private static String describeAdError(AdErrorEvent adErrorEvent) {
        if (adErrorEvent == null || adErrorEvent.getError() == null) {
            return "unknown";
        }
        return adErrorEvent.getError().getMessage();
    }

    private void fireLinearCompleted() {
        if (skippedSent.get() || playbackFailedSent.get()
                || !linearCompletedSent.compareAndSet(false, true)) {
            return;
        }
        terminalEventSent.set(true);
        if (completionListener != null) {
            completionListener.onVideoCompleted();
        }
    }

    private void fireSkipped() {
        if (linearCompletedSent.get() || playbackFailedSent.get()
                || !skippedSent.compareAndSet(false, true)) {
            return;
        }
        terminalEventSent.set(true);
        if (completionListener != null) {
            completionListener.onVideoSkipped();
        }
    }

    private void fireSessionCompleted() {
        if (playbackFailedSent.get() || !sessionCompletedSent.compareAndSet(false, true)) {
            return;
        }
        if (completionListener != null) {
            completionListener.onAdSessionCompleted();
        }
    }

    private void firePlaybackFailed() {
        if (terminalEventSent.get() || !playbackFailedSent.compareAndSet(false, true)) {
            return;
        }
        isVideoPlaying = false;
        if (completionListener != null) {
            completionListener.onVideoPlaybackFailed();
        }
    }

    @Override
    public void skipVideo() {
        if (adsManager != null && isVideoPlaying) {
            try {
                adsManager.skip();
            } catch (Exception e) {
                SDKLogger.e("IMAPlayerHandler", "Error skipping video ad: " + e.getMessage());
                fireSkipped();
            }
        } else if (!linearCompletedSent.get() && !skippedSent.get() && !playbackFailedSent.get()) {
            fireSkipped();
        }
    }

    private void hideSkipButton() {
        if (skipButton != null) {
            skipButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void setOnVideoCompletionListener(BidscubeVastVideoPlayer.OnVideoCompletionListener listener) {
        this.completionListener = listener;
    }

    @Override
    public void release() {
        if (adsManager != null) {
            adsManager.destroy();
            adsManager = null;
        }
        if (adsLoader != null) {
            adsLoader.release();
            adsLoader = null;
        }
        isVideoPlaying = false;
    }
}
