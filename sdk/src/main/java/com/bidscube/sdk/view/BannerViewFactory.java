package com.bidscube.sdk.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bidscube.sdk.utils.CompanionUrlSafety;
import com.bidscube.sdk.utils.SDKLogger;

/**
 * Creates WebView instances for HTML banner ADM.
 */
public class BannerViewFactory {

    public interface PageLifecycleListener {
        void onPageFinished(WebView view);
    }

    private static int standardBannerMinHeightPx(Context context) {
        return (int) (50f * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static WebView createBanner(Context context, String adHtml) {
        return createBanner(context, adHtml, null, null, null);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static WebView createBanner(Context context, String adHtml, int heightDp) {
        return createBanner(context, adHtml, heightDp, null, null);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static WebView createBanner(Context context, String adHtml, PageLifecycleListener listener) {
        return createBanner(context, adHtml, null, null, listener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static WebView createBanner(Context context, String adHtml, Integer heightDp,
            BannerAdTrace trace, PageLifecycleListener listener) {
        WebView webView = new WebView(context);
        int minH = standardBannerMinHeightPx(context);
        int layoutH = heightDp != null
                ? Math.max((int) (heightDp * context.getResources().getDisplayMetrics().density + 0.5f), minH)
                : minH;
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, layoutH));

        configureSettings(webView);
        webView.setBackgroundColor(Color.TRANSPARENT);
        try {
            webView.setMinimumHeight(minH);
        } catch (Throwable ignored) {
        }
        attachTouchHandler(webView);
        attachClients(webView, trace, listener);

        webView.post(() -> {
            String finalHtml = BannerHtmlPreparer.prepareFinalHtml(adHtml);
            if (trace != null) {
                trace.admStage("sanitized", BannerHtmlPreparer.prepareSanitizedContent(adHtml));
                trace.finalHtml(finalHtml);
            }
            SDKLogger.d("BannerViewFactory", "createBanner admLen="
                    + (adHtml != null ? adHtml.length() : 0)
                    + " finalHtmlLen=" + finalHtml.length()
                    + " imgs=" + BannerHtmlPreparer.countImgTags(finalHtml));
            webView.loadDataWithBaseURL("https://adcontent.local/", finalHtml, "text/html", "utf-8", null);
        });
        return webView;
    }

    private static void configureSettings(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setBlockNetworkImage(false);
            settings.setLoadsImagesAutomatically(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
        } catch (Throwable ignored) {
        }
        try {
            webView.setVerticalScrollBarEnabled(false);
            webView.setHorizontalScrollBarEnabled(false);
        } catch (Throwable ignored) {
        }
    }

    private static void attachTouchHandler(WebView webView) {
        webView.setOnTouchListener((v, event) -> {
            try {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    android.view.ViewParent p = v.getParent();
                    while (p != null) {
                        if (p instanceof android.widget.ScrollView) {
                            ((android.widget.ScrollView) p).requestDisallowInterceptTouchEvent(true);
                            break;
                        }
                        p = p.getParent();
                    }
                    return false;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
            return false;
        });
    }

    private static void attachClients(WebView webView, BannerAdTrace trace, PageLifecycleListener listener) {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) {
                    return false;
                }
                CompanionUrlSafety.openExternal(view.getContext(), request.getUrl());
                return true;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null || url.isEmpty()) {
                    return false;
                }
                CompanionUrlSafety.openExternal(view.getContext(), Uri.parse(url));
                return true;
            }

            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view,
                    WebResourceRequest request) {
                if (trace != null && request != null && request.getUrl() != null) {
                    trace.networkRequest(request.getUrl().toString(), request.isForMainFrame());
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                SDKLogger.d("BannerViewFactory", "onPageFinished w=" + view.getMeasuredWidth()
                        + " h=" + view.getMeasuredHeight());
                if (trace != null) {
                    trace.layout(view);
                }
                adjustHeightToContent(view);
                if (listener != null) {
                    listener.onPageFinished(view);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                String msg = "code=" + errorCode + " url=" + failingUrl;
                SDKLogger.e("BannerViewFactory", "onReceivedError " + msg);
                if (trace != null) {
                    trace.webViewError("onReceivedError", msg);
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                    android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                String msg = "url=" + (request.getUrl()) + " status="
                        + (errorResponse != null ? errorResponse.getStatusCode() : -1);
                SDKLogger.e("BannerViewFactory", "onReceivedHttpError " + msg);
                if (trace != null) {
                    trace.webViewError("onReceivedHttpError", msg);
                }
            }
        });

        try {
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                    SDKLogger.d("BannerViewFactory", "console: " + consoleMessage.message());
                    return super.onConsoleMessage(consoleMessage);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static void adjustHeightToContent(WebView view) {
        view.postDelayed(() -> {
            try {
                view.evaluateJavascript(
                        "(function(){return Math.max(document.body.scrollHeight,document.documentElement.scrollHeight);})();",
                        value -> {
                            try {
                                if (value == null) {
                                    return;
                                }
                                String s = value.replaceAll("\"", "").trim();
                                if (s.isEmpty()) {
                                    return;
                                }
                                float cssPx = Float.parseFloat(s);
                                int minBanner = standardBannerMinHeightPx(view.getContext());
                                int heightPx = Math.max(minBanner,
                                        Math.round(cssPx * view.getResources().getDisplayMetrics().density));
                                ViewGroup.LayoutParams params = view.getLayoutParams();
                                if (params == null) {
                                    params = new ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
                                } else {
                                    params.height = heightPx;
                                }
                                view.setLayoutParams(params);
                                view.requestLayout();
                            } catch (Exception ignored) {
                            }
                        });
            } catch (Throwable ignored) {
            }
        }, 300);
    }
}
