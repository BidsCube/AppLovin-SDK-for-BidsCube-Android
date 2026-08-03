package com.bidscube.sdk.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bidscube.sdk.network.TrackerPinger;
import com.bidscube.sdk.utils.SDKLogger;
import com.bidscube.sdk.utils.VastParser;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint({ "SetJavaScriptEnabled", "ViewConstructor" })
public final class WebViewVastVideoPlayer extends BidscubeVastVideoPlayer {

    private static final String TAG = "WebViewVastVideoPlayer";
    private static final String BRIDGE_NAME = "BidscubeVideoBridge";

    private final WebView webView;
    private final String initialVastMarkup;
    private final String initialClickThroughRedirectUrl;

    private OnVideoCompletionListener completionListener;
    private String vastMarkup;
    private String clickThroughRedirectUrl;
    private List<String> impressionUrls = Collections.emptyList();
    private List<String> startUrls = Collections.emptyList();
    private List<String> firstQuartileUrls = Collections.emptyList();
    private List<String> midpointUrls = Collections.emptyList();
    private List<String> thirdQuartileUrls = Collections.emptyList();
    private List<String> completeUrls = Collections.emptyList();
    private List<String> clickTrackingUrls = Collections.emptyList();
    private List<String> skipUrls = Collections.emptyList();
    private List<String> closeLinearUrls = Collections.emptyList();
    private List<String> errorUrls = Collections.emptyList();
    private String mediaFileUrl;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean skipped = new AtomicBoolean(false);
    private final AtomicBoolean firstQuartileTracked = new AtomicBoolean(false);
    private final AtomicBoolean midpointTracked = new AtomicBoolean(false);
    private final AtomicBoolean thirdQuartileTracked = new AtomicBoolean(false);

    public WebViewVastVideoPlayer(Context context, String vastMarkup, String clickThroughRedirectUrl) {
        super(context);
        this.initialVastMarkup = vastMarkup;
        this.initialClickThroughRedirectUrl = clickThroughRedirectUrl;
        webView = createConfiguredWebView(context);
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public WebViewVastVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialVastMarkup = null;
        this.initialClickThroughRedirectUrl = null;
        webView = createConfiguredWebView(context);
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private WebView createConfiguredWebView(Context context) {
        WebView view = new WebView(context);
        view.setBackgroundColor(android.graphics.Color.BLACK);
        view.setWebViewClient(new WebViewClient());
        view.setWebChromeClient(new WebChromeClient());
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        view.addJavascriptInterface(new JsBridge(), BRIDGE_NAME);
        return view;
    }

    @Override
    public void playVast(String vastTag, boolean isUrl) {
        vastMarkup = !TextUtils.isEmpty(vastTag) ? vastTag : initialVastMarkup;
        clickThroughRedirectUrl = !TextUtils.isEmpty(initialClickThroughRedirectUrl)
                ? initialClickThroughRedirectUrl
                : VastParser.getClickThroughUrl(vastMarkup);

        if (TextUtils.isEmpty(vastMarkup)) {
            handleError("Missing VAST markup");
            return;
        }

        if (isUrl) {
            handleError("Remote VAST URLs are not supported by WebViewVastVideoPlayer");
            return;
        }

        mediaFileUrl = VastParser.getMediaFileUrl(vastMarkup);
        if (TextUtils.isEmpty(mediaFileUrl)) {
            handleError("VAST does not contain a MediaFile URL");
            return;
        }

        impressionUrls = VastParser.getImpressionUrls(vastMarkup);
        startUrls = VastParser.getTrackingUrls(vastMarkup, "start");
        firstQuartileUrls = VastParser.getTrackingUrls(vastMarkup, "firstQuartile");
        midpointUrls = VastParser.getTrackingUrls(vastMarkup, "midpoint");
        thirdQuartileUrls = VastParser.getTrackingUrls(vastMarkup, "thirdQuartile");
        completeUrls = VastParser.getTrackingUrls(vastMarkup, "complete");
        clickTrackingUrls = VastParser.getClickTrackingUrls(vastMarkup);
        skipUrls = VastParser.getTrackingUrls(vastMarkup, "skip");
        closeLinearUrls = VastParser.getTrackingUrls(vastMarkup, "closeLinear");
        errorUrls = VastParser.getErrorUrls(vastMarkup);

        resetPlaybackState();
        webView.loadDataWithBaseURL(mediaFileUrl, buildHtml(mediaFileUrl), "text/html", "utf-8", null);
    }

    @Override
    public void setOnVideoCompletionListener(OnVideoCompletionListener listener) {
        completionListener = listener;
    }

    @Override
    public void release() {
        try {
            webView.stopLoading();
            webView.loadUrl("about:blank");
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void skipVideo() {
        try {
            webView.evaluateJavascript(
                    "(function(){var v=document.getElementById('bidscube-video');if(v){v.pause();}})();",
                    null);
        } catch (Throwable ignored) {
        }
        markSkipped();
    }

    private void resetPlaybackState() {
        started.set(false);
        completed.set(false);
        skipped.set(false);
        firstQuartileTracked.set(false);
        midpointTracked.set(false);
        thirdQuartileTracked.set(false);
    }

    private String buildHtml(String videoUrl) {
        String jsVideoUrl = escapeForJs(videoUrl);
        return "<!DOCTYPE html><html><head><meta charset='utf-8' />"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0' />"
                + "<style>html,body{margin:0;padding:0;background:#000;width:100%;height:100%;overflow:hidden;}"
                + "#bidscube-video{width:100%;height:100%;object-fit:contain;background:#000;}</style>"
                + "</head><body>"
                + "<video id='bidscube-video' playsinline controls autoplay preload='auto'></video>"
                + "<script>"
                + "var video=document.getElementById('bidscube-video');"
                + "video.src='" + jsVideoUrl + "';"
                + "video.addEventListener('play', function(){ " + BRIDGE_NAME + ".onStarted(); });"
                + "video.addEventListener('ended', function(){ " + BRIDGE_NAME + ".onCompleted(); });"
                + "video.addEventListener('error', function(){ "
                + BRIDGE_NAME + ".onError(video.error ? String(video.error.code) : 'unknown'); });"
                + "video.addEventListener('click', function(){ " + BRIDGE_NAME + ".onClicked(); });"
                + "video.addEventListener('timeupdate', function(){ "
                + BRIDGE_NAME + ".onProgress(video.currentTime || 0, video.duration || 0); });"
                + "video.play().catch(function(err){ " + BRIDGE_NAME
                + ".onError(err && err.message ? err.message : 'play failed'); });"
                + "</script></body></html>";
    }

    private static String escapeForJs(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "")
                .replace("\r", "");
    }

    private void handleStarted() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        TrackerPinger.pingUrls("vast.impression", impressionUrls);
        TrackerPinger.pingUrls("vast.start", startUrls);
    }

    private void handleProgress(double currentSeconds, double durationSeconds) {
        if (!started.get() || durationSeconds <= 0d) {
            return;
        }
        double progress = currentSeconds / durationSeconds;
        if (progress >= 0.25d && firstQuartileTracked.compareAndSet(false, true)) {
            TrackerPinger.pingUrls("vast.firstQuartile", firstQuartileUrls);
        }
        if (progress >= 0.50d && midpointTracked.compareAndSet(false, true)) {
            TrackerPinger.pingUrls("vast.midpoint", midpointUrls);
        }
        if (progress >= 0.75d && thirdQuartileTracked.compareAndSet(false, true)) {
            TrackerPinger.pingUrls("vast.thirdQuartile", thirdQuartileUrls);
        }
    }

    private void handleCompleted() {
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        TrackerPinger.pingUrls("vast.complete", completeUrls);
        if (completionListener != null) {
            completionListener.onVideoCompleted();
            completionListener.onAdSessionCompleted();
        }
    }

    private void markSkipped() {
        if (completed.get() || !skipped.compareAndSet(false, true)) {
            return;
        }
        TrackerPinger.pingUrls("vast.skip", skipUrls);
        TrackerPinger.pingUrls("vast.closeLinear", closeLinearUrls);
        if (completionListener != null) {
            completionListener.onVideoSkipped();
        }
    }

    private void handleClicked() {
        TrackerPinger.pingUrls("vast.clickTracking", clickTrackingUrls);
        if (TextUtils.isEmpty(clickThroughRedirectUrl)) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughRedirectUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        } catch (Exception e) {
            SDKLogger.w(TAG, "Failed to open click-through URL: " + e.getMessage());
        }
    }

    private void handleError(String message) {
        SDKLogger.e(TAG, "WebView video error: " + message);
        TrackerPinger.pingUrls("vast.error", errorUrls);
        markSkipped();
    }

    private final class JsBridge {
        @JavascriptInterface
        public void onStarted() {
            post(WebViewVastVideoPlayer.this::handleStarted);
        }

        @JavascriptInterface
        public void onCompleted() {
            post(WebViewVastVideoPlayer.this::handleCompleted);
        }

        @JavascriptInterface
        public void onSkipped() {
            post(WebViewVastVideoPlayer.this::markSkipped);
        }

        @JavascriptInterface
        public void onError(String message) {
            post(() -> handleError(message));
        }

        @JavascriptInterface
        public void onClicked() {
            post(WebViewVastVideoPlayer.this::handleClicked);
        }

        @JavascriptInterface
        public void onProgress(double current, double duration) {
            post(() -> handleProgress(current, duration));
        }
    }
}
