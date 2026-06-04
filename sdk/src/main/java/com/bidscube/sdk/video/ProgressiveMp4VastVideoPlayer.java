package com.bidscube.sdk.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.bidscube.sdk.network.TrackerPinger;
import com.bidscube.sdk.utils.SDKLogger;
import com.bidscube.sdk.utils.VastParser;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Plays progressive MP4 from inline VAST ({@code MediaFile} URL). Does not require Google IMA on the host app classpath.
 */
@SuppressLint("ViewConstructor")
public class ProgressiveMp4VastVideoPlayer extends BidscubeVastVideoPlayer {

    private static final String TAG = "ProgressiveMp4Vast";
    private static final long PROGRESS_POLL_MS = 250L;

    private final VideoView videoView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            pollProgress();
            handler.postDelayed(this, PROGRESS_POLL_MS);
        }
    };

    private final String initialVastMarkup;
    private final String initialClickThroughRedirectUrl;
    private OnVideoCompletionListener completionListener;
    private String vastMarkup;
    private String clickThroughRedirectUrl;
    private List<String> impressionUrls = Collections.emptyList();
    private List<String> startUrls = Collections.emptyList();
    private List<String> firstQuartileUrls = Collections.emptyList();
    private List<String> midpointUrls = Collections.emptyList();
    private List<String> thirdQuartileUrls = Collections.emptyList();
    private List<String> completeUrls = Collections.emptyList();
    private List<String> clickTrackingUrls = Collections.emptyList();
    private List<String> skipUrls = Collections.emptyList();
    private List<String> closeLinearUrls = Collections.emptyList();
    private List<String> errorUrls = Collections.emptyList();
    private int durationMs;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean skipped = new AtomicBoolean(false);
    private final AtomicBoolean firstQuartileTracked = new AtomicBoolean(false);
    private final AtomicBoolean midpointTracked = new AtomicBoolean(false);
    private final AtomicBoolean thirdQuartileTracked = new AtomicBoolean(false);

    public ProgressiveMp4VastVideoPlayer(Context context, String vastMarkup, String clickThroughRedirectUrl) {
        super(context);
        this.initialVastMarkup = vastMarkup;
        this.initialClickThroughRedirectUrl = clickThroughRedirectUrl;
        videoView = createConfiguredVideoView(context);
        addView(videoView);
    }

    public ProgressiveMp4VastVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialVastMarkup = null;
        this.initialClickThroughRedirectUrl = null;
        videoView = createConfiguredVideoView(context);
        addView(videoView);
    }

    private VideoView createConfiguredVideoView(Context context) {
        VideoView view = new VideoView(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        view.setLayoutParams(params);
        view.setOnClickListener(v -> handleClicked());
        return view;
    }

    @Override
    public void playVast(String vastTag, boolean isUrl) {
        vastMarkup = !TextUtils.isEmpty(vastTag) ? vastTag : initialVastMarkup;
        clickThroughRedirectUrl = !TextUtils.isEmpty(initialClickThroughRedirectUrl)
                ? initialClickThroughRedirectUrl
                : VastParser.getClickThroughUrl(vastMarkup);

        if (TextUtils.isEmpty(vastMarkup)) {
            handleError("Missing VAST markup");
            return;
        }
        if (isUrl) {
            handleError("Remote VAST URLs are not supported by ProgressiveMp4VastVideoPlayer");
            return;
        }

        String mediaFileUrl = VastParser.getMediaFileUrl(vastMarkup);
        if (TextUtils.isEmpty(mediaFileUrl)) {
            handleError("VAST does not contain a MediaFile URL");
            return;
        }

        impressionUrls = VastParser.getImpressionUrls(vastMarkup);
        startUrls = VastParser.getTrackingUrls(vastMarkup, "start");
        firstQuartileUrls = VastParser.getTrackingUrls(vastMarkup, "firstQuartile");
        midpointUrls = VastParser.getTrackingUrls(vastMarkup, "midpoint");
        thirdQuartileUrls = VastParser.getTrackingUrls(vastMarkup, "thirdQuartile");
        completeUrls = VastParser.getTrackingUrls(vastMarkup, "complete");
        clickTrackingUrls = VastParser.getClickTrackingUrls(vastMarkup);
        skipUrls = VastParser.getTrackingUrls(vastMarkup, "skip");
        closeLinearUrls = VastParser.getTrackingUrls(vastMarkup, "closeLinear");
        errorUrls = VastParser.getErrorUrls(vastMarkup);

        resetPlaybackState();
        stopProgressPolling();

        videoView.setOnPreparedListener(this::onPrepared);
        videoView.setOnCompletionListener(mediaPlayer -> onCompleted());
        videoView.setOnErrorListener((mediaPlayer, what, extra) -> {
            handleError("MediaPlayer error " + what + " / " + extra);
            return true;
        });
        videoView.setVideoURI(Uri.parse(mediaFileUrl));
        videoView.start();
    }

    @Override
    public void setOnVideoCompletionListener(OnVideoCompletionListener listener) {
        completionListener = listener;
    }

    @Override
    public void release() {
        stopProgressPolling();
        try {
            videoView.stopPlayback();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void skipVideo() {
        try {
            videoView.pause();
        } catch (Throwable ignored) {
        }
        markSkipped();
    }

    private void onPrepared(MediaPlayer mediaPlayer) {
        durationMs = mediaPlayer != null ? mediaPlayer.getDuration() : 0;
        if (started.compareAndSet(false, true)) {
            TrackerPinger.pingUrls("vast.impression", impressionUrls);
            TrackerPinger.pingUrls("vast.start", startUrls);
        }
        startProgressPolling();
    }

    private void onCompleted() {
        stopProgressPolling();
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        TrackerPinger.pingUrls("vast.complete", completeUrls);
        if (completionListener != null) {
            completionListener.onVideoCompleted();
        }
    }

    private void handleClicked() {
        TrackerPinger.pingUrls("vast.clickTracking", clickTrackingUrls);
        if (TextUtils.isEmpty(clickThroughRedirectUrl)) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughRedirectUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        } catch (Exception e) {
            SDKLogger.w(TAG, "Failed to open click-through URL: " + e.getMessage());
        }
    }

    private void handleError(String message) {
        SDKLogger.e(TAG, "Progressive MP4 video error: " + message);
        TrackerPinger.pingUrls("vast.error", errorUrls);
        markSkipped();
    }

    private void markSkipped() {
        stopProgressPolling();
        if (completed.get() || !skipped.compareAndSet(false, true)) {
            return;
        }
        TrackerPinger.pingUrls("vast.skip", skipUrls);
        TrackerPinger.pingUrls("vast.closeLinear", closeLinearUrls);
        if (completionListener != null) {
            completionListener.onVideoSkipped();
        }
    }

    private void startProgressPolling() {
        handler.removeCallbacks(progressRunnable);
        handler.postDelayed(progressRunnable, PROGRESS_POLL_MS);
    }

    private void stopProgressPolling() {
        handler.removeCallbacks(progressRunnable);
    }

    private void pollProgress() {
        if (!started.get() || durationMs <= 0 || completed.get() || skipped.get()) {
            return;
        }
        int currentMs;
        try {
            currentMs = videoView.getCurrentPosition();
        } catch (Throwable ignored) {
            return;
        }

        double progress = (double) currentMs / (double) durationMs;
        if (progress >= 0.25d && firstQuartileTracked.compareAndSet(false, true)) {
            TrackerPinger.pingUrls("vast.firstQuartile", firstQuartileUrls);
        }
        if (progress >= 0.50d && midpointTracked.compareAndSet(false, true)) {
            TrackerPinger.pingUrls("vast.midpoint", midpointUrls);
        }
        if (progress >= 0.75d && thirdQuartileTracked.compareAndSet(false, true)) {
            TrackerPinger.pingUrls("vast.thirdQuartile", thirdQuartileUrls);
        }
    }

    private void resetPlaybackState() {
        durationMs = 0;
        started.set(false);
        completed.set(false);
        skipped.set(false);
        firstQuartileTracked.set(false);
        midpointTracked.set(false);
        thirdQuartileTracked.set(false);
    }
}
