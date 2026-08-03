package com.bidscube.sdk.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;

import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.models.CompanionAd;
import com.bidscube.sdk.utils.CompanionClickHandler;
import com.bidscube.sdk.utils.VastParser;

/**
 * Post-video HTML / IFrame VAST Companion rendered in a locked-down WebView.
 */
public final class VideoHtmlCompanionOverlay {

    public interface OnClosedListener {
        void onClosed();
    }

    private static final String INLINE_HTML_BASE_URL = "https://bidscube.local/companion/";

    private final FrameLayout rootView;
    private final WebView webView;
    private final CompanionClickHandler clickHandler;

    @SuppressLint("SetJavaScriptEnabled")
    public VideoHtmlCompanionOverlay(Context context, String vastXml, String placementId, AdCallback callback,
            OnClosedListener closedListener) {
        this(context, VastParser.selectPostVideoCompanion(vastXml), placementId, callback, closedListener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public VideoHtmlCompanionOverlay(Context context, CompanionAd companionAd, String placementId, AdCallback callback,
            OnClosedListener closedListener) {
        rootView = new FrameLayout(context);
        rootView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        rootView.setBackgroundColor(0xFF121212);

        clickHandler = new CompanionClickHandler(companionAd);

        webView = new WebView(context);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.BLACK);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) {
                    return true;
                }
                return clickHandler.handleExternalNavigation(context, request.getUrl(), placementId, callback);
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (TextUtils.isEmpty(url)) {
                    return true;
                }
                return clickHandler.handleExternalNavigation(context, Uri.parse(url), placementId, callback);
            }
        });
        rootView.addView(webView);

        if (companionAd != null) {
            if (companionAd.getResourceType() == CompanionAd.ResourceType.HTML) {
                webView.loadDataWithBaseURL(
                        INLINE_HTML_BASE_URL,
                        companionAd.getResource(),
                        "text/html",
                        "UTF-8",
                        null);
            } else if (companionAd.getResourceType() == CompanionAd.ResourceType.IFRAME) {
                webView.loadUrl(companionAd.getResource());
            }
        }

        rootView.addView(buildCloseButton(context, closedListener));
    }

    public void attach(FrameLayout parent) {
        parent.addView(rootView);
        clickHandler.fireCreativeViewOnce();
    }

    public void destroy() {
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.loadUrl("about:blank");
                webView.destroy();
            } catch (Throwable ignored) {
            }
        }
        try {
            if (rootView.getParent() instanceof FrameLayout) {
                ((FrameLayout) rootView.getParent()).removeView(rootView);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Button buildCloseButton(Context context, OnClosedListener closedListener) {
        Button closeButton = new Button(context);
        closeButton.setText("✕");
        closeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        closeButton.setTextColor(Color.WHITE);
        closeButton.setAllCaps(false);
        VideoEndCardOverlay.styleChipPublic(context, closeButton);
        closeButton.setMinWidth(VideoEndCardOverlay.dpPublic(context, 36));
        closeButton.setMinHeight(VideoEndCardOverlay.dpPublic(context, 36));
        closeButton.setPadding(
                VideoEndCardOverlay.dpPublic(context, 10),
                VideoEndCardOverlay.dpPublic(context, 4),
                VideoEndCardOverlay.dpPublic(context, 10),
                VideoEndCardOverlay.dpPublic(context, 4));
        closeButton.setOnClickListener(v -> {
            if (closedListener != null) {
                closedListener.onClosed();
            }
        });
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        closeLp.gravity = Gravity.TOP | Gravity.END;
        int margin = VideoEndCardOverlay.dpPublic(context, 16);
        closeLp.setMargins(0, VideoEndCardOverlay.dpPublic(context, 20), margin, 0);
        closeButton.setLayoutParams(closeLp);
        return closeButton;
    }
}
