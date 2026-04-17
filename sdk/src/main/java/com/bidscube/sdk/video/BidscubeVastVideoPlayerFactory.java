package com.bidscube.sdk.video;

import android.content.Context;

/**
 * Creates {@link BidscubeVastVideoPlayer} instances for each video ad load. Set on
 * {@link com.bidscube.sdk.config.SDKConfig.Builder#vastVideoPlayerFactory(BidscubeVastVideoPlayerFactory)}.
 */
public interface BidscubeVastVideoPlayerFactory {

    /**
     * @param vastAdMarkup             Inline VAST XML from the bid response (same string later passed to
     *                                 {@link BidscubeVastVideoPlayer#playVast(String, boolean)} with {@code isUrl=false}).
     * @param clickThroughRedirectUrl  Best-effort click-through URL from parsed VAST; may be empty.
     */
    BidscubeVastVideoPlayer create(Context context, String vastAdMarkup, String clickThroughRedirectUrl);
}
