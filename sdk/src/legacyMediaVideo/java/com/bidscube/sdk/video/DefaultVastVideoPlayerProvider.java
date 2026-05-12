package com.bidscube.sdk.video;

import android.content.Context;

public final class DefaultVastVideoPlayerProvider {

    private DefaultVastVideoPlayerProvider() {
    }

    public static BidscubeVastVideoPlayer create(Context context, String vastAdMarkup, String clickThroughRedirectUrl) {
        return new LegacyMediaVastVideoPlayer(context, vastAdMarkup, clickThroughRedirectUrl);
    }
}
