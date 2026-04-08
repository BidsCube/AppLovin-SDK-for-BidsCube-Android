package com.bidscube.sdk.stats;

import android.net.Uri;

import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.models.DeviceInfo;
import com.bidscube.sdk.network.SspAdUriHelper;
import com.bidscube.sdk.network.TrackerPinger;
import com.bidscube.sdk.utils.SDKLogger;

/**
 * Optional HTTPS GET beacons to a host chosen by the integrator. When {@link SDKConfig#getStatsRequestAuthority()}
 * is unset, this class performs no network I/O.
 */
public final class SdkStatsReporter {

    private static final String TAG = "SdkStatsReporter";
    /** First path segment on the stats host, e.g. {@code https://stats.example.com/collect?…}. */
    private static final String PATH_COLLECT = "collect";

    private SdkStatsReporter() {
    }

    /**
     * Fires after SDK async init completes (DeviceInfo ready). No-op without stats authority.
     */
    public static void reportSdkInit(SDKConfig config, DeviceInfo deviceInfo) {
        if (config == null || deviceInfo == null) {
            return;
        }
        report(config, "sdk_init",
                "bundle", safe(deviceInfo.getBundle()),
                "av", safe(deviceInfo.getAppVersion()),
                "ad_host", safe(deviceInfo.getAdRequestAuthority()));
    }

    /**
     * Fires on ad pipeline failures (network, parse, empty creative). No-op without stats authority.
     */
    public static void reportAdFailure(SDKConfig config, String placementId, String format, String detail) {
        if (config == null) {
            return;
        }
        report(config, "ad_fail",
                "pid", safe(placementId),
                "fmt", safe(format),
                "detail", truncate(safe(detail), 400));
    }

    private static void report(SDKConfig config, String event, String... queryPairs) {
        String auth = config.getStatsRequestAuthority();
        if (auth == null || auth.trim().isEmpty()) {
            return;
        }
        try {
            Uri.Builder b = SspAdUriHelper.newHttpsPathUriBuilder(auth.trim(), PATH_COLLECT);
            b.appendQueryParameter("e", event);
            for (int i = 0; i + 1 < queryPairs.length; i += 2) {
                String k = queryPairs[i];
                String v = queryPairs[i + 1];
                if (k != null && v != null) {
                    b.appendQueryParameter(k, v);
                }
            }
            String url = b.build().toString();
            SDKLogger.d(TAG, "optional stats: " + url);
            TrackerPinger.pingUrl("sdk.stats", url);
        } catch (Exception e) {
            SDKLogger.w(TAG, "optional stats skipped: " + e.getMessage());
        }
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
