package com.bidscube.sdk.video;

import android.content.Context;
import android.util.AttributeSet;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Video-disabled implementation used by LiteNoVideo.
 */
public final class NoOpVastVideoPlayer extends BidscubeVastVideoPlayer {

    public static final int ERROR_VIDEO_NOT_SUPPORTED = 1006;

    private final String unsupportedMessage;
    private OnVideoCompletionListener completionListener;
    private final AtomicBoolean skipped = new AtomicBoolean(false);

    public NoOpVastVideoPlayer(Context context, String unsupportedMessage) {
        super(context);
        this.unsupportedMessage = unsupportedMessage;
    }

    public NoOpVastVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.unsupportedMessage = "Video playback is not supported in this build.";
    }

    @Override
    public void playVast(String vastTag, boolean isUrl) {
        // Intentionally no-op. AdDisplayManager should detect unsupported players before calling into them.
    }

    @Override
    public void setOnVideoCompletionListener(OnVideoCompletionListener listener) {
        completionListener = listener;
    }

    @Override
    public void release() {
        // no-op
    }

    @Override
    public void skipVideo() {
        if (completionListener != null && skipped.compareAndSet(false, true)) {
            completionListener.onVideoSkipped();
        }
    }

    @Override
    public boolean isVideoSupported() {
        return false;
    }

    @Override
    public int getUnsupportedErrorCode() {
        return ERROR_VIDEO_NOT_SUPPORTED;
    }

    @Override
    public String getUnsupportedErrorMessage() {
        return unsupportedMessage;
    }
}
