package com.bidscube.sdk.view;

import android.view.View;
import android.webkit.WebView;

import com.bidscube.sdk.utils.SDKLogger;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Correlates one banner show across request → ADM → WebView (debug-friendly, minimal fields).
 */
public final class BannerAdTrace {

    private static final String TAG = "BannerAdTrace";
    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final Pattern IMP_H = Pattern.compile("(?i)[?&]h=([^&\"'\\s<>]+)");

    private static final Pattern IMP_URL = Pattern.compile("(?i)[?&]m=i(?:&|$)");

    private final int traceId;
    private final String placementId;
    private String impTokenHint = "";
    private final Set<String> impNetworkRequests = ConcurrentHashMap.newKeySet();

    private BannerAdTrace(String placementId) {
        this.traceId = SEQ.incrementAndGet();
        this.placementId = placementId != null ? placementId : "";
    }

    public static BannerAdTrace start(String placementId, String requestUrl) {
        BannerAdTrace t = new BannerAdTrace(placementId);
        SDKLogger.d(TAG, t.prefix("start") + " reqHost=" + hostOnly(requestUrl)
                + " format=c=b");
        return t;
    }

    public void rawResponse(int code, String body) {
        SDKLogger.d(TAG, prefix("raw") + " http=" + code + " bodyLen="
                + (body != null ? body.length() : 0));
    }

    public void admStage(String stage, String adm) {
        impTokenHint = extractImpToken(adm);
        SDKLogger.d(TAG, prefix(stage) + " admLen=" + (adm != null ? adm.length() : 0)
                + " imgs=" + BannerHtmlPreparer.countImgTags(adm)
                + " hasImpPixel=" + containsImpUrl(adm)
                + " impH=" + maskToken(impTokenHint));
    }

    public void finalHtml(String html) {
        SDKLogger.d(TAG, prefix("finalHtml") + " len=" + (html != null ? html.length() : 0)
                + " imgs=" + BannerHtmlPreparer.countImgTags(html)
                + " hasImpPixel=" + containsImpUrl(html)
                + " impH=" + maskToken(impTokenHint));
    }

    public void callbackOrder(String name) {
        SDKLogger.d(TAG, prefix("cb") + " " + name);
    }

    public void webViewError(String kind, String detail) {
        SDKLogger.e(TAG, prefix("webview") + " " + kind + " " + detail);
    }

    public void layout(WebView view) {
        if (view == null) {
            return;
        }
        SDKLogger.d(TAG, prefix("layout") + " w=" + view.getWidth() + " h=" + view.getHeight()
                + " attached=" + view.isAttachedToWindow()
                + " viewVisibility=" + (view.getVisibility() == View.VISIBLE)
                + " (container metrics only — not viewability)");
    }

    /**
     * Logs subresource loads from WebView. For {@code m=i} URLs tracks first vs duplicate requests.
     */
    public void networkRequest(String url, boolean mainFrame) {
        if (url == null || url.isEmpty()) {
            return;
        }
        if (!isImpNetworkUrl(url)) {
            return;
        }
        String key = normalizeImpUrl(url);
        boolean first = impNetworkRequests.add(key);
        SDKLogger.d(TAG, prefix("network") + " impRequest first=" + first
                + " mainFrame=" + mainFrame
                + " impH=" + maskToken(extractImpToken(url))
                + " urlHost=" + hostOnly(url));
        if (!first) {
            SDKLogger.w(TAG, prefix("network") + " DUPLICATE imp request for impH="
                    + maskToken(extractImpToken(url)));
        }
    }

    public int impRequestCount() {
        return impNetworkRequests.size();
    }

    private static boolean isImpNetworkUrl(String url) {
        return IMP_URL.matcher(url).find();
    }

    private static String normalizeImpUrl(String url) {
        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            android.net.Uri.Builder b = uri.buildUpon().clearQuery();
            for (String name : uri.getQueryParameterNames()) {
                if ("h".equalsIgnoreCase(name)) {
                    String h = uri.getQueryParameter(name);
                    if (h != null) {
                        b.appendQueryParameter("h", h);
                    }
                } else if ("m".equalsIgnoreCase(name)) {
                    b.appendQueryParameter("m", uri.getQueryParameter(name));
                } else if ("c".equalsIgnoreCase(name)) {
                    b.appendQueryParameter("c", uri.getQueryParameter(name));
                }
            }
            return b.build().toString();
        } catch (Exception e) {
            return url;
        }
    }

    private String prefix(String phase) {
        return "trace=" + traceId + " placement=" + placementId + " phase=" + phase;
    }

    private static boolean containsImpUrl(String html) {
        return html != null && html.contains("m=i");
    }

    private static String extractImpToken(String html) {
        if (html == null) {
            return "";
        }
        Matcher m = IMP_H.matcher(html);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "-";
        }
        if (token.length() <= 6) {
            return "***";
        }
        return token.substring(0, 3) + "…" + token.substring(token.length() - 2);
    }

    private static String hostOnly(String url) {
        if (url == null) {
            return "";
        }
        try {
            return android.net.Uri.parse(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }
}
