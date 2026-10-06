package com.bidscube.sdk.video;

/**
 * Dismiss surface for a fullscreen video. Implemented by {@code FullscreenVideoAdActivity}
 * so the host app activity is paused for the duration of the ad.
 */
public interface FullscreenAdHost {

    boolean isShowing();

    void dismiss();

    void setOnDismissListener(Runnable listener);
}
