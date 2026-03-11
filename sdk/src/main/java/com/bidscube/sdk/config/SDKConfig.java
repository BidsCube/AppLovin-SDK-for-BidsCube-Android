package com.bidscube.sdk.config;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.WebSettings;

/**
 * Configuration class for the Bidscube SDK
 */
public class SDKConfig {

    private final String appId;
    private final String appName;
    private final String appVersion;
    private final String language;
    private final String userAgent;
    private final boolean enableLogging;
    private final boolean enableDebugMode;
    private final int defaultAdTimeout;
    private final String defaultAdPosition;

    // Consent parameters
    private final Integer gdpr;
    private final String gdprConsent;
    private final String usPrivacy;
    private final Boolean coppa;

    private SDKConfig(Builder builder) {
        this.appId = builder.appId;
        this.appName = builder.appName;
        this.appVersion = builder.appVersion;
        this.language = builder.language;
        this.userAgent = builder.userAgent;
        this.enableLogging = builder.enableLogging;
        this.enableDebugMode = builder.enableDebugMode;
        this.defaultAdTimeout = builder.defaultAdTimeout;
        this.defaultAdPosition = builder.defaultAdPosition;
        this.gdpr = builder.gdpr;
        this.gdprConsent = builder.gdprConsent;
        this.usPrivacy = builder.usPrivacy;
        this.coppa = builder.coppa;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getLanguage() {
        return language;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public boolean isEnableDebugMode() {
        return enableDebugMode;
    }

    public int getDefaultAdTimeout() {
        return defaultAdTimeout;
    }

    public String getDefaultAdPosition() {
        return defaultAdPosition;
    }

    public Integer getGdpr() {
        return gdpr;
    }

    public String getGdprConsent() {
        return gdprConsent;
    }

    public String getUsPrivacy() {
        return usPrivacy;
    }

    public Boolean getCoppa() {
        return coppa;
    }

    /**
     * Get the SDK version from environment variable or default to 1.0.1
     */
    private static String getSDKVersion() {
        return System.getenv().getOrDefault("BidscubeVersion", "1.0.1");
    }

    /**
     * Builder class for SDKConfig with automatic app detection
     */
    public static class Builder {
        private String appId;
        private String appName;
        private String appVersion;
        private String language = "en";
        private String userAgent;
        private boolean enableLogging = true;
        private boolean enableDebugMode = false;
        private int defaultAdTimeout = 15000;
        private String defaultAdPosition = "UNKNOWN";

        // Consent parameters (null means use ConsentManager defaults)
        private Integer gdpr = null;
        private String gdprConsent = null;
        private String usPrivacy = null;
        private Boolean coppa = null;

        /**
         * Create a new Builder with automatic app detection
         *
         * @param context Application context for automatic detection
         */
        public Builder(Context context) {
            autoDetectAppInfo(context);
        }

        /**
         * Automatically detect app information from Android manifest and system
         */
        private void autoDetectAppInfo(Context context) {
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);

                this.appId = context.getPackageName();

                this.appName = pm.getApplicationLabel(pm.getApplicationInfo(context.getPackageName(), 0)).toString();

                this.appVersion = packageInfo.versionName != null ? packageInfo.versionName
                        : String.valueOf(packageInfo.versionCode);

                this.language = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
                this.userAgent = buildDefaultUserAgent(context);

            } catch (Exception e) {

                this.appId = "unknown_app";
                this.appName = "Unknown App";
                this.appVersion = getSDKVersion();
                this.language = "en";
                this.userAgent = buildDefaultUserAgent(context);
            }
        }

        private String buildDefaultUserAgent(Context context) {
            try {
                String webViewUa = WebSettings.getDefaultUserAgent(context);
                if (webViewUa != null && !webViewUa.trim().isEmpty()) {
                    return webViewUa;
                }
            } catch (Throwable ignored) {
                // Fall through to secondary UA sources.
            }

            String httpAgent = System.getProperty("http.agent");
            if (httpAgent != null && !httpAgent.trim().isEmpty()) {
                return httpAgent;
            }

            return "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL
                    + ") AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36";
        }

        /**
         * Override auto-detected app ID
         */
        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        /**
         * Override auto-detected app name
         */
        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        /**
         * Override auto-detected app version
         */
        public Builder appVersion(String appVersion) {
            this.appVersion = appVersion;
            return this;
        }

        /**
         * Override auto-detected language
         */
        public Builder language(String language) {
            this.language = language;
            return this;
        }

        /**
         * Override auto-detected user agent
         */
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Enable or disable logging
         */
        public Builder enableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
            return this;
        }

        /**
         * Enable or disable debug mode
         */
        public Builder enableDebugMode(boolean enableDebugMode) {
            this.enableDebugMode = enableDebugMode;
            return this;
        }

        /**
         * Set default ad timeout in milliseconds
         */
        public Builder defaultAdTimeout(int timeoutMs) {
            this.defaultAdTimeout = timeoutMs;
            return this;
        }

        /**
         * Set default ad position
         */
        public Builder defaultAdPosition(String position) {
            this.defaultAdPosition = position;
            return this;
        }

        /**
         * Set GDPR applies value (0 = no, 1 = yes, null = use ConsentManager)
         */
        public Builder gdpr(Integer gdpr) {
            this.gdpr = gdpr;
            return this;
        }

        /**
         * Set GDPR consent string (null = use ConsentManager)
         */
        public Builder gdprConsent(String gdprConsent) {
            this.gdprConsent = gdprConsent;
            return this;
        }

        /**
         * Set US Privacy string (null = use ConsentManager)
         */
        public Builder usPrivacy(String usPrivacy) {
            this.usPrivacy = usPrivacy;
            return this;
        }

        /**
         * Set COPPA compliance (null = use ConsentManager)
         */
        public Builder coppa(Boolean coppa) {
            this.coppa = coppa;
            return this;
        }

        /**
         * Build the SDKConfig
         */
        public SDKConfig build() {
            return new SDKConfig(this);
        }
    }
}
