package com.bidscube.sdk.network;

import android.net.Uri;

import com.bidscube.sdk.models.DeviceInfo;

public class VideoAdUrlBuilder {
    private final String id;
    private final DeviceInfo deviceInfo;

    public VideoAdUrlBuilder(String id, DeviceInfo deviceInfo) {
        this.id = id;
        this.deviceInfo = deviceInfo;
    }

    public Uri build() {
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority("ssp-bcc-ads.com")
                .appendPath("sdk");

        builder.appendQueryParameter("c", "v")
               .appendQueryParameter("m", "xml")
               .appendQueryParameter("id", id)
               .appendQueryParameter("app", "1");

        builder.appendQueryParameter("w", String.valueOf(deviceInfo.getDeviceWidth()))
               .appendQueryParameter("h", String.valueOf(deviceInfo.getDeviceHeight()));

        builder.appendQueryParameter("bundle", deviceInfo.getBundle())
               .appendQueryParameter("name", deviceInfo.getAppName())
               .appendQueryParameter("app_version", deviceInfo.getAppVersion() != null ? deviceInfo.getAppVersion() : "");

        builder.appendQueryParameter("ifa", deviceInfo.getIfa())
               .appendQueryParameter("dnt", String.valueOf(deviceInfo.getDnt()))
               .appendQueryParameter("app_store_url", deviceInfo.getAppStoreUrl())
               .appendQueryParameter("ua", deviceInfo.getUserAgent())
               .appendQueryParameter("language", deviceInfo.getLanguage())
               .appendQueryParameter("deviceWidth", String.valueOf(deviceInfo.getDeviceWidth()))
               .appendQueryParameter("deviceHeight", String.valueOf(deviceInfo.getDeviceHeight()));
        
        return builder.build();
    }
}