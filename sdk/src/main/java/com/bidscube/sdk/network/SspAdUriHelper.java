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
}
