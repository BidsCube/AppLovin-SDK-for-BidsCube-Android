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
        boolean hasInlineMediaFile = VastParser.validateVastStructure(vastAdMarkup);
        if (hasInlineMediaFile) {
            Log.i(INTEGRATION, "VAST player selection: ProgressiveMp4VastVideoPlayer"
                    + " reason=inline MediaFile present (IMA mini-game unavailable for this creative)");
            return new ProgressiveMp4VastVideoPlayer(context, vastAdMarkup, clickThroughRedirectUrl);
        }
        if (isImaAvailable()) {
            Log.i(INTEGRATION, "VAST player selection: IMAPlayerHandler reason=no inline MediaFile");
            return new IMAPlayerHandler(vastAdMarkup, clickThroughRedirectUrl, context);
        }
        Log.w(INTEGRATION, "VAST player selection: ProgressiveMp4VastVideoPlayer fallback (IMA not on classpath)");
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
