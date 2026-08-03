package com.bidscube.sdk.config;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.WebSettings;

import com.bidscube.sdk.video.BidscubeVastVideoPlayerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Configuration class for the Bidscube SDK
 */
public class SDKConfig {

    public static final String DEFAULT_AD_REQUEST_AUTHORITY = com.bidscube.sdk.models.DeviceInfo.DEFAULT_AD_REQUEST_AUTHORITY;

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
    /** HTTPS authority (host[:port]) for ad requests; default {@link #DEFAULT_AD_REQUEST_AUTHORITY}. */
    private final String adRequestAuthority;
    /**
     * Optional HTTPS authority for SDK-originated statistics beacons ({@code GET …/collect?…}).
     * {@code null} = disabled (default); no traffic to a stats backend unless the integrator sets this.
     */
    private final String statsRequestAuthority;
    /** When {@code null}, the SDK uses the built-in Google IMA–based player ({@link com.bidscube.sdk.view.IMAPlayerHandler}). */
    private final BidscubeVastVideoPlayerFactory vastVideoPlayerFactory;
    /** Optional publisher user identifier; sent as {@code user_id} on ad requests when set. */
    private final String userId;
    /**
     * When {@code false} (default), fullscreen video remains open after linear playback so IMA post-roll / mini-game
     * or a VAST Companion can display; {@link com.bidscube.sdk.interfaces.AdCallback#onAdClosed(String)} fires only
     * after the user closes the ad. When {@code true}, the fullscreen ad closes immediately after linear video ends
     * or is skipped (no post-video content).
     */
    private final boolean autoClose;

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
        this.adRequestAuthority = builder.adRequestAuthority != null && !builder.adRequestAuthority.isEmpty()
                ? builder.adRequestAuthority
                : DEFAULT_AD_REQUEST_AUTHORITY;
        this.statsRequestAuthority = builder.statsRequestAuthority;
        this.vastVideoPlayerFactory = builder.vastVideoPlayerFactory;
        this.userId = normalizeUserId(builder.userId);
        this.autoClose = builder.autoClose;
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
     * Host (and optional port) for Bidscube SSP ad URLs, e.g. {@code ssp-bcc-ads.com} or {@code edge.example.com:8443}.
     */
    public String getAdRequestAuthority() {
        return adRequestAuthority;
    }

    /**
     * Optional host (and port) for SDK statistics only. {@code null} or unset means the SDK sends no such beacons.
     */
    public String getStatsRequestAuthority() {
        return statsRequestAuthority;
    }

    /**
     * Optional factory for VAST video playback. {@code null} means the default IMA-backed player.
     */
    public BidscubeVastVideoPlayerFactory getVastVideoPlayerFactory() {
        return vastVideoPlayerFactory;
    }

    /**
     * Optional publisher-defined user id. When non-empty, included as {@code user_id} on SSP ad requests.
     */
    public String getUserId() {
        return userId;
    }

    /** @see Builder#autoClose(boolean) */
    public boolean isAutoClose() {
        return autoClose;
    }

    private static String normalizeUserId(String userId) {
        if (userId == null) {
            return null;
        }
        String trimmed = userId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** @return copy with a different {@link #getUserId()} (other fields unchanged). */
    public SDKConfig withUserId(String userId) {
        Builder b = new Builder(null);
        b.appId = this.appId;
        b.appName = this.appName;
        b.appVersion = this.appVersion;
        b.language = this.language;
        b.userAgent = this.userAgent;
        b.enableLogging = this.enableLogging;
        b.enableDebugMode = this.enableDebugMode;
        b.defaultAdTimeout = this.defaultAdTimeout;
        b.defaultAdPosition = this.defaultAdPosition;
        b.gdpr = this.gdpr;
        b.gdprConsent = this.gdprConsent;
        b.usPrivacy = this.usPrivacy;
        b.coppa = this.coppa;
        b.adRequestAuthority = this.adRequestAuthority;
        b.statsRequestAuthority = this.statsRequestAuthority;
        b.vastVideoPlayerFactory = this.vastVideoPlayerFactory;
        b.userId = userId;
        b.autoClose = this.autoClose;
        return b.build();
    }

    /** Fallback app version label when PackageManager lookup fails; matches published AAR ({@link com.bidscube.sdk.BuildConfig#SDK_VERSION_NAME}). */
    private static String embeddedSdkVersionLabel() {
        return com.bidscube.sdk.BuildConfig.SDK_VERSION_NAME;
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
        private String adRequestAuthority = DEFAULT_AD_REQUEST_AUTHORITY;
        /** {@code null} = stats beacons disabled */
        private String statsRequestAuthority = null;
        private BidscubeVastVideoPlayerFactory vastVideoPlayerFactory = null;
        private String userId = null;
        private boolean autoClose = false;

        /**
         * Create a new Builder with automatic app detection
         *
         * @param context Application context for automatic detection; {@code null} skips auto-detect (internal copy builders).
         */
        public Builder(Context context) {
            if (context != null) {
                autoDetectAppInfo(context);
            }
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
                this.appVersion = embeddedSdkVersionLabel();
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
         * Override the HTTPS host (and optional port) used for all Bidscube ad request URLs.
         * Accepts {@code host}, {@code host:port}, IPv6 {@code [addr]:port}, a full prefix such as
         * {@code https://edge.example.com/sdk} (scheme and path are stripped), or percent-encoded
         * input (e.g. {@code %3A} for {@code :}). The SDK appends path {@code /sdk} and query parameters internally.
         */
        public Builder adRequestAuthority(String authorityOrUrl) {
            this.adRequestAuthority = normalizeAdRequestAuthority(authorityOrUrl);
            return this;
        }

        /**
         * Optional HTTPS authority for SDK statistics ({@code GET https://&lt;host&gt;/collect?e=…}).
         * Same input rules as {@link #adRequestAuthority(String)} (host, host:port, pasted URL prefix).
         * If unset or cleared, the SDK performs no stats beacons.
         */
        public Builder statsRequestAuthority(String authorityOrUrl) {
            this.statsRequestAuthority = normalizeOptionalStatsAuthority(authorityOrUrl);
            return this;
        }

        /**
         * Supply a custom {@link BidscubeVastVideoPlayerFactory} for VAST playback (e.g. ExoPlayer or another IMA shell).
         * {@code null} clears a previously set value and restores the default built-in player.
         */
        public Builder vastVideoPlayerFactory(BidscubeVastVideoPlayerFactory factory) {
            this.vastVideoPlayerFactory = factory;
            return this;
        }

        /**
         * Publisher user identifier for postback attribution. Sent as {@code user_id} query parameter on ad requests.
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Controls fullscreen video auto-close after linear playback ends or is skipped.
         * <ul>
         *   <li>{@code false} (default) — keep the ad open for post-video content; user closes manually.</li>
         *   <li>{@code true} — dismiss immediately after linear video (legacy behavior).</li>
         * </ul>
         * Immutable after {@link #build()}; set only at SDK init.
         */
        public Builder autoClose(boolean autoClose) {
            this.autoClose = autoClose;
            return this;
        }

        private static String normalizeAdRequestAuthority(String input) {
            if (input == null) {
                return DEFAULT_AD_REQUEST_AUTHORITY;
            }
            String stripped = stripAuthorityInput(input);
            return stripped.isEmpty() ? DEFAULT_AD_REQUEST_AUTHORITY : stripped;
        }

        /**
         * @return {@code null} when disabled / blank after normalization (never falls back to the default ad host).
         */
        private static String normalizeOptionalStatsAuthority(String input) {
            if (input == null) {
                return null;
            }
            String stripped = stripAuthorityInput(input);
            return stripped.isEmpty() ? null : stripped;
        }

        private static String stripAuthorityInput(String input) {
            String s = input.trim();
            if (s.isEmpty()) {
                return "";
            }
            // Paste from browser / config files may contain percent-encoding (e.g. %3A for ':').
            for (int i = 0; i < 3; i++) {
                try {
                    String dec = URLDecoder.decode(s, "UTF-8");
                    if (dec.equals(s)) {
                        break;
                    }
                    s = dec.trim();
                } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                    break;
                }
            }
            if (s.regionMatches(true, 0, "https://", 0, 8)) {
                s = s.substring(8);
            } else if (s.regionMatches(true, 0, "http://", 0, 7)) {
                s = s.substring(7);
            }
            int slash = s.indexOf('/');
            if (slash > 0) {
                s = s.substring(0, slash);
            }
            int q = s.indexOf('?');
            if (q > 0) {
                s = s.substring(0, q);
            }
            return s.trim();
        }

        /**
         * Build the SDKConfig
         */
        public SDKConfig build() {
            return new SDKConfig(this);
        }
    }
}
