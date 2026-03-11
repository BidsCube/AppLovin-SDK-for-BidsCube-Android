package com.bidscube.sdk.adapters;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.VideoView;
import com.bidscube.sdk.utils.SDKLogger;

import androidx.annotation.NonNull;

import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class VideoAdPlayerAdapter implements VideoAdPlayer {

    private static final String LOGTAG = "VideoAdPlayerAdapter";
    private static final long POLLING_TIME_MS = 250;
    private static final long INITIAL_DELAY_MS = 250;

    private final VideoView videoPlayer;
    private final AudioManager audioManager;
    private final List<VideoAdPlayerCallback> videoAdPlayerCallbacks = new ArrayList<>();

    private Timer timer;
    private int adDuration;
    private int savedAdPosition;
    private AdMediaInfo loadedAdMediaInfo;

    private boolean isAdPlaying = false;
    private boolean isAdPaused = false;
    private boolean isAdLoaded = false;
    private boolean isReleased = false;

    public VideoAdPlayerAdapter(VideoView videoPlayer, AudioManager audioManager) {
        this.videoPlayer = videoPlayer;
        this.audioManager = audioManager;

        setupVideoPlayerListeners();
    }

    private void setupVideoPlayerListeners() {
        if (videoPlayer == null) {
            SDKLogger.e(LOGTAG, "VideoPlayer is null, cannot set up listeners");
            return;
        }

        videoPlayer.setOnCompletionListener(mediaPlayer -> {
            SDKLogger.d(LOGTAG, "Video completed");
            isAdPlaying = false;
            isAdPaused = false;
            savedAdPosition = 0;
            notifyImaOnContentCompleted();
        });

        videoPlayer.setOnErrorListener((mediaPlayer, errorType, extra) -> {
            SDKLogger.e(LOGTAG, "Video error: " + errorType + ", extra: " + extra);
            isAdPlaying = false;
            isAdPaused = false;
            notifyImaSdkAboutAdError(errorType);
            return false;
        });

        videoPlayer.setOnPreparedListener(mediaPlayer -> {
            SDKLogger.d(LOGTAG, "Video prepared successfully");
            isAdLoaded = true;
            adDuration = mediaPlayer.getDuration();
            if (savedAdPosition > 0) {
                mediaPlayer.seekTo(savedAdPosition);
            }
        });
    }

    @Override
    public void addCallback(@NonNull VideoAdPlayerCallback videoAdPlayerCallback) {
        if (videoAdPlayerCallback != null && !videoAdPlayerCallbacks.contains(videoAdPlayerCallback)) {
            videoAdPlayerCallbacks.add(videoAdPlayerCallback);
            SDKLogger.d(LOGTAG, "Callback added, total callbacks: " + videoAdPlayerCallbacks.size());
        }
    }

    @Override
    public void loadAd(@NonNull AdMediaInfo adMediaInfo, @NonNull AdPodInfo adPodInfo) {
        SDKLogger.i(LOGTAG, "Loading ad: " + adMediaInfo.getUrl());

        if (adMediaInfo == null) {
            SDKLogger.e(LOGTAG, "AdMediaInfo is null");
            return;
        }

        loadedAdMediaInfo = adMediaInfo;
        isAdLoaded = false;
        isAdPlaying = false;
        isAdPaused = false;

        SDKLogger.d(LOGTAG, "Ad loaded successfully");
    }

    @Override
    public void pauseAd(@NonNull AdMediaInfo adMediaInfo) {
        SDKLogger.i(LOGTAG, "Pausing ad");

        if (videoPlayer != null && isAdPlaying) {
            try {
                savedAdPosition = videoPlayer.getCurrentPosition();
                videoPlayer.pause();
                isAdPlaying = false;
                isAdPaused = true;
                stopAdTracking();
                SDKLogger.d(LOGTAG, "Ad paused at position: " + savedAdPosition);
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error pausing ad: " + e.getMessage(), e);
            }
        } else {
            SDKLogger.w(LOGTAG,
                    "Cannot pause ad: videoPlayer=" + (videoPlayer != null) + ", isAdPlaying=" + isAdPlaying);
        }
    }

    @Override
    public void playAd(AdMediaInfo adMediaInfo) {
        SDKLogger.i(LOGTAG, "Playing ad");

        if (videoPlayer == null) {
            SDKLogger.e(LOGTAG, "VideoPlayer is null, cannot play ad");
            notifyImaSdkAboutAdError(MediaPlayer.MEDIA_ERROR_UNSUPPORTED);
            return;
        }

        if (adMediaInfo == null) {
            SDKLogger.e(LOGTAG, "AdMediaInfo is null, cannot play ad");
            notifyImaSdkAboutAdError(MediaPlayer.MEDIA_ERROR_UNSUPPORTED);
            return;
        }

        try {
            String videoUrl = adMediaInfo.getUrl();
            SDKLogger.d(LOGTAG, "Loading video from URL: " + videoUrl);

            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                SDKLogger.e(LOGTAG, "Video URL is null or empty");
                notifyImaSdkAboutAdError(MediaPlayer.MEDIA_ERROR_UNSUPPORTED);
                return;
            }

            isAdPlaying = false;
            isAdPaused = false;
            isAdLoaded = false;

            if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                videoPlayer.setVideoPath(videoUrl);
            } else {

                videoPlayer.setVideoURI(Uri.parse(videoUrl));
            }

            videoPlayer.setOnPreparedListener(mediaPlayer -> {
                SDKLogger.d(LOGTAG, "Video prepared successfully");
                isAdLoaded = true;
                adDuration = mediaPlayer.getDuration();

                if (savedAdPosition > 0) {
                    mediaPlayer.seekTo(savedAdPosition);
                    SDKLogger.d(LOGTAG, "Seeking to saved position: " + savedAdPosition);
                }

                try {
                    mediaPlayer.start();
                    isAdPlaying = true;
                    isAdPaused = false;
                    startAdTracking();
                    SDKLogger.d(LOGTAG, "Ad started playing successfully");
                } catch (Exception e) {
                    SDKLogger.e(LOGTAG, "Error starting video playback: " + e.getMessage(), e);
                    notifyImaSdkAboutAdError(MediaPlayer.MEDIA_ERROR_UNSUPPORTED);
                }
            });

            videoPlayer.setOnErrorListener((mediaPlayer, errorType, extra) -> {
                SDKLogger.e(LOGTAG, "Video error: " + errorType + ", extra: " + extra);
                isAdPlaying = false;
                isAdPaused = false;
                isAdLoaded = false;
                notifyImaSdkAboutAdError(errorType);
                return false;
            });

            videoPlayer.setOnCompletionListener(mediaPlayer -> {
                SDKLogger.d(LOGTAG, "Video completed");
                isAdPlaying = false;
                isAdPaused = false;
                isAdLoaded = false;
                savedAdPosition = 0;
                stopAdTracking();
                notifyImaSdkAboutAdEnded();
            });

        } catch (Exception e) {
            SDKLogger.e(LOGTAG, "Error setting video source: " + e.getMessage(), e);
            isAdPlaying = false;
            isAdPaused = false;
            isAdLoaded = false;
            notifyImaSdkAboutAdError(MediaPlayer.MEDIA_ERROR_UNSUPPORTED);
        }
    }

    @Override
    public void release() {
        try {

            isReleased = true;

            stopAdTracking();

            if (videoPlayer != null) {
                videoPlayer.stopPlayback();
            }

            isAdPlaying = false;
            isAdPaused = false;
            isAdLoaded = false;
            savedAdPosition = 0;

            SDKLogger.d(LOGTAG, "VideoAdPlayerAdapter released successfully");
        } catch (Exception e) {
            SDKLogger.e(LOGTAG, "Error releasing VideoAdPlayerAdapter: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
        if (videoAdPlayerCallback != null) {
            videoAdPlayerCallbacks.remove(videoAdPlayerCallback);
            SDKLogger.d(LOGTAG, "Callback removed, total callbacks: " + videoAdPlayerCallbacks.size());
        }
    }

    @Override
    public void stopAd(AdMediaInfo adMediaInfo) {
        SDKLogger.i(LOGTAG, "Stopping ad");

        if (videoPlayer != null) {
            try {
                videoPlayer.stopPlayback();
                isAdPlaying = false;
                isAdPaused = false;
                isAdLoaded = false;
                savedAdPosition = 0;
                stopAdTracking();
                SDKLogger.d(LOGTAG, "Ad stopped successfully");
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error stopping ad: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Returns current volume as a percent of max volume.
     */
    @Override
    public int getVolume() {
        try {
            if (audioManager != null) {
                return (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100)
                        / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            }
        } catch (Exception e) {
            SDKLogger.e(LOGTAG, "Error getting volume: " + e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Check if ad is currently playing
     */
    public boolean isAdPlaying() {
        return isAdPlaying;
    }

    /**
     * Check if ad is currently paused
     */
    public boolean isAdPaused() {
        return isAdPaused;
    }

    /**
     * Check if ad is loaded and ready
     */
    public boolean isAdLoaded() {
        return isAdLoaded;
    }

    /**
     * Get current ad position
     */
    public int getCurrentPosition() {
        if (videoPlayer != null) {
            try {
                return videoPlayer.getCurrentPosition();
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error getting current position: " + e.getMessage(), e);
            }
        }
        return 0;
    }

    /**
     * Get ad duration
     */
    public int getAdDuration() {
        return adDuration;
    }

    private void startAdTracking() {
        SDKLogger.i(LOGTAG, "Starting ad tracking");
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        TimerTask updateTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    VideoProgressUpdate progressUpdate = getAdProgress();
                    notifyImaSdkAboutAdProgress(progressUpdate);
                } catch (Exception e) {
                    SDKLogger.e(LOGTAG, "Error in ad tracking timer: " + e.getMessage(), e);
                }
            }
        };
        timer.schedule(updateTimerTask, POLLING_TIME_MS, INITIAL_DELAY_MS);
    }

    private void notifyImaSdkAboutAdEnded() {
        if (isReleased) {
            SDKLogger.d(LOGTAG, "Skipping ad ended callback - adapter is released");
            return;
        }

        SDKLogger.i(LOGTAG, "Notifying IMA SDK about ad ended");
        savedAdPosition = 0;

        for (VideoAdPlayer.VideoAdPlayerCallback callback : videoAdPlayerCallbacks) {
            try {
                callback.onEnded(loadedAdMediaInfo);
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error notifying callback about ad ended: " + e.getMessage(), e);
            }
        }
    }

    private void notifyImaSdkAboutAdProgress(VideoProgressUpdate adProgress) {
        if (isReleased) {
            SDKLogger.d(LOGTAG, "Skipping ad progress callback - adapter is released");
            return;
        }

        for (VideoAdPlayer.VideoAdPlayerCallback callback : videoAdPlayerCallbacks) {
            try {
                callback.onAdProgress(loadedAdMediaInfo, adProgress);
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error notifying callback about ad progress: " + e.getMessage(), e);
            }
        }
    }

    /**
     * @param errorType Media player's error type as defined at
     *                  https://cs.android.com/android/platform/superproject/+/master:frameworks/base/media/java/android/media/MediaPlayer.java;l=4335
     * @return True to stop the current ad playback.
     */
    private boolean notifyImaSdkAboutAdError(int errorType) {
        if (isReleased) {
            SDKLogger.d(LOGTAG, "Skipping ad error callback - adapter is released");
            return true;
        }

        SDKLogger.i(LOGTAG, "Notifying IMA SDK about ad error: " + errorType);

        switch (errorType) {
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                SDKLogger.e(LOGTAG, "MEDIA_ERROR_UNSUPPORTED");
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                SDKLogger.e(LOGTAG, "MEDIA_ERROR_TIMED_OUT");
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                SDKLogger.e(LOGTAG, "MEDIA_ERROR_SERVER_DIED");
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                SDKLogger.e(LOGTAG, "MEDIA_ERROR_IO");
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                SDKLogger.e(LOGTAG, "MEDIA_ERROR_MALFORMED");
                break;
            default:
                SDKLogger.e(LOGTAG, "Unknown media error: " + errorType);
                break;
        }

        for (VideoAdPlayer.VideoAdPlayerCallback callback : videoAdPlayerCallbacks) {
            try {
                callback.onError(loadedAdMediaInfo);
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error notifying callback about ad error: " + e.getMessage(), e);
            }
        }
        return true;
    }

    public void notifyImaOnContentCompleted() {
        if (isReleased) {
            SDKLogger.d(LOGTAG, "Skipping content completed callback - adapter is released");
            return;
        }

        SDKLogger.i(LOGTAG, "Notifying IMA SDK about content completed");
        for (VideoAdPlayer.VideoAdPlayerCallback callback : videoAdPlayerCallbacks) {
            try {
                callback.onContentComplete();
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error notifying callback about content completed: " + e.getMessage(), e);
            }
        }
    }

    private void stopAdTracking() {
        SDKLogger.i(LOGTAG, "Stopping ad tracking");
        if (timer != null) {
            timer.cancel();
            timer.purge(); // Remove cancelled tasks from the timer's task queue
            timer = null;
        }
    }

    @Override
    public VideoProgressUpdate getAdProgress() {
        try {
            if (videoPlayer != null && isAdPlaying) {
                long adPosition = videoPlayer.getCurrentPosition();
                return new VideoProgressUpdate(adPosition, adDuration);
            }
        } catch (Exception e) {
            SDKLogger.e(LOGTAG, "Error getting ad progress: " + e.getMessage(), e);
        }
        return new VideoProgressUpdate(0, adDuration);
    }

    /**
     * Finalizer to ensure timer is properly disposed of
     * This is a safety measure in case normal cleanup methods aren't called
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            stopAdTracking();
        } finally {
            super.finalize();
        }
    }
}