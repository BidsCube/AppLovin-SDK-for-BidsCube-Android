package com.bidscube.sdk.ads;

import android.net.Uri;

import com.bidscube.sdk.models.DeviceInfo;
import com.bidscube.sdk.network.ImageAdUrlBuilder;

public class ImageAdType implements AdType {
    private final String placementId;

    public ImageAdType(String placementId) {
        this.placementId = placementId;
    }

    @Override
    public Uri buildRequestUrl(DeviceInfo deviceInfo) {
        return new ImageAdUrlBuilder(placementId, deviceInfo).build();
    }
}

