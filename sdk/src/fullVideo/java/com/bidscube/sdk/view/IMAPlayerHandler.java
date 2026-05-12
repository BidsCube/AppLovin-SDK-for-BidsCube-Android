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
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;

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
            adsManager.init();
            adsManager.addAdEventListener(adEvent -> {
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
                        SDKLogger.d(eventsTag, "Ad completed");
                        isVideoPlaying = false;
                        hideSkipButton();
                        if (completionListener != null) {
                            completionListener.onVideoCompleted();
                        }
                        break;

                    case SKIPPED:
                        SDKLogger.d(eventsTag, "Ad skipped");
                        isVideoPlaying = false;
                        hideSkipButton();
                        if (completionListener != null) {
                            completionListener.onVideoSkipped();
                        }
                        break;

                    default:
                        SDKLogger.d(eventsTag, "Other ad event: " + adEvent.getType());
                        break;
                }
            });

            adsManager.start();
        });

        adsLoader.requestAds(request);
    }

    @Override
    public void skipVideo() {
        if (adsManager != null && isVideoPlaying) {
            try {
                adsManager.skip();
                isVideoPlaying = false;
                hideSkipButton();
                if (completionListener != null) {
                    completionListener.onVideoSkipped();
                }
            } catch (Exception e) {
                SDKLogger.e("IMAPlayerHandler", "Error skipping video ad: " + e.getMessage());
                if (completionListener != null) {
                    completionListener.onVideoSkipped();
                }
            }
        } else if (completionListener != null) {
            completionListener.onVideoSkipped();
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
