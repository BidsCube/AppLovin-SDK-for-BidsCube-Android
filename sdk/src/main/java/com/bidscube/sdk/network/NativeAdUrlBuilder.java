package com.bidscube.sdk.network;

import android.net.Uri;
import com.bidscube.sdk.models.DeviceInfo;

public class NativeAdUrlBuilder {
    private final String placementId;
    private final DeviceInfo deviceInfo;

    private double adWidth;
    private double adHeight;

    public NativeAdUrlBuilder(String placementId, DeviceInfo deviceInfo, double adWidth, double adHeight) {
        this.placementId = placementId;
        this.deviceInfo = deviceInfo;
        this.adWidth = adWidth;
        this.adHeight = adHeight;
    }

    public Uri build() {
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority("ssp-bcc-ads.com")
                .appendPath("sdk");

        builder.appendQueryParameter("c", "n")
               .appendQueryParameter("m", "s")
               .appendQueryParameter("id", placementId)
               .appendQueryParameter("app", "1");

        builder.appendQueryParameter("bundle", deviceInfo.getBundle())
               .appendQueryParameter("name", deviceInfo.getAppName())
               .appendQueryParameter("app_version", deviceInfo.getAppVersion());

        builder.appendQueryParameter("ifa", deviceInfo.getIfa() != null ? deviceInfo.getIfa() : "")
               .appendQueryParameter("dnt", String.valueOf(deviceInfo.getDnt()));

        builder.appendQueryParameter("app_store_url", deviceInfo.getAppStoreUrl())
               .appendQueryParameter("ua", deviceInfo.getUserAgent());

        builder.appendQueryParameter("gdpr", deviceInfo.getGdpr())
               .appendQueryParameter("gdpr_consent", deviceInfo.getGdprConsent() != null ? deviceInfo.getGdprConsent() : "null")
               .appendQueryParameter("us_privacy", deviceInfo.getUsPrivacy() != null ? deviceInfo.getUsPrivacy() : "null")
               .appendQueryParameter("ccpa", deviceInfo.getUsPrivacy() != null ? deviceInfo.getUsPrivacy() : "null")
               .appendQueryParameter("coppa", deviceInfo.isCoppa());

        builder.appendQueryParameter("language", deviceInfo.getLanguage())
               .appendQueryParameter("deviceWidth", String.valueOf(deviceInfo.getDeviceWidth()))
               .appendQueryParameter("deviceHeight", String.valueOf(deviceInfo.getDeviceHeight()));

        builder.appendQueryParameter("w", String.valueOf(adWidth))
               .appendQueryParameter("h", String.valueOf(adHeight));
        
        return builder.build();
    }
}