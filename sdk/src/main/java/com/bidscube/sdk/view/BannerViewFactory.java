package com.bidscube.sdk.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.view.ViewGroup;
import android.view.MotionEvent;
import com.bidscube.sdk.utils.SDKLogger;

public class BannerViewFactory {

    @SuppressLint("SetJavaScriptEnabled")
    public static WebView createBanner(Context context, String adHtml) {
        WebView webView = new WebView(context);

        // Default the WebView to match parent width but wrap content height so
        // it can expand to the creative height when hosted inside a WRAP_CONTENT container
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        // Allow mixed content (http resources on https pages) and file/Universal access
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            // Allow JS from file URLs (useful when baseUrl is file-based)
            settings.setAllowUniversalAccessFromFileURLs(true);
        } catch (Throwable ignored) {}

        webView.setBackgroundColor(Color.TRANSPARENT);
        // Ensure webview has a sensible minimum height so it doesn't measure to zero before content loads.
        int minHeightDp = 250;
        int minHeightPx = (int) (minHeightDp * context.getResources().getDisplayMetrics().density + 0.5f);
        try { webView.setMinimumHeight(minHeightPx); } catch (Throwable ignored) {}
        try { webView.setVisibility(android.view.View.VISIBLE); } catch (Throwable ignored) {}

        // Disable scrollbars and prevent WebView fling/scroll gestures from moving the parent ScrollView
        try { webView.setVerticalScrollBarEnabled(false); webView.setHorizontalScrollBarEnabled(false); } catch (Throwable ignored) {}
        webView.setOnTouchListener((v, event) -> {
            try {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // prevent parent ScrollView from stealing touch events while interacting with the ad
                    v.getParent();
                    android.view.ViewParent p = v.getParent();
                    while (p != null) {
                        if (p instanceof android.widget.ScrollView) {
                            ((android.widget.ScrollView) p).requestDisallowInterceptTouchEvent(true);
                            break;
                        }
                        p = p.getParent();
                    }
                    return false; // allow click handling
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    // consume move events to keep the banner static
                    return true;
                }
            } catch (Throwable ignored) {}
            return false;
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                view.getContext().startActivity(intent);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                try {
                    SDKLogger.d("BannerViewFactory", "onPageFinished: view measuredWidth=" + view.getMeasuredWidth() + " measuredHeight=" + view.getMeasuredHeight());
                } catch (Throwable ignored) {}
                // JS: set child to fill width and keep auto height so images scale down to fit without cropping.
                String js = "(function(){try{var root=document.getElementById('ad-root'); if(!root) return; var child=root.firstElementChild||root;" +
                        "if(!child) return;" +
                        // set width to 100% and height to auto to preserve aspect ratio
                        "child.style.position='relative'; child.style.left='0'; child.style.top='0'; child.style.margin='0';" +
                        "child.style.width='100%'; child.style.height='auto'; child.style.maxWidth='100%'; child.style.display='block'; child.style.transform='none';" +
                        // clear inline negative margins/positions for descendants
                        "var els=child.querySelectorAll('[style]'); for(var i=0;i<els.length;i++){ try{ els[i].style.margin='0'; els[i].style.left='0'; els[i].style.top='0'; els[i].style.height='auto'; }catch(e){} }" +
                        // fallback: if child is still wider than container, scale it down proportionally
                        "var w=child.scrollWidth||child.offsetWidth||child.clientWidth; var cw=root.clientWidth||document.documentElement.clientWidth; if(w>0 && cw>0 && w>cw){ var scale=cw/w; child.style.transformOrigin='center top'; child.style.transform='scale('+scale+')'; }" +
                        "}catch(e){console.error(e);}})();";
                try {
                    view.evaluateJavascript(js, null);
                } catch (Throwable ignored) {
                }

                // Extra normalization for stubborn creatives: force widths to 100vw and retry after delays
                try {
                    String forceWidthJs = "(function(){try{var root=document.getElementById('ad-root'); if(root){ root.style.width='100vw'; root.style.maxWidth='100vw'; root.style.boxSizing='border-box'; } var child=(root&&root.firstElementChild)?root.firstElementChild:root; if(child){ child.style.width='100vw'; child.style.maxWidth='100vw'; child.style.boxSizing='border-box'; } document.documentElement.style.width='100vw'; document.body.style.width='100vw';}catch(e){console.error(e);}})();";
                    view.evaluateJavascript(forceWidthJs, null);
                    // re-run the height calculation after short delays to accommodate late image/script loads
                    view.postDelayed(() -> {
                        try {
                            view.evaluateJavascript("(function(){return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);})();", v -> {
                                try {
                                    if (v == null) return;
                                    String s = v.replaceAll("\"", "").trim();
                                    if (s.isEmpty()) return;
                                    float cssPx = Float.parseFloat(s);
                                    float density = view.getContext().getResources().getDisplayMetrics().density;
                                    int heightPx = Math.round(cssPx * density);
                                    SDKLogger.d("BannerViewFactory", "onPageFinished: computed content cssHeight=" + cssPx + " => heightPx=" + heightPx);
                                    if (heightPx < 1) return;
                                    ViewGroup.LayoutParams params = view.getLayoutParams();
                                    if (params == null)
                                        params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
                                    else params.height = heightPx;
                                    view.setLayoutParams(params);
                                    view.requestLayout();
                                } catch (Exception ignored) {
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }, 300);
                    view.postDelayed(() -> {
                        try {
                            view.evaluateJavascript("(function(){return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);})();", v -> {
                                try {
                                    if (v == null) return;
                                    String s = v.replaceAll("\"", "").trim();
                                    if (s.isEmpty()) return;
                                    float cssPx = Float.parseFloat(s);
                                    float density = view.getContext().getResources().getDisplayMetrics().density;
                                    int heightPx = Math.round(cssPx * density);
                                    if (heightPx < 1) return;
                                    ViewGroup.LayoutParams params = view.getLayoutParams();
                                    if (params == null)
                                        params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
                                    else params.height = heightPx;
                                    view.setLayoutParams(params);
                                    view.requestLayout();
                                } catch (Exception ignored) {
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }, 800);
                    // Additional cleanup: remove tiny/tracking images that may appear under the creative
                    try {
                        String removeTinyImgsJs = "(function(){try{var imgs=document.getElementsByTagName('img'); for(var i=imgs.length-1;i>=0;i--){var im=imgs[i]; try{var w=im.naturalWidth||im.width; var h=im.naturalHeight||im.height; if((w&&w<=2)||(h&&h<=2)){ im.parentNode&&im.parentNode.removeChild(im); } else { var s=(im.getAttribute('style')||'').toLowerCase(); if(s.indexOf('position: absolute')!==-1 && (s.indexOf('width:1px')!==-1||s.indexOf('height:1px')!==-1)){ im.parentNode&&im.parentNode.removeChild(im); } } }catch(e){} } }catch(e){} })();";
                        view.evaluateJavascript(removeTinyImgsJs, null);
                    } catch (Throwable ignored) {}
                 } catch (Throwable ignored) {
                 }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                SDKLogger.e("BannerViewFactory", "WebView onReceivedError: code=" + errorCode + " desc=" + description + " url=" + failingUrl);
            }

            @Override
            public void onReceivedHttpError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                SDKLogger.e("BannerViewFactory", "WebView onReceivedHttpError: url=" + (request != null && request.getUrl() != null ? request.getUrl().toString() : "") + " status=" + (errorResponse != null ? errorResponse.getStatusCode() : -1));
            }
        });

        // Add WebChromeClient for console logging to capture JS errors from creatives
        try {
            webView.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                    try {
                        String msg = consoleMessage.message();
                        String src = consoleMessage.sourceId();
                        int line = consoleMessage.lineNumber();
                        SDKLogger.d("BannerViewFactory", "WebView console: " + msg + " (" + src + ":" + line + ")");
                    } catch (Throwable ignored) {}
                    return super.onConsoleMessage(consoleMessage);
                }
            });
        } catch (Throwable ignored) {}

        // Ensure images and resources are not blocked
        try {
            settings.setBlockNetworkImage(false);
            settings.setLoadsImagesAutomatically(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
        } catch (Throwable ignored) {}

        webView.post(() -> {
             // Preprocess adHtml: if it contains a document.write(...) wrapper, try to extract inner HTML
             String content = adHtml != null ? adHtml : "";
            SDKLogger.d("BannerViewFactory", "createBanner called, admLen=" + (adHtml != null ? adHtml.length() : 0));

            String lower = content.toLowerCase();
            if (lower.contains("document.write") || lower.contains("document.writeln")) {
                int docIdx = lower.indexOf("document.write");
                if (docIdx == -1) docIdx = lower.indexOf("document.writeln");
                int openIdx = content.indexOf('(', docIdx);
                if (openIdx >= 0) {
                    int depth = 0;
                    int closeIdx = -1;
                    for (int i = openIdx; i < content.length(); i++) {
                        char c = content.charAt(i);
                        if (c == '(') depth++;
                        else if (c == ')') {
                            depth--;
                            if (depth == 0) {
                                closeIdx = i;
                                break;
                            }
                        }
                    }
                    if (closeIdx > openIdx) {
                        content = content.substring(openIdx + 1, closeIdx).trim();
                        // strip surrounding quotes/backticks if present
                        if (content.length() >= 2) {
                            char s = content.charAt(0);
                            char e = content.charAt(content.length() - 1);
                            if ((s == '\'' && e == '\'') || (s == '"' && e == '"') || (s == '`' && e == '`')) {
                                content = content.substring(1, content.length() - 1);
                            }
                        }
                        // simple unescape
                        content = content.replace("\\'", "'")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\/", "/");
                    }
                }
            }

            // Remove inline style attributes so our CSS and JS can normalize sizing and positioning.
            // This helps with creatives that embed fixed pixel widths/heights or negative margins.
            try {
                // remove style="..." and style='...'
                content = content.replaceAll("(?i)\\sstyle=\"[^\"]*\"", "");
                content = content.replaceAll("(?i)\\sstyle='[^']*'", "");
                // Remove obvious 1x1 tracking images and absolutely positioned 1px beacons
                content = content.replaceAll("(?i)<img[^>]*(?:width\\s*=\\s*['\"]?1['\"]?|height\\s*=\\s*['\"]?1['\"]?)[^>]*>", "");
                content = content.replaceAll("(?i)<img[^>]*style=['\"][^'\"]*(?:position\\s*:\\s*absolute|width\\s*:\\s*1px|height\\s*:\\s*1px)[^'\"]*['\"][^>]*>", "");
            } catch (Throwable ignored) {
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<html><head>");
            sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\">");
            sb.append("<style>");
            // Responsive wrapper: force no scrolling inside webview (we want static banner) and make width 100%
            sb.append("html,body,#ad-root{height:auto;min-height:0;width:100%;margin:0;padding:0;overflow:hidden;}");
            // Allow overflow visible so creatives that position elements slightly outside are still visible
            // while keeping box-sizing reset.
            sb.append("#ad-root{display:block;overflow:visible;box-sizing:border-box;position:relative;}");
            /* Ensure ad elements scale properly */
            sb.append("#ad-root, #ad-root * {"
                    + "box-sizing:border-box !important;"
                    + "max-width:100% !important;"
                    + "width:100% !important;"
                    + "margin:0 !important;"
                    + "padding:0 !important;"
                    + "}");

            /* Media */
            sb.append("img, iframe, video {"
                    + "width:100% !important;"
                    + "height:auto !important;"
                    + "object-fit:contain !important;"
                    + "display:block !important;"
                    + "max-width:100% !important;"
                    + "}");


            /* --- AdChoices / i-icon wrapper override --- */
            sb.append(".ad_choices_icon, .adchoices, .ad-choice-icon, .ad_choices, .ad_mark, .adchoice {"
                    + "position:absolute !important;"
                    + "top:8px !important;"
                    + "right:8px !important;"
                    + "z-index:99999 !important;"
                    + "width:auto !important;"
                    + "height:auto !important;"
                    + "opacity:1 !important;"
                    + "pointer-events:auto !important;"
                    + "}");


            /* Inline style overrides */
            sb.append("*[style] { max-width:100% !important; height:auto !important; }");

            sb.append("</style></head>");
            sb.append("<body><div id=\"ad-root\">");

            sb.append(content != null ? content : "");

            sb.append("</div></body></html>");

            String finalHtml = sb.toString();
            SDKLogger.d("BannerViewFactory", "Loading finalHtml length=" + finalHtml.length());

            // Use a non-null base URL so relative resources and external script tags are allowed to load.
            String base = "https://adcontent.local/";
            webView.loadDataWithBaseURL(base, finalHtml, "text/html", "utf-8", null);

        });

        return webView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static WebView createBanner(Context context, String adHtml, int heightDp) {
        WebView webView = new WebView(context);

        // Set the WebView height to the specified value in dp, with matching parent width
        int heightPx = (int) (heightDp * context.getResources().getDisplayMetrics().density + 0.5f);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        // Allow mixed content (http resources on https pages) and file/Universal access
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            // Allow JS from file URLs (useful when baseUrl is file-based)
            settings.setAllowUniversalAccessFromFileURLs(true);
        } catch (Throwable ignored) {}

        webView.setBackgroundColor(Color.TRANSPARENT);
        // Ensure webview has a sensible minimum height (250dp) matching the explicit height above.
        int minHeightDp = 250;
        int minHeightPx = (int) (minHeightDp * context.getResources().getDisplayMetrics().density + 0.5f);
        try { webView.setMinimumHeight(minHeightPx); } catch (Throwable ignored) {}
        try { webView.setVisibility(android.view.View.VISIBLE); } catch (Throwable ignored) {}

        // Disable scrollbars and prevent WebView fling/scroll gestures from moving the parent ScrollView
        try { webView.setVerticalScrollBarEnabled(false); webView.setHorizontalScrollBarEnabled(false); } catch (Throwable ignored) {}
        webView.setOnTouchListener((v, event) -> {
            try {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // prevent parent ScrollView from stealing touch events while interacting with the ad
                    v.getParent();
                    android.view.ViewParent p = v.getParent();
                    while (p != null) {
                        if (p instanceof android.widget.ScrollView) {
                            ((android.widget.ScrollView) p).requestDisallowInterceptTouchEvent(true);
                            break;
                        }
                        p = p.getParent();
                    }
                    return false; // allow click handling
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    // consume move events to keep the banner static
                    return true;
                }
            } catch (Throwable ignored) {}
            return false;
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                view.getContext().startActivity(intent);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                try {
                    SDKLogger.d("BannerViewFactory", "onPageFinished: view measuredWidth=" + view.getMeasuredWidth() + " measuredHeight=" + view.getMeasuredHeight());
                } catch (Throwable ignored) {}
                // JS: set child to fill width and keep auto height so images scale down to fit without cropping.
                String js = "(function(){try{var root=document.getElementById('ad-root'); if(!root) return; var child=root.firstElementChild||root;" +
                        "if(!child) return;" +
                        // set width to 100% and height to auto to preserve aspect ratio
                        "child.style.position='relative'; child.style.left='0'; child.style.top='0'; child.style.margin='0';" +
                        "child.style.width='100%'; child.style.height='auto'; child.style.maxWidth='100%'; child.style.display='block'; child.style.transform='none';" +
                        // clear inline negative margins/positions for descendants
                        "var els=child.querySelectorAll('[style]'); for(var i=0;i<els.length;i++){ try{ els[i].style.margin='0'; els[i].style.left='0'; els[i].style.top='0'; els[i].style.height='auto'; }catch(e){} }" +
                        // fallback: if child is still wider than container, scale it down proportionally
                        "var w=child.scrollWidth||child.offsetWidth||child.clientWidth; var cw=root.clientWidth||document.documentElement.clientWidth; if(w>0 && cw>0 && w>cw){ var scale=cw/w; child.style.transformOrigin='center top'; child.style.transform='scale('+scale+')'; }" +
                        "}catch(e){console.error(e);}})();";
                try {
                    view.evaluateJavascript(js, null);
                } catch (Throwable ignored) {
                }

                // Extra normalization for stubborn creatives: force widths to 100vw and retry after delays
                try {
                    String forceWidthJs = "(function(){try{var root=document.getElementById('ad-root'); if(root){ root.style.width='100vw'; root.style.maxWidth='100vw'; root.style.boxSizing='border-box'; } var child=(root&&root.firstElementChild)?root.firstElementChild:root; if(child){ child.style.width='100vw'; child.style.maxWidth='100vw'; child.style.boxSizing='border-box'; } document.documentElement.style.width='100vw'; document.body.style.width='100vw';}catch(e){console.error(e);}})();";
                    view.evaluateJavascript(forceWidthJs, null);
                    // re-run the height calculation after short delays to accommodate late image/script loads
                    view.postDelayed(() -> {
                        try {
                            view.evaluateJavascript("(function(){return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);})();", v -> {
                                try {
                                    if (v == null) return;
                                    String s = v.replaceAll("\"", "").trim();
                                    if (s.isEmpty()) return;
                                    float cssPx = Float.parseFloat(s);
                                    float density = view.getContext().getResources().getDisplayMetrics().density;
                                    int heightPx = Math.round(cssPx * density);
                                    SDKLogger.d("BannerViewFactory", "onPageFinished: computed content cssHeight=" + cssPx + " => heightPx=" + heightPx);
                                    if (heightPx < 1) return;
                                    ViewGroup.LayoutParams params = view.getLayoutParams();
                                    if (params == null)
                                        params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
                                    else params.height = heightPx;
                                    view.setLayoutParams(params);
                                    view.requestLayout();
                                } catch (Exception ignored) {
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }, 300);
                    view.postDelayed(() -> {
                        try {
                            view.evaluateJavascript("(function(){return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);})();", v -> {
                                try {
                                    if (v == null) return;
                                    String s = v.replaceAll("\"", "").trim();
                                    if (s.isEmpty()) return;
                                    float cssPx = Float.parseFloat(s);
                                    float density = view.getContext().getResources().getDisplayMetrics().density;
                                    int heightPx = Math.round(cssPx * density);
                                    if (heightPx < 1) return;
                                    ViewGroup.LayoutParams params = view.getLayoutParams();
                                    if (params == null)
                                        params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
                                    else params.height = heightPx;
                                    view.setLayoutParams(params);
                                    view.requestLayout();
                                } catch (Exception ignored) {
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }, 800);
                    // Additional cleanup: remove tiny/tracking images that may appear under the creative
                    try {
                        String removeTinyImgsJs = "(function(){try{var imgs=document.getElementsByTagName('img'); for(var i=imgs.length-1;i>=0;i--){var im=imgs[i]; try{var w=im.naturalWidth||im.width; var h=im.naturalHeight||im.height; if((w&&w<=2)||(h&&h<=2)){ im.parentNode&&im.parentNode.removeChild(im); } else { var s=(im.getAttribute('style')||'').toLowerCase(); if(s.indexOf('position: absolute')!==-1 && (s.indexOf('width:1px')!==-1||s.indexOf('height:1px')!==-1)){ im.parentNode&&im.parentNode.removeChild(im); } } }catch(e){} } }catch(e){} })();";
                        view.evaluateJavascript(removeTinyImgsJs, null);
                    } catch (Throwable ignored) {}
                 } catch (Throwable ignored) {
                 }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                SDKLogger.e("BannerViewFactory", "WebView onReceivedError: code=" + errorCode + " desc=" + description + " url=" + failingUrl);
            }

            @Override
            public void onReceivedHttpError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                SDKLogger.e("BannerViewFactory", "WebView onReceivedHttpError: url=" + (request != null && request.getUrl() != null ? request.getUrl().toString() : "") + " status=" + (errorResponse != null ? errorResponse.getStatusCode() : -1));
            }
        });

        // Add WebChromeClient for console logging to capture JS errors from creatives
        try {
            webView.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                    try {
                        String msg = consoleMessage.message();
                        String src = consoleMessage.sourceId();
                        int line = consoleMessage.lineNumber();
                        SDKLogger.d("BannerViewFactory", "WebView console: " + msg + " (" + src + ":" + line + ")");
                    } catch (Throwable ignored) {}
                    return super.onConsoleMessage(consoleMessage);
                }
            });
        } catch (Throwable ignored) {}

        // Ensure images and resources are not blocked
        try {
            settings.setBlockNetworkImage(false);
            settings.setLoadsImagesAutomatically(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
        } catch (Throwable ignored) {}

        webView.post(() -> {
             // Preprocess adHtml: if it contains a document.write(...) wrapper, try to extract inner HTML
             String content = adHtml != null ? adHtml : "";
            SDKLogger.d("BannerViewFactory", "createBanner called, admLen=" + (adHtml != null ? adHtml.length() : 0));

            String lower = content.toLowerCase();
            if (lower.contains("document.write") || lower.contains("document.writeln")) {
                int docIdx = lower.indexOf("document.write");
                if (docIdx == -1) docIdx = lower.indexOf("document.writeln");
                int openIdx = content.indexOf('(', docIdx);
                if (openIdx >= 0) {
                    int depth = 0;
                    int closeIdx = -1;
                    for (int i = openIdx; i < content.length(); i++) {
                        char c = content.charAt(i);
                        if (c == '(') depth++;
                        else if (c == ')') {
                            depth--;
                            if (depth == 0) {
                                closeIdx = i;
                                break;
                            }
                        }
                    }
                    if (closeIdx > openIdx) {
                        content = content.substring(openIdx + 1, closeIdx).trim();
                        // strip surrounding quotes/backticks if present
                        if (content.length() >= 2) {
                            char s = content.charAt(0);
                            char e = content.charAt(content.length() - 1);
                            if ((s == '\'' && e == '\'') || (s == '"' && e == '"') || (s == '`' && e == '`')) {
                                content = content.substring(1, content.length() - 1);
                            }
                        }
                        // simple unescape
                        content = content.replace("\\'", "'")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\/", "/");
                    }
                }
            }

            // Remove inline style attributes so our CSS and JS can normalize sizing and positioning.
            // This helps with creatives that embed fixed pixel widths/heights or negative margins.
            try {
                // remove style="..." and style='...'
                content = content.replaceAll("(?i)\\sstyle=\"[^\"]*\"", "");
                content = content.replaceAll("(?i)\\sstyle='[^']*'", "");
                // Remove obvious 1x1 tracking images and absolutely positioned 1px beacons
                content = content.replaceAll("(?i)<img[^>]*(?:width\\s*=\\s*['\"]?1['\"]?|height\\s*=\\s*['\"]?1['\"]?)[^>]*>", "");
                content = content.replaceAll("(?i)<img[^>]*style=['\"][^'\"]*(?:position\\s*:\\s*absolute|width\\s*:\\s*1px|height\\s*:\\s*1px)[^'\"]*['\"][^>]*>", "");
            } catch (Throwable ignored) {
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<html><head>");
            sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\">");
            sb.append("<style>");
            // Responsive wrapper: force no scrolling inside webview (we want static banner) and make width 100%
            sb.append("html,body,#ad-root{height:auto;min-height:0;width:100%;margin:0;padding:0;overflow:hidden;}");
            // Allow overflow visible so creatives that position elements slightly outside are still visible
            // while keeping box-sizing reset.
            sb.append("#ad-root{display:block;overflow:visible;box-sizing:border-box;position:relative;}");
            /* Ensure ad elements scale properly */
            sb.append("#ad-root, #ad-root * {"
                    + "box-sizing:border-box !important;"
                    + "max-width:100% !important;"
                    + "width:100% !important;"
                    + "margin:0 !important;"
                    + "padding:0 !important;"
                    + "}");

            /* Media */
            sb.append("img, iframe, video {"
                    + "width:100% !important;"
                    + "height:auto !important;"
                    + "object-fit:contain !important;"
                    + "display:block !important;"
                    + "max-width:100% !important;"
                    + "}");


            /* --- AdChoices / i-icon wrapper override --- */
            sb.append(".ad_choices_icon, .adchoices, .ad-choice-icon, .ad_choices, .ad_mark, .adchoice {"
                    + "position:absolute !important;"
                    + "top:8px !important;"
                    + "right:8px !important;"
                    + "z-index:99999 !important;"
                    + "width:auto !important;"
                    + "height:auto !important;"
                    + "opacity:1 !important;"
                    + "pointer-events:auto !important;"
                    + "}");


            /* Inline style overrides */
            sb.append("*[style] { max-width:100% !important; height:auto !important; }");

            sb.append("</style></head>");
            sb.append("<body><div id=\"ad-root\">");

            sb.append(content != null ? content : "");

            sb.append("</div></body></html>");

            String finalHtml = sb.toString();
            SDKLogger.d("BannerViewFactory", "Loading finalHtml length=" + finalHtml.length());

            // Use a non-null base URL so relative resources and external script tags are allowed to load.
            String base = "https://adcontent.local/";
            webView.loadDataWithBaseURL(base, finalHtml, "text/html", "utf-8", null);

        });

        return webView;
    }
}
