package com.bidscube.sdk.network;

import android.net.Uri;

import com.bidscube.sdk.models.DeviceInfo;

/**
 * Builds the base {@code https://&lt;authority&gt;/sdk} URI for Bidscube ad requests.
 * <p>
 * {@link Uri.Builder#authority(String)} encodes {@code :} in {@code host:port}, which breaks
 * {@link java.net.URL} / {@link java.net.HttpURLConnection}; this helper splits host and port and
 * uses {@link Uri.Builder#port(int)} when the suffix is a valid TCP port.
 */
public final class SspAdUriHelper {

    private SspAdUriHelper() {
    }

    /**
     * @return {@code Uri.Builder} with scheme {@code https}, authority (and optional port), and path segment {@code sdk}.
     */
    public static Uri.Builder newHttpsSdkUriBuilder(DeviceInfo deviceInfo) {
        String authority = deviceInfo != null ? deviceInfo.getAdRequestAuthority() : null;
        return applyHttpsAuthority(new Uri.Builder().scheme("https"), authority).appendPath("sdk");
    }

    /**
     * {@code https://&lt;authority&gt;/&lt;pathSegment&gt;} for optional stats (or other) endpoints.
     * {@code authority} must be non-blank; callers typically gate on {@link com.bidscube.sdk.config.SDKConfig#getStatsRequestAuthority()}.
     */
    public static Uri.Builder newHttpsPathUriBuilder(String authority, String pathSegment) {
        if (authority == null || authority.trim().isEmpty()) {
            throw new IllegalArgumentException("authority required");
        }
        if (pathSegment == null || pathSegment.trim().isEmpty()) {
            throw new IllegalArgumentException("pathSegment required");
        }
        return applyHttpsAuthority(new Uri.Builder().scheme("https"), authority.trim())
                .appendPath(pathSegment.trim());
    }

    static Uri.Builder applyHttpsAuthority(Uri.Builder builder, String authority) {
        if (authority == null || authority.isEmpty()) {
            return builder.authority(DeviceInfo.DEFAULT_AD_REQUEST_AUTHORITY);
        }
        String a = authority.trim();
        if (a.isEmpty()) {
            return builder.authority(DeviceInfo.DEFAULT_AD_REQUEST_AUTHORITY);
        }

        // IPv6 [addr]:port
        if (a.startsWith("[")) {
            int close = a.indexOf(']');
            if (close > 1 && a.length() > close + 1 && a.charAt(close + 1) == ':') {
                String inside = a.substring(1, close);
                String portStr = a.substring(close + 2);
                if (portStr.matches("\\d{1,5}")) {
                    int p = Integer.parseInt(portStr);
                    if (p >= 0 && p <= 65535) {
                        // encodedAuthority: host:port without encoding ':' between host and port (valid URI authority)
                        return builder.encodedAuthority("[" + inside + "]:" + p);
                    }
                }
            }
            if (a.endsWith("]")) {
                return builder.authority(a.substring(1, a.length() - 1));
            }
        }

        int lastColon = a.lastIndexOf(':');
        if (lastColon > 0) {
            String hostPart = a.substring(0, lastColon);
            String portPart = a.substring(lastColon + 1);
            if (portPart.matches("\\d{1,5}")
                    && !hostPart.isEmpty()
                    && !hostPart.contains(":")
                    && !hostPart.contains("]")) {
                int p = Integer.parseInt(portPart);
                if (p >= 0 && p <= 65535) {
                    return builder.encodedAuthority(hostPart + ":" + p);
                }
            }
        }

        return builder.authority(a);
    }

    /** Appends {@code user_id} when {@link DeviceInfo#getUserId()} is set. */
    public static void appendUserIdIfPresent(Uri.Builder builder, DeviceInfo deviceInfo) {
        if (builder == null || deviceInfo == null) {
            return;
        }
        String userId = deviceInfo.getUserId();
        if (userId != null && !userId.isEmpty()) {
            builder.appendQueryParameter("user_id", userId);
        }
    }
}
