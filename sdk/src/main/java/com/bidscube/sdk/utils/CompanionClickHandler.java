package com.bidscube.sdk.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.models.CompanionAd;
import com.bidscube.sdk.network.TrackerPinger;

import java.util.concurrent.atomic.AtomicBoolean;

/** Companion click tracking and click-through, scoped to a single {@link CompanionAd}. */
public final class CompanionClickHandler {

    private final AtomicBoolean clickFired = new AtomicBoolean(false);
    private final AtomicBoolean creativeViewFired = new AtomicBoolean(false);

    private final CompanionAd companionAd;

    public CompanionClickHandler(CompanionAd companionAd) {
        this.companionAd = companionAd;
    }

    public void fireCreativeViewOnce() {
        if (companionAd == null || !creativeViewFired.compareAndSet(false, true)) {
            return;
        }
        TrackerPinger.pingUrls("vast.companion.creativeView", companionAd.getCreativeViewTrackingUrls());
    }

    public boolean handleClick(Context context, String placementId, AdCallback callback) {
        if (companionAd == null) {
            return false;
        }
        TrackerPinger.pingUrls("vast.companion.clickTracking", companionAd.getClickTrackingUrls());
        String clickThrough = companionAd.getClickThroughUrl();
        if (!TextUtils.isEmpty(clickThrough)) {
            CompanionUrlSafety.openExternal(context, Uri.parse(clickThrough));
        }
        if (callback != null && placementId != null && clickFired.compareAndSet(false, true)) {
            try {
                callback.onAdClicked(placementId);
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    public boolean handleExternalNavigation(Context context, Uri uri, String placementId, AdCallback callback) {
        if (uri == null || CompanionUrlSafety.isBlockedScheme(uri)) {
            return true;
        }
        if (!CompanionUrlSafety.openExternal(context, uri)) {
            return true;
        }
        TrackerPinger.pingUrls("vast.companion.clickTracking", companionAd.getClickTrackingUrls());
        if (callback != null && placementId != null && clickFired.compareAndSet(false, true)) {
            try {
                callback.onAdClicked(placementId);
            } catch (Throwable ignored) {
            }
        }
        return true;
    }
}
