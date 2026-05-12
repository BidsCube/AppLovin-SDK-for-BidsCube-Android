package com.bidscube.sdk.adapters;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.VideoView;

import androidx.annotation.NonNull;

import com.bidscube.sdk.utils.SDKLogger;
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
            isAdPlaying = false;
            isAdPaused = false;
            savedAdPosition = 0;
            notifyImaOnContentCompleted();
        });

        videoPlayer.setOnErrorListener((mediaPlayer, errorType, extra) -> {
            isAdPlaying = false;
            isAdPaused = false;
            notifyImaSdkAboutAdError(errorType);
            return false;
        });

        videoPlayer.setOnPreparedListener(mediaPlayer -> {
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
        }
    }

    @Override
    public void loadAd(@NonNull AdMediaInfo adMediaInfo, @NonNull AdPodInfo adPodInfo) {
        loadedAdMediaInfo = adMediaInfo;
        isAdLoaded = false;
        isAdPlaying = false;
        isAdPaused = false;
    }

    @Override
    public void pauseAd(@NonNull AdMediaInfo adMediaInfo) {
        if (videoPlayer != null && isAdPlaying) {
            try {
                savedAdPosition = videoPlayer.getCurrentPosition();
                videoPlayer.pause();
                isAdPlaying = false;
                isAdPaused = true;
                stopAdTracking();
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error pausing ad: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void playAd(AdMediaInfo adMediaInfo) {
        if (videoPlayer == null || adMediaInfo == null) {
            notifyImaSdkAboutAdError(MediaPlayer.MEDIA_ERROR_UNSUPPORTED);
            return;
        }

        try {
            String videoUrl = adMediaInfo.getUrl();
            if (videoUrl == null || videoUrl.trim().isEmpty()) {
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
                isAdLoaded = true;
                adDuration = mediaPlayer.getDuration();
                if (savedAdPosition > 0) {
                    mediaPlayer.seekTo(savedAdPosition);
                }
                try {
                    mediaPlayer.start();
                    isAdPlaying = true;
                    isAdPaused = false;
                    startAdTracking();
                } catch (Exception e) {
                    SDKLogger.e(LOGTAG, "Error starting video playback: " + e.getMessage(), e);
                    notifyImaSdkAboutAdError(MediaPlayer.MEDIA_ERROR_UNSUPPORTED);
                }
            });

            videoPlayer.setOnErrorListener((mediaPlayer, errorType, extra) -> {
                isAdPlaying = false;
                isAdPaused = false;
                isAdLoaded = false;
                notifyImaSdkAboutAdError(errorType);
                return false;
            });

            videoPlayer.setOnCompletionListener(mediaPlayer -> {
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
        } catch (Exception e) {
            SDKLogger.e(LOGTAG, "Error releasing VideoAdPlayerAdapter: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
        if (videoAdPlayerCallback != null) {
            videoAdPlayerCallbacks.remove(videoAdPlayerCallback);
        }
    }

    @Override
    public void stopAd(AdMediaInfo adMediaInfo) {
        if (videoPlayer != null) {
            try {
                videoPlayer.stopPlayback();
                isAdPlaying = false;
                isAdPaused = false;
                isAdLoaded = false;
                savedAdPosition = 0;
                stopAdTracking();
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error stopping ad: " + e.getMessage(), e);
            }
        }
    }

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

    private void startAdTracking() {
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
            return;
        }
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

    private boolean notifyImaSdkAboutAdError(int errorType) {
        if (isReleased) {
            return true;
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
            return;
        }
        for (VideoAdPlayer.VideoAdPlayerCallback callback : videoAdPlayerCallbacks) {
            try {
                callback.onContentComplete();
            } catch (Exception e) {
                SDKLogger.e(LOGTAG, "Error notifying callback about content completed: " + e.getMessage(), e);
            }
        }
    }

    private void stopAdTracking() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
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

    @Override
    protected void finalize() throws Throwable {
        try {
            stopAdTracking();
        } finally {
            super.finalize();
        }
    }
}
