package com.bidscube.sdk.models;

public class AdInfo {
    private final String adId;
    private final boolean limitAdTracking;

    public AdInfo(String adId, boolean limitAdTracking) {
        this.adId = adId;
        this.limitAdTracking = limitAdTracking;
    }

    public String getAdId() {
        return adId;
    }

    public boolean isLimitAdTracking() {
        return limitAdTracking;
    }
}
