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

    public interface OnVideoCompletionListener {
        void onVideoCompleted();

        void onVideoSkipped();
    }
}
