package com.bidscube.sdk.models;

import com.bidscube.sdk.ads.VideoAdFormat;

/**
 * Cached video ad response for preload-then-show mediation flows.
 */
public final class CachedVideoAd {

    private final String placementId;
    private final String adm;
    private final int position;
    private final VideoAdFormat format;

    public CachedVideoAd(String placementId, String adm, int position, VideoAdFormat format) {
        this.placementId = placementId;
        this.adm = adm;
        this.position = position;
        this.format = format;
    }

    public String getPlacementId() {
        return placementId;
    }

    public String getAdm() {
        return adm;
    }

    public int getPosition() {
        return position;
    }

    public VideoAdFormat getFormat() {
        return format;
    }
}
