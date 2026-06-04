package com.bidscube.sdk.video;

import android.content.Context;
import android.util.Log;

import com.bidscube.sdk.utils.VastParser;
import com.bidscube.sdk.view.IMAPlayerHandler;

public final class DefaultVastVideoPlayerProvider {

    private static final String INTEGRATION = "BidscubeIntegration";

    private DefaultVastVideoPlayerProvider() {
    }

    public static BidscubeVastVideoPlayer create(Context context, String vastAdMarkup, String clickThroughRedirectUrl) {
        if (VastParser.validateVastStructure(vastAdMarkup)) {
            Log.i(INTEGRATION, "VAST player: progressive MP4 (inline MediaFile, no IMA required)");
            return new ProgressiveMp4VastVideoPlayer(context, vastAdMarkup, clickThroughRedirectUrl);
        }
        if (isImaAvailable()) {
            Log.i(INTEGRATION, "VAST player: Google IMA (no inline MediaFile)");
            return new IMAPlayerHandler(vastAdMarkup, clickThroughRedirectUrl, context);
        }
        Log.w(INTEGRATION, "VAST player: IMA not on classpath; trying progressive MP4 anyway");
        return new ProgressiveMp4VastVideoPlayer(context, vastAdMarkup, clickThroughRedirectUrl);
    }

    private static boolean isImaAvailable() {
        try {
            Class.forName("com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
