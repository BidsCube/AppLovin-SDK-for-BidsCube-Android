package com.bidscube.sdk.video;

import android.content.Context;

import com.bidscube.sdk.view.IMAPlayerHandler;

public final class DefaultVastVideoPlayerProvider {

    private DefaultVastVideoPlayerProvider() {
    }

    public static BidscubeVastVideoPlayer create(Context context, String vastAdMarkup, String clickThroughRedirectUrl) {
        return new IMAPlayerHandler(vastAdMarkup, clickThroughRedirectUrl, context);
    }
}
