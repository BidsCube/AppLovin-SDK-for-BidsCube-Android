package com.bidscube.sdk.models;

/**
 * Immutable device information model.
 * Includes device info, app info, and GDPR/CCPA/COPPA compliance fields.
 */
public class DeviceInfo {

    private final String bundle;
    private final String appName;
    private final String appStoreUrl;
    private final String language;
    private final int deviceWidth;
    private final int deviceHeight;
    private final String userAgent;
    private final String ifa;
    private final int dnt;
    private final String appVersion;

    private final int gdpr;
    private final String gdprConsent;
    private final String usPrivacy;
    private final boolean coppa;

    private static final int DEFAULT_GDPR = 0;
    private static final String DEFAULT_GDPR_CONSENT = "";
    private static final String DEFAULT_US_PRIVACY = "";
    private static final boolean DEFAULT_COPPA = false;

    /**
     * Full constructor with all fields.
     */
    public DeviceInfo(String bundle, String appName, String appStoreUrl, String language,
                      int deviceWidth, int deviceHeight, String userAgent, String ifa,
                      int dnt, String appVersion,
                      int gdpr, String gdprConsent, String usPrivacy, boolean coppa) {
        this.bundle = bundle;
        this.appName = appName;
        this.appStoreUrl = appStoreUrl;
        this.language = language;
        this.deviceWidth = deviceWidth;
        this.deviceHeight = deviceHeight;
        this.userAgent = userAgent;
        this.ifa = ifa;
        this.dnt = dnt;
        this.appVersion = appVersion;
        this.gdpr = gdpr;
        this.gdprConsent = gdprConsent;
        this.usPrivacy = usPrivacy;
        this.coppa = coppa;
    }

    /**
     * Simplified constructor with GDPR/CCPA/COPPA defaults.
     */
    public DeviceInfo(String bundle, String appName, String appStoreUrl, String language,
                      int deviceWidth, int deviceHeight, String userAgent, String ifa,
                      int dnt, String appVersion) {
        this(bundle, appName, appStoreUrl, language, deviceWidth, deviceHeight,
                userAgent, ifa, dnt, appVersion,
                DEFAULT_GDPR, DEFAULT_GDPR_CONSENT, DEFAULT_US_PRIVACY, DEFAULT_COPPA);
    }

    public String getBundle() {
        return bundle;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppStoreUrl() {
        return appStoreUrl;
    }

    public String getLanguage() {
        return language;
    }

    public int getDeviceWidth() {
        return deviceWidth;
    }

    public int getDeviceHeight() {
        return deviceHeight;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIfa() {
        return ifa;
    }

    public int getDnt() {
        return dnt;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getGdpr() {
        return String.valueOf(gdpr);
    }

    public String getGdprConsent() {
        return gdprConsent;
    }

    public String getUsPrivacy() {
        return usPrivacy;
    }

    public String isCoppa() {
        return coppa ? "1" : "0";
    }
}
