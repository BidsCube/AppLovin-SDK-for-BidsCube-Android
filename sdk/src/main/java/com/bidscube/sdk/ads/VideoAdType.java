package com.bidscube.sdk.ads;

import android.net.Uri;

import com.bidscube.sdk.models.DeviceInfo;
import com.bidscube.sdk.network.VideoAdUrlBuilder;

public class VideoAdType implements AdType {
    private final String placementId;

    public VideoAdType(String placementId) {
        this.placementId = placementId;
    }

    @Override
    public Uri buildRequestUrl(DeviceInfo deviceInfo) {
        return new VideoAdUrlBuilder(placementId, deviceInfo).build();
    }
}

