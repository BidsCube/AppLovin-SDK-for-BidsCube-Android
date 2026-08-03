package com.bidscube.sdk.video;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Host-app–pluggable surface for VAST playback used by the SDK. The default implementation is
 * {@link com.bidscube.sdk.view.IMAPlayerHandler}; integrators may supply a {@link BidscubeVastVideoPlayerFactory}
 * via {@link com.bidscube.sdk.config.SDKConfig.Builder#vastVideoPlayerFactory(BidscubeVastVideoPlayerFactory)}.
 */
public abstract class BidscubeVastVideoPlayer extends FrameLayout {

    public BidscubeVastVideoPlayer(Context context) {
        super(context);
    }

    public BidscubeVastVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BidscubeVastVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public abstract void playVast(String vastTag, boolean isUrl);

    public abstract void setOnVideoCompletionListener(OnVideoCompletionListener listener);

    public abstract void release();

    public abstract void skipVideo();

    /**
     * When {@code true}, the player may continue showing post-linear content (e.g. Google IMA interactive end card)
     * after {@link OnVideoCompletionListener#onVideoCompleted()}. The SDK must not release the player until the user
     * closes the fullscreen ad or the player fires {@link OnVideoCompletionListener#onAdSessionCompleted()}.
     */
    public boolean managesPostVideoExperience() {
        return false;
    }

    public boolean isVideoSupported() {
        return true;
    }

    public int getUnsupportedErrorCode() {
        return -1;
    }

    public String getUnsupportedErrorMessage() {
        return "Video playback is not supported in this build.";
    }

    public interface OnVideoCompletionListener {
        /** Linear VAST / IMA {@code COMPLETED} — not the same as fullscreen ad close. */
        void onVideoCompleted();

        void onVideoSkipped();

        /**
         * Entire ad session finished (IMA {@code ALL_ADS_COMPLETED}). Default players fire this immediately after
         * {@link #onVideoCompleted()} because they do not manage post-linear UI.
         */
        default void onAdSessionCompleted() {
        }

        /**
         * Playback failed before a normal terminal event. Default players may omit this.
         */
        default void onVideoPlaybackFailed() {
        }
    }
}
