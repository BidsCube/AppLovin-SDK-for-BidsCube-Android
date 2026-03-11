package com.bidscube.sdk.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import com.bidscube.sdk.utils.SDKLogger;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.VideoView;

import androidx.media3.common.util.UnstableApi;

import com.bidscube.sdk.adapters.VideoAdPlayerAdapter;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;

@SuppressLint("ViewConstructor")
@UnstableApi
public class IMAPlayerHandler extends FrameLayout {

    /**
     * Interface for video completion callbacks
     */
    public interface OnVideoCompletionListener {
        void onVideoCompleted();

        void onVideoSkipped();
    }

    private ImaSdkFactory sdkFactory;
    private AdsLoader adsLoader;
    private AdsManager adsManager;
    private VideoView videoView;
    private Button skipButton;
    private final String eventsTag = "IMAevent";
    private final String vastUrl;
    private final String redirectUrl;
    private OnVideoCompletionListener completionListener;
    private boolean isVideoPlaying = false;

    public IMAPlayerHandler(String vastUrl, String redirectUrl, Context context) {
        super(context);
        this.vastUrl = vastUrl;
        this.redirectUrl = redirectUrl;
        init(vastUrl, redirectUrl, context);
    }

    public IMAPlayerHandler(String vastUrl, String redirectUrl, Context context, AttributeSet attrs) {
        super(context, attrs);
        this.vastUrl = vastUrl;
        this.redirectUrl = redirectUrl;
        init(vastUrl, redirectUrl, context);
    }

    private void init(String vastUrl, String redirectUrl, Context context) {

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        videoView = new VideoView(context);

        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        videoParams.gravity = Gravity.CENTER;
        videoView.setLayoutParams(videoParams);

        videoView.setScaleX(1.0f);
        videoView.setScaleY(1.0f);

        setBackgroundColor(android.graphics.Color.BLACK);

        addView(videoView);

        VideoAdPlayer videoAdPlayerAdapter = new VideoAdPlayerAdapter(videoView, audioManager);
        AdDisplayContainer adDisplayContainer = ImaSdkFactory.createAdDisplayContainer(this, videoAdPlayerAdapter);

        setupClickToOpenUrl(redirectUrl);

        sdkFactory = ImaSdkFactory.getInstance();

        ImaSdkSettings imaSdkSettings = sdkFactory.createImaSdkSettings();
        adsLoader = sdkFactory.createAdsLoader(context, imaSdkSettings, adDisplayContainer);
    }

    /**
     * Sets up click listener to open the redirect URL when video player is clicked
     */
    private void setupClickToOpenUrl(String redirectUrl) {
        try {

            setOnClickListener(v -> {
                SDKLogger.d("IMAPlayerHandler", "Video player clicked - opening: " + redirectUrl);
                openUrlInBrowser(redirectUrl);
            });

            if (videoView != null) {
                videoView.setOnClickListener(v -> {
                    SDKLogger.d("IMAPlayerHandler", "VideoView clicked - opening: " + redirectUrl);
                    openUrlInBrowser(redirectUrl);
                });
            }

            SDKLogger.d("IMAPlayerHandler", "Click listener set up to open: " + redirectUrl);
        } catch (Exception e) {
            SDKLogger.e("IMAPlayerHandler", "Error setting up click listener: " + e.getMessage());
        }
    }

    /**
     * Opens URL in external browser
     */
    private void openUrlInBrowser(String url) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            android.content.Context context = getContext();
            if (context != null) {
                context.startActivity(intent);
                SDKLogger.d("IMAPlayerHandler", "Opened URL in browser: " + url);
            } else {
                SDKLogger.e("IMAPlayerHandler", "Context is null, cannot open URL");
            }
        } catch (Exception e) {
            SDKLogger.e("IMAPlayerHandler", "Error opening URL in browser: " + e.getMessage());
        }
    }

    public void playVast(String vastTag, boolean isUrl) {
        AdsRequest request = sdkFactory.createAdsRequest();

        if (isUrl) {
            request.setAdTagUrl(vastTag);
            Log.i("VASTTag", "Playing VAST from URL: " + vastTag);
        } else {
            request.setAdsResponse(vastTag);
            Log.i("VASTTag", "Playing VAST from String response " + vastTag);
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

                    case CLICKED:
                        SDKLogger.d(eventsTag, "Ad click-through URL clicked");

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

    /**
     * Skips the current video ad
     */
    public void skipVideo() {
        if (adsManager != null && isVideoPlaying) {
            try {
                SDKLogger.d("IMAPlayerHandler", "Attempting to skip video ad");
                adsManager.skip();
                isVideoPlaying = false;
                hideSkipButton();

                if (completionListener != null) {
                    completionListener.onVideoSkipped();
                }

                SDKLogger.d("IMAPlayerHandler", "Video ad skipped successfully");
            } catch (Exception e) {
                SDKLogger.e("IMAPlayerHandler", "Error skipping video ad: " + e.getMessage());

                if (completionListener != null) {
                    completionListener.onVideoSkipped();
                }
            }
        } else {
            Log.w("IMAPlayerHandler", "Cannot skip video - adsManager is null or video not playing");

            if (completionListener != null) {
                completionListener.onVideoSkipped();
            }
        }
    }

    /**
     * Hides the skip button
     */
    private void hideSkipButton() {
        if (skipButton != null) {
            skipButton.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the video completion listener
     *
     * @param listener The listener to be called when video completes
     */
    public void setOnVideoCompletionListener(OnVideoCompletionListener listener) {
        this.completionListener = listener;
    }

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
