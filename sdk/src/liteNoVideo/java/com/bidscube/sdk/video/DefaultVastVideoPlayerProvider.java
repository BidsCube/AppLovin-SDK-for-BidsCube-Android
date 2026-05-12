package com.bidscube.sdk.video;

import android.content.Context;

public final class DefaultVastVideoPlayerProvider {

    private DefaultVastVideoPlayerProvider() {
    }

    public static BidscubeVastVideoPlayer create(Context context, String vastAdMarkup, String clickThroughRedirectUrl) {
        return new NoOpVastVideoPlayer(context,
                "Video playback is disabled in LiteNoVideo. Use WebViewVideoNoDesugar, LegacyMediaVideoNoDesugar, or FullWithVideo.");
    }
}
