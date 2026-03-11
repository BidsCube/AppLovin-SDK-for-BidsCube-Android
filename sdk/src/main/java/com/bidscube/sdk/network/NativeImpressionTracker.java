package com.bidscube.sdk.network;

import com.bidscube.sdk.models.natives.EventTracker;
import com.bidscube.sdk.models.natives.NativeAd;
import com.bidscube.sdk.utils.SDKLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Fires Native impression trackers exactly once per NativeAd instance.
 */
public final class NativeImpressionTracker {

    private static final String TAG = "NativeImpressionTracker";
    private static final Set<NativeAd> FIRED_ADS =
            Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));

    private NativeImpressionTracker() {
    }

    public static boolean fireIfNeeded(NativeAd nativeAd, String source) {
        if (nativeAd == null) {
            SDKLogger.d(TAG, "Skip impression fire: nativeAd is null, source=" + source);
            return false;
        }

        synchronized (FIRED_ADS) {
            if (FIRED_ADS.contains(nativeAd)) {
                SDKLogger.d(TAG, "Native impression already fired, source=" + source);
                return false;
            }
            FIRED_ADS.add(nativeAd);
        }

        int imptrackerTotal = nativeAd.imptrackers != null ? nativeAd.imptrackers.size() : 0;
        List<String> eventTrackerUrls = getImpressionEventTrackerUrls(nativeAd.eventtrackers);
        int eventTrackerTotal = eventTrackerUrls.size();

        SDKLogger.d(TAG, "Preparing native impression fire (" + source + "): imptrackers=" + imptrackerTotal
                + ", impressionEventTrackers=" + eventTrackerTotal);

        int firedImptrackers = TrackerPinger.pingUrls("native.imptrackers", nativeAd.imptrackers);
        int firedEventTrackers = TrackerPinger.pingUrls("native.eventtrackers.impression.img", eventTrackerUrls);
        int totalFired = firedImptrackers + firedEventTrackers;

        SDKLogger.d(TAG, "Native impression fired (" + source + "): " + totalFired + " trackers "
                + "(imptrackers=" + firedImptrackers + ", eventtrackers=" + firedEventTrackers + ")");
        return true;
    }

    private static List<String> getImpressionEventTrackerUrls(List<EventTracker> eventTrackers) {
        List<String> urls = new ArrayList<>();
        if (eventTrackers == null || eventTrackers.isEmpty()) {
            return urls;
        }

        for (EventTracker eventTracker : eventTrackers) {
            if (eventTracker == null) {
                continue;
            }

            // OpenRTB Native: event=1 is impression, method=1 is image tracker (GET).
            if (eventTracker.getEvent() == 1 && eventTracker.getMethod() == 1 && eventTracker.getUrl() != null) {
                urls.addAll(eventTracker.getUrl());
            } else if (eventTracker.getEvent() == 1 && eventTracker.getMethod() == 2) {
                SDKLogger.d(TAG, "Skipping JS impression eventtracker (method=2): no WebView context");
            }
        }

        return urls;
    }
}
