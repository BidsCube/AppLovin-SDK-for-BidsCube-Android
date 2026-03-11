package com.bidscube.sdk.network;

import android.text.TextUtils;

import com.bidscube.sdk.utils.SDKLogger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fire-and-forget HTTP pinger for tracking endpoints.
 */
public final class TrackerPinger {

    private static final String TAG = "TrackerPinger";
    private static final int CONNECT_TIMEOUT_MS = 2500;
    private static final int READ_TIMEOUT_MS = 2500;
    private static final ExecutorService TRACKER_EXECUTOR = Executors.newCachedThreadPool();

    private TrackerPinger() {
    }

    public static int pingUrls(List<String> urls) {
        return pingUrls("tracker", urls);
    }

    public static int pingUrls(String source, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            SDKLogger.d(TAG, "No trackers to fire for source: " + source);
            return 0;
        }

        int fired = 0;
        for (String url : urls) {
            if (pingUrl(source, url)) {
                fired++;
            }
        }
        SDKLogger.d(TAG, "Queued " + fired + "/" + urls.size() + " trackers for source: " + source);
        return fired;
    }

    public static boolean pingUrl(String url) {
        return pingUrl("tracker", url);
    }

    public static boolean pingUrl(String source, String url) {
        if (TextUtils.isEmpty(url)) {
            SDKLogger.w(TAG, "Skipping empty tracker URL for source: " + source);
            return false;
        }

        final String trackerUrl = url.trim();
        if (trackerUrl.isEmpty()) {
            SDKLogger.w(TAG, "Skipping blank tracker URL for source: " + source);
            return false;
        }

        SDKLogger.d(TAG, "Queue tracker [" + source + "]: " + trackerUrl);

        TRACKER_EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            InputStream stream = null;
            try {
                connection = (HttpURLConnection) new URL(trackerUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setInstanceFollowRedirects(true);

                int responseCode = connection.getResponseCode();
                if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    stream = connection.getErrorStream();
                } else {
                    stream = connection.getInputStream();
                }

                if (stream != null) {
                    stream.close();
                }

                SDKLogger.d(TAG, "Tracker ping finished [" + source + "] code=" + responseCode + " url=" + trackerUrl);
            } catch (Exception e) {
                // Tracker failures must never break ad rendering.
                SDKLogger.w(TAG, "Tracker ping failed [" + source + "] url=" + trackerUrl + " error=" + e.getMessage());
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (Exception ignored) {
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });

        return true;
    }
}
