package com.bidscube.sdk.network;

import android.net.Uri;
import com.bidscube.sdk.models.DeviceInfo;

public class ImageAdUrlBuilder {
    private final String placementId;
    private final DeviceInfo deviceInfo;

    public ImageAdUrlBuilder(String placementId, DeviceInfo deviceInfo) {
        this.placementId = placementId;
        this.deviceInfo = deviceInfo;
    }

    public Uri build() {
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority("ssp-bcc-ads.com")
                .appendPath("sdk");

        builder.appendQueryParameter("placementId", placementId)
               .appendQueryParameter("c", "b")
               .appendQueryParameter("m", "api")
               .appendQueryParameter("res", "js")
               .appendQueryParameter("app", "1");

        builder.appendQueryParameter("bundle", deviceInfo.getBundle())
               .appendQueryParameter("name", deviceInfo.getAppName())
               .appendQueryParameter("app_store_url", deviceInfo.getAppStoreUrl())
               .appendQueryParameter("language", deviceInfo.getLanguage());

        builder.appendQueryParameter("deviceWidth", String.valueOf(deviceInfo.getDeviceWidth()))
               .appendQueryParameter("deviceHeight", String.valueOf(deviceInfo.getDeviceHeight()));

        builder.appendQueryParameter("ua", deviceInfo.getUserAgent())
               .appendQueryParameter("ifa", deviceInfo.getIfa())
               .appendQueryParameter("dnt", String.valueOf(deviceInfo.getDnt()));
        
        return builder.build();
    }
}