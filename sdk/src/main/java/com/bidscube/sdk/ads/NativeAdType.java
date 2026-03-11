package com.bidscube.sdk.ads;

import android.net.Uri;
import com.bidscube.sdk.models.DeviceInfo;
import com.bidscube.sdk.network.NativeAdUrlBuilder;

public class NativeAdType implements AdType {
    private final String placementId;

    public NativeAdType(String placementId) {
        this.placementId = placementId;
    }
    
    @Override
    public Uri buildRequestUrl(DeviceInfo deviceInfo) {
        return new NativeAdUrlBuilder(placementId, deviceInfo, 300, 50).build();
    }
}

