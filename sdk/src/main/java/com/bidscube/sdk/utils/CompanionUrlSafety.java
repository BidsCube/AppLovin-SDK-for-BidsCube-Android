package com.bidscube.sdk.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.Locale;

/** URL scheme checks for VAST Companion click-through and WebView navigation. */
public final class CompanionUrlSafety {

    private CompanionUrlSafety() {
    }

    public static boolean isBlockedScheme(Uri uri) {
        if (uri == null) {
            return true;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return true;
        }
        String lower = scheme.toLowerCase(Locale.US);
        return "javascript".equals(lower)
                || "file".equals(lower)
                || "content".equals(lower)
                || "data".equals(lower);
    }

    public static boolean isHttpOrHttps(Uri uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        String lower = scheme.toLowerCase(Locale.US);
        return "http".equals(lower) || "https".equals(lower);
    }

    /**
     * Opens {@code uri} when allowed. HTTP/HTTPS always open externally. Other schemes open only
     * when Android resolves a handler.
     */
    public static boolean openExternal(Context context, Uri uri) {
        if (context == null || uri == null || isBlockedScheme(uri)) {
            return false;
        }
        if (isHttpOrHttps(uri)) {
            return launchViewIntent(context, uri);
        }
        Intent probe = new Intent(Intent.ACTION_VIEW, uri);
        probe.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (probe.resolveActivity(context.getPackageManager()) == null) {
            return false;
        }
        return launchViewIntent(context, uri);
    }

    private static boolean launchViewIntent(Context context, Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
