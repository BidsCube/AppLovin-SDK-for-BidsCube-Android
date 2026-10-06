package com.bidscube.sdk.video;

/**
 * Process-wide flag read by the IMA player (fullVideo flavor) when SDK debug mode is on.
 */
public final class VideoPlaybackDebug {

    public static volatile boolean imaDebugMode;

    private VideoPlaybackDebug() {
    }
}
