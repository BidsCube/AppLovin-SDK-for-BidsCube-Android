package com.bidscube.sdk.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bidscube.sdk.models.natives.NativeAd;
import com.bidscube.sdk.models.natives.NativeAsset;
import com.bidscube.sdk.models.enums.NativeDataType;
import com.bidscube.sdk.models.enums.ImageType;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.lang.ref.WeakReference;

/**
 * Small helper to bind NativeAd model fields to views. Reads the OpenRTB-style
 * native response (assets list) and binds title, description (data), icon/main images.
 * <p>
 * Added flexibility: bind by root view + resource IDs, bind CTA, and attach click
 * handler to open the ad link. This lets SDK users provide custom layouts and
 * makes the native ad renderable as a banner (main image + icon + texts + CTA).
 */
public final class NativeAdBinder {

    private NativeAdBinder() {
    }

    // Track the last attached banner and its container so cleanup/different ad loads remove previous banner.
    private static WeakReference<ViewGroup> sCurrentContainerRef = null;
    private static WeakReference<View> sCurrentBannerRef = null;
    private static WeakReference<Context> sCurrentContextRef = null;

    private static boolean isLikelyCta(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        if (t.isEmpty()) return false;
        // common CTA cues and short labels
        String[] cues = new String[]{"jetzt", "bewerben", "apply", "learn", "more", "install", "buy", "get", "start", "open", "download", "signup", "sign up", "mehr", "mehr erfahren", "bewerben"};
        for (String c : cues) {
            if (t.contains(c)) return true;
        }
        // short strings likely to be CTA (<= 20 chars)
        return t.length() <= 20;
    }

    public static void bindToTextView(TextView tv, String text) {
        if (tv == null) return;
        tv.setText(text != null ? text : "");
        // Make the view visible only when content exists (apps can override)
        tv.setVisibility(text != null && !text.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public static void bindToImageView(Context ctx, ImageView iv, String url, Drawable placeholder) {
        if (iv == null) return;
        // Ensure the ImageView is configured to respect image aspect and scaling
        try {
            iv.setAdjustViewBounds(true);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } catch (Exception ignored) {
        }

        if (url == null || url.trim().isEmpty()) {
            if (placeholder != null) iv.setImageDrawable(placeholder);
            return;
        }

        try {
            Glide.with(ctx.getApplicationContext())
                    .load(url)
                    .placeholder(placeholder)
                    .into(iv);
        } catch (Exception e) {
            // Glide can throw runtime exceptions if called with a bad context; fall back to placeholder
            if (placeholder != null) iv.setImageDrawable(placeholder);
        }
    }

    /**
     * Load image into ImageView with optional override size and scaleType.
     * When overrideW/overrideH are provided, Glide will request a resized image which avoids handling huge bitmaps
     * for very small banner containers.
     */
    public static void loadImageIntoView(Context ctx, ImageView iv, String url, Drawable placeholder, Integer overrideW, Integer overrideH, ImageView.ScaleType scaleType) {
        if (iv == null) return;
        try {
            iv.setAdjustViewBounds(true);
            if (scaleType != null) iv.setScaleType(scaleType);
        } catch (Exception ignored) {
        }

        if (url == null || url.trim().isEmpty()) {
            if (placeholder != null) iv.setImageDrawable(placeholder);
            return;
        }

        try {
            RequestOptions options = new RequestOptions();
            if (overrideW != null && overrideH != null && overrideW > 0 && overrideH > 0) {
                options = options.override(overrideW, overrideH).centerInside();
            }
            Glide.with(ctx.getApplicationContext())
                    .load(url)
                    .apply(options)
                    .placeholder(placeholder)
                    .into(iv);
        } catch (Exception e) {
            if (placeholder != null) iv.setImageDrawable(placeholder);
        }
    }

    /**
     * Delegates to the full bind(...) method with null CTA view.
     */
    public static void bind(NativeAd nativeAd, Context ctx,
                            TextView titleView, TextView bodyView, ImageView iconView, ImageView mainImageView) {
        bind(nativeAd, ctx, titleView, bodyView, /*ctaView=*/ null, iconView, mainImageView);
    }

    /**
     * Convenience binder: scans the native ad assets to find title, description, cta and images
     */
    public static void bind(NativeAd nativeAd, Context ctx,
                            TextView titleView, TextView bodyView, TextView ctaView, ImageView iconView, ImageView mainImageView) {
        if (nativeAd == null) return;

        String titleText = null;
        String bodyText = null;
        String ctaText = null;
        String iconUrl = null;
        String mainUrl = null;

        // Fallbacks in case typed assets aren't present
        String firstImgUrl = null;

        if (nativeAd.assets != null) {
            for (NativeAsset a : nativeAd.assets) {
                if (a == null) continue;
                if (a.title != null && a.title.text != null && titleText == null) {
                    titleText = a.title.text;
                }
                if (a.data != null && a.data.value != null) {
                    NativeDataType t = a.data.type;
                    // Try to map common data types to roles
                    if (t != null) {
                        switch (t) {
                            case DESC:
                                if (bodyText == null) bodyText = a.data.value;
                                break;
                            case CTATEXT:
                                if (ctaText == null) ctaText = a.data.value;
                                break;
                            default:
                                // Fallback to body if nothing else
                                if (bodyText == null) bodyText = a.data.value;
                                break;
                        }
                    } else {
                        // If data.type missing, try to heuristically detect CTA vs body
                        if (isLikelyCta(a.data.value)) {
                            if (ctaText == null) ctaText = a.data.value;
                        } else {
                            if (bodyText == null) bodyText = a.data.value;
                        }
                    }
                }
                if (a.img != null && a.img.url != null) {
                    if (firstImgUrl == null) firstImgUrl = a.img.url;
                    ImageType it = a.img.type;
                    if (it == ImageType.ICON && iconUrl == null) {
                        iconUrl = a.img.url;
                    }
                    if (it == ImageType.MAIN && mainUrl == null) {
                        mainUrl = a.img.url;
                    }
                }
            }
        }

        // Apply fallbacks
        if (mainUrl == null) mainUrl = firstImgUrl;
        if (iconUrl == null) iconUrl = firstImgUrl;

        bindToTextView(titleView, titleText);
        bindToTextView(bodyView, bodyText);
        bindToTextView(ctaView, ctaText);
        bindToImageView(ctx, iconView, iconUrl, null);
        bindToImageView(ctx, mainImageView, mainUrl, null);

        // If a click URL exists, attach it to the most prominent view(s)
        if (nativeAd.link != null && nativeAd.link.url != null) {
            final String clickUrl = nativeAd.link.url;
            attachClickHandler(titleView, clickUrl, ctx);
            attachClickHandler(bodyView, clickUrl, ctx);
            attachClickHandler(ctaView, clickUrl, ctx);
            attachClickHandler(iconView, clickUrl, ctx);
            attachClickHandler(mainImageView, clickUrl, ctx);
        }
    }

    /**
     * Bind by providing the root view and resource IDs. This makes it easy for SDK users
     * to supply their own layout (e.g., banner-like native layout) and have the binder populate it.
     * Example: NativeAdBinder.bindToView(nativeAd, context, root, R.id.title, R.id.body, R.id.cta, R.id.icon, R.id.mainImage);
     */
    public static void bindToView(NativeAd nativeAd, Context ctx, View root,
                                  int titleResId, int bodyResId, int ctaResId, int iconResId, int mainImageResId) {
        if (root == null) return;
        TextView title = null;
        TextView body = null;
        TextView cta = null;
        ImageView icon = null;
        ImageView main = null;

        try {
            title = root.findViewById(titleResId);
        } catch (Exception ignored) {
        }
        try {
            body = root.findViewById(bodyResId);
        } catch (Exception ignored) {
        }
        try {
            cta = root.findViewById(ctaResId);
        } catch (Exception ignored) {
        }
        try {
            icon = root.findViewById(iconResId);
        } catch (Exception ignored) {
        }
        try {
            main = root.findViewById(mainImageResId);
        } catch (Exception ignored) {
        }

        bind(nativeAd, ctx, title, body, cta, icon, main);
    }

    private static void attachClickHandler(final View v, final String url, final Context ctx) {
        if (v == null || url == null || url.isEmpty() || ctx == null) return;
        v.setClickable(true);
        v.setOnClickListener(view -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Create a simple full-width banner View from native ad. Uses white background by default.
     * Backward-compatible wrapper that uses Color.WHITE as the background.
     */
    public static View createBannerView(Context ctx, NativeAd nativeAd, int widthPx, int heightPx) {
        return createBannerView(ctx, nativeAd, widthPx, heightPx, Color.WHITE);
    }

    /**
     * Create a banner view with a configurable background color. Backward-compat: shows disclosure by default.
     */
    public static View createBannerView(Context ctx, NativeAd nativeAd, int widthPx, int heightPx, int backgroundColor) {
        return createBannerView(ctx, nativeAd, widthPx, heightPx, backgroundColor, true);
    }

    /**
     * Create a banner view with a configurable background color and disclosure toggle.
     * Uses CENTER_CROP for main image and ellipsized texts by default.
     */
    public static View createBannerView(Context ctx, NativeAd nativeAd, int widthPx, int heightPx, int backgroundColor, boolean showAdDisclosure) {
        return createBannerView(ctx, nativeAd, widthPx, heightPx, backgroundColor, showAdDisclosure, ImageView.ScaleType.CENTER_CROP, true);
    }

    /**
     * Create a banner view with additional layout options.
     *
     * @param mainImageScaleType how the main creative image should be scaled inside the banner
     * @param ellipsizeText      whether to ellipsize long title/body/cta text so the banner keeps its height
     */
    public static View createBannerView(Context ctx, NativeAd nativeAd, int widthPx, int heightPx, int backgroundColor, boolean showAdDisclosure, ImageView.ScaleType mainImageScaleType, boolean ellipsizeText) {
        return createBannerView(ctx, nativeAd, widthPx, heightPx, backgroundColor, showAdDisclosure, mainImageScaleType, ellipsizeText, com.bidscube.sdk.models.enums.AdPosition.UNKNOWN);
    }

    /**
     * AdPosition-aware banner creation. If position is HEADER/FOOTER/SIDEBAR we position the overlay accordingly
     */
    public static View createBannerView(Context ctx, NativeAd nativeAd, int widthPx, int heightPx, int backgroundColor, boolean showAdDisclosure, ImageView.ScaleType mainImageScaleType, boolean ellipsizeText, com.bidscube.sdk.models.enums.AdPosition position) {
        if (ctx == null || nativeAd == null) return null;

        // Extract main image and text similarly to bind()
        String titleText = null;
        String ctaText = null;
        String mainUrl = null;
        String iconUrl = null;
        String firstImgUrl = null;

        String bodyText = null;
        if (nativeAd.assets != null) {
            for (NativeAsset a : nativeAd.assets) {
                if (a == null) continue;
                if (a.title != null && a.title.text != null && titleText == null)
                    titleText = a.title.text;
                if (a.data != null && a.data.value != null) {
                    NativeDataType t = a.data.type;
                    if (t != null) {
                        switch (t) {
                            case DESC:
                                if (bodyText == null) bodyText = a.data.value;
                                break;
                            case CTATEXT:
                                if (ctaText == null) ctaText = a.data.value;
                                break;
                            default:
                                if (bodyText == null) bodyText = a.data.value;
                                break;
                        }
                    } else {
                        // heuristic: CTA vs body
                        if (isLikelyCta(a.data.value)) {
                            if (ctaText == null) ctaText = a.data.value;
                        } else {
                            if (bodyText == null) bodyText = a.data.value;
                        }
                    }
                }
                if (a.img != null && a.img.url != null) {
                    if (firstImgUrl == null) firstImgUrl = a.img.url;
                    ImageType it = a.img.type;
                    if (it == ImageType.ICON && iconUrl == null) iconUrl = a.img.url;
                    if (it == ImageType.MAIN && mainUrl == null) mainUrl = a.img.url;
                }
            }
        }
        if (mainUrl == null) mainUrl = firstImgUrl;

        // Root container: use explicit widthPx/heightPx (previous behavior) so the banner has the intended size
        FrameLayout root = new FrameLayout(ctx);
        FrameLayout.LayoutParams rootLp = new FrameLayout.LayoutParams(widthPx, heightPx);
        root.setLayoutParams(rootLp);
        // Set background color
        try {
            root.setBackgroundColor(backgroundColor);
        } catch (Exception ignored) {
        }

        // Main image view - fills parent (may be skipped for very small banners)
        ImageView mainIv = new ImageView(ctx);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mainIv.setLayoutParams(ivLp);
        try {
            mainIv.setAdjustViewBounds(true);
        } catch (Exception ignored) {
        }
        try {
            mainIv.setScaleType(mainImageScaleType != null ? mainImageScaleType : ImageView.ScaleType.CENTER_CROP);
        } catch (Exception ignored) {
        }
        try {
            mainIv.setBackgroundColor(backgroundColor);
        } catch (Exception ignored) {
        }

        // Decide whether to load the main creative.
        // Requirement: if native ad is NOT full screen, ignore big main images — only show title/subtitle and a small icon.
        float densityLocal = ctx.getResources().getDisplayMetrics().density;
        int thresholdPx = (int) (200 * densityLocal + 0.5f);
        boolean mainLoaded = false;
        if (mainUrl != null) {
            // Only load the full main image when position is FULL_SCREEN and height is sufficiently large.
            if (position == com.bidscube.sdk.models.enums.AdPosition.FULL_SCREEN && heightPx > 0 && heightPx >= thresholdPx) {
                // request a resized bitmap matching the banner dimensions to avoid large memory/scale issues
                loadImageIntoView(ctx, mainIv, mainUrl, null, widthPx > 0 ? widthPx : null, heightPx, mainImageScaleType != null ? mainImageScaleType : ImageView.ScaleType.CENTER_CROP);
                root.addView(mainIv);
                mainLoaded = true;
            } else {
                // Non-fullscreen: do not load large main creative; rely on icon + text
                mainLoaded = false;
            }
        }

        // Overlay: icon | (title + body) | CTA
        LinearLayout overlay = new LinearLayout(ctx);
        // Overlay gravity should respect the requested AdPosition (header/footer/sidebar).
        // For non-fullscreen header/footer we want overlay anchored to top/bottom respectively so content is visible.
        int overlayGravity;
        if (position == null) position = com.bidscube.sdk.models.enums.AdPosition.UNKNOWN;
        switch (position) {
            case HEADER:
                // If main image occupies the banner, keep overlay at bottom; otherwise anchor overlay at top so it's visible in header
                overlayGravity = mainLoaded ? Gravity.BOTTOM : Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                break;
            case FOOTER:
                // For footer: if a large main image occupies the banner, keep overlay at bottom; otherwise anchor at top
                overlayGravity = mainLoaded ? (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL) : (Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                break;
            case SIDEBAR:
                overlayGravity = Gravity.CENTER_VERTICAL | Gravity.START;
                break;
            case FULL_SCREEN:
                overlayGravity = mainLoaded ? Gravity.BOTTOM : Gravity.CENTER;
                break;
            default:
                overlayGravity = mainLoaded ? Gravity.BOTTOM : Gravity.CENTER;
                break;
        }
        FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, overlayGravity);
        overlay.setLayoutParams(overlayLp);
        overlay.setOrientation(LinearLayout.HORIZONTAL);
        int padH = (int) (12 * ctx.getResources().getDisplayMetrics().density + 0.5f);
        int padV = (int) (8 * ctx.getResources().getDisplayMetrics().density + 0.5f);
        overlay.setPadding((int) (16 * ctx.getResources().getDisplayMetrics().density + 0.5f), padV, (int) (16 * ctx.getResources().getDisplayMetrics().density + 0.5f), padV);
        // Use stronger overlay to guarantee text readability
        overlay.setBackgroundColor(Color.parseColor("#CC000000"));
        // Minimum height for the overlay so text is not clipped
        overlay.setMinimumHeight((int) (56 * ctx.getResources().getDisplayMetrics().density + 0.5f));

        float density = ctx.getResources().getDisplayMetrics().density;
        int iconSize = (int) (48 * density + 0.5f);
        ImageView iconIv = new ImageView(ctx);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.setMarginEnd((int) (8 * density));
        iconIv.setLayoutParams(iconLp);
        iconIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        // Only show icon if we have a URL from the native response; do NOT use android default icon as fallback
        boolean hasIcon = (iconUrl != null && !iconUrl.trim().isEmpty());
        if (hasIcon) {
            bindToImageView(ctx, iconIv, iconUrl, null);
            iconIv.setVisibility(View.VISIBLE);
        } else {
            // hide icon view when no remote icon available
            iconIv.setVisibility(View.GONE);
        }

        // Text column (title above body)
        LinearLayout textColumn = new LinearLayout(ctx);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textColLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textColumn.setLayoutParams(textColLp);

        TextView titleTv = new TextView(ctx);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(16);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        if (ellipsizeText) {
            titleTv.setMaxLines(2);
            try {
                titleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            } catch (Exception ignored) {
            }
        }
        bindToTextView(titleTv, titleText);

        TextView bodyTv = new TextView(ctx);
        bodyTv.setTextColor(Color.WHITE);
        bodyTv.setTextSize(14);
        bodyTv.setLineSpacing(0f, 1.05f);
        if (ellipsizeText) {
            bodyTv.setMaxLines(2);
            try {
                bodyTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            } catch (Exception ignored) {
            }
        }
        bindToTextView(bodyTv, bodyText);

        textColumn.addView(titleTv);
        textColumn.addView(bodyTv);

        TextView ctaTv = new TextView(ctx);
        LinearLayout.LayoutParams ctaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ctaTv.setLayoutParams(ctaLp);
        ctaTv.setTextColor(Color.WHITE);
        ctaTv.setTextSize(14);
        ctaTv.setPadding(12, 6, 12, 6);
        ctaTv.setBackgroundColor(Color.parseColor("#FF6200EE"));
        if (ellipsizeText) {
            ctaTv.setMaxLines(1);
            try {
                ctaTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            } catch (Exception ignored) {
            }
        }
        bindToTextView(ctaTv, ctaText != null ? ctaText : "Learn more");

        // ensure icon has right margins and only add if visible
        try {
            iconIv.setLayoutParams(iconLp);
        } catch (Exception ignored) {
        }
        if (iconIv.getVisibility() == View.VISIBLE) {
            overlay.addView(iconIv);
        }
        overlay.addView(textColumn);
        overlay.addView(ctaTv);
        root.addView(overlay);

        // Attach click
        if (nativeAd.link != null && nativeAd.link.url != null)
            attachClickHandler(root, nativeAd.link.url, ctx);

        // Disclosure
        if (showAdDisclosure) {
            try {
                int size = (int) (24 * density + 0.5f);
                int margin = (int) (8 * density + 0.5f);
                ImageView disclosureIv = new ImageView(ctx);
                disclosureIv.setImageResource(android.R.drawable.ic_dialog_info);
                int r = (backgroundColor >> 16) & 0xff;
                int g = (backgroundColor >> 8) & 0xff;
                int b = backgroundColor & 0xff;
                double luminance = (0.299 * r + 0.587 * g + 0.114 * b);
                int tint = luminance > 186 ? Color.BLACK : Color.WHITE;
                try {
                    disclosureIv.setColorFilter(tint);
                } catch (Exception ignored) {
                }
                FrameLayout.LayoutParams discLp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.END);
                discLp.setMargins(margin, margin, margin, margin);
                disclosureIv.setLayoutParams(discLp);
                disclosureIv.setPadding(4, 4, 4, 4);
                disclosureIv.setContentDescription("Ad");
                try {
                    disclosureIv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                } catch (Exception ignored) {
                }
                root.addView(disclosureIv);
            } catch (Exception ignored) {
            }
        }

        return root;
    }

    /**
     * Convenience: remove all children from container and add native banner view using default white background.
     */
    public static View attachBannerToContainer(Context ctx, NativeAd nativeAd, ViewGroup container, int widthPx, int heightPx) {
        return attachBannerToContainer(ctx, nativeAd, container, widthPx, heightPx, Color.WHITE, true, ImageView.ScaleType.CENTER_CROP, true, com.bidscube.sdk.models.enums.AdPosition.UNKNOWN);
    }

    /**
     * Convenience: remove all children from container and add native banner view with a configurable background color.
     */
    public static View attachBannerToContainer(Context ctx, NativeAd nativeAd, ViewGroup container, int widthPx, int heightPx, int backgroundColor) {
        return attachBannerToContainer(ctx, nativeAd, container, widthPx, heightPx, backgroundColor, true, ImageView.ScaleType.CENTER_CROP, true, com.bidscube.sdk.models.enums.AdPosition.UNKNOWN);
    }

    /**
     * Convenience: remove all children from container and add native banner view with a configurable background color and disclosure.
     */
    public static View attachBannerToContainer(Context ctx, NativeAd nativeAd, ViewGroup container, int widthPx, int heightPx, int backgroundColor, boolean showAdDisclosure) {
        return attachBannerToContainer(ctx, nativeAd, container, widthPx, heightPx, backgroundColor, showAdDisclosure, ImageView.ScaleType.CENTER_CROP, true, com.bidscube.sdk.models.enums.AdPosition.UNKNOWN);
    }

    /**
     * Full-featured attach method allowing callers to control image scale type, text ellipsizing and position.
     */
    public static View attachBannerToContainer(Context ctx,
                                               NativeAd nativeAd,
                                               ViewGroup container,
                                               int widthPx,
                                               int heightPx,
                                               int backgroundColor,
                                               boolean showAdDisclosure,
                                               ImageView.ScaleType mainImageScaleType,
                                               boolean ellipsizeText,
                                               com.bidscube.sdk.models.enums.AdPosition position) {
        if (container == null || ctx == null || nativeAd == null) return null;
        container.removeAllViews();
        View banner = createBannerView(ctx, nativeAd, widthPx, heightPx, backgroundColor, showAdDisclosure, mainImageScaleType, ellipsizeText, position);
        if (banner != null) {
            // Ensure the banner fills the provided container area so it's visible and anchored correctly
            try {
                // Use WRAP_CONTENT height so the banner keeps its intended height (important for header/footer)
                int gravity = Gravity.CENTER;
                if (position != null) {
                    switch (position) {
                        case HEADER:
                            gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                            break;
                        case FOOTER:
                            gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                            break;
                        case SIDEBAR:
                            gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                            break;
                        case FULL_SCREEN:
                            gravity = Gravity.CENTER;
                            break;
                        default:
                            gravity = Gravity.CENTER;
                    }
                }
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
                container.addView(banner, lp);

                // Track attached banner/container so clearAttachedBanner works
                synchronized (NativeAdBinder.class) {
                    sCurrentContainerRef = new WeakReference<>(container);
                    sCurrentBannerRef = new WeakReference<>(banner);
                    try { sCurrentContextRef = new WeakReference<>(ctx.getApplicationContext()); } catch (Exception ignored) { sCurrentContextRef = null; }
                }
            } catch (Exception e) {
                // Fallback: add without explicit params
                container.addView(banner);
            }
        }
        return banner;
    }

    /**
     * Backwards-compatible variant that keeps the previous signature but delegates to position-aware method.
     */
    public static View attachBannerToContainer(Context ctx,
                                               NativeAd nativeAd,
                                               ViewGroup container,
                                               int widthPx,
                                               int heightPx,
                                               int backgroundColor,
                                               boolean showAdDisclosure,
                                               ImageView.ScaleType mainImageScaleType,
                                               boolean ellipsizeText) {
        return attachBannerToContainer(ctx, nativeAd, container, widthPx, heightPx, backgroundColor, showAdDisclosure, mainImageScaleType, ellipsizeText, com.bidscube.sdk.models.enums.AdPosition.UNKNOWN);
    }

    /**
     * Bind a host-provided custom layout view (already inflated) using the resource ids specified.
     * The host can fully control layouting and styling; this helper will populate views inside it.
     */
    public static View attachCustomLayoutToContainer(Context ctx, NativeAd nativeAd, ViewGroup container, View customLayout, int titleResId, int bodyResId, int ctaResId, int iconResId, int mainImageResId) {
        if (container == null || ctx == null || nativeAd == null || customLayout == null)
            return null;
        container.removeAllViews();
        bindToView(nativeAd, ctx, customLayout, titleResId, bodyResId, ctaResId, iconResId, mainImageResId);
        container.addView(customLayout);

        // Track custom layout as current banner so it gets cleared on cleanup
        synchronized (NativeAdBinder.class) {
            sCurrentContainerRef = new WeakReference<>(container);
            sCurrentBannerRef = new WeakReference<>(customLayout);
            sCurrentContextRef = new WeakReference<>(ctx.getApplicationContext());
        }

        return customLayout;
    }

    /**
     * Remove currently attached banner (if any). Safe to call during SDK cleanup or before loading a new ad.
     */
    public static void clearAttachedBanner() {
        synchronized (NativeAdBinder.class) {
            View banner = sCurrentBannerRef != null ? sCurrentBannerRef.get() : null;
            ViewGroup container = sCurrentContainerRef != null ? sCurrentContainerRef.get() : null;
            Context ctx = sCurrentContextRef != null ? sCurrentContextRef.get() : null;

            if (banner != null) {
                // Clear images loaded by Glide inside the banner to avoid memory leaks
                try {
                    if (ctx != null) clearGlideImagesInView(ctx, banner);
                } catch (Exception ignored) {
                }

                // Clear click listeners recursively
                try {
                    clearClickListenersRecursively(banner);
                } catch (Exception ignored) {
                }

                // Remove from parent if still attached
                try {
                    if (banner.getParent() instanceof ViewGroup) {
                        ((ViewGroup) banner.getParent()).removeView(banner);
                    } else if (container != null) {
                        container.removeView(banner);
                    }
                } catch (Exception ignored) {
                }
            }

            // Also remove any remaining children in tracked container
            if (container != null) {
                try {
                    container.removeAllViews();
                } catch (Exception ignored) {
                }
            }

            sCurrentBannerRef = null;
            sCurrentContainerRef = null;
            sCurrentContextRef = null;
        }
    }

    /**
     * Remove banner only if it was attached to the provided container.
     */
    public static void detachBannerFromContainer(ViewGroup container) {
        if (container == null) return;
        synchronized (NativeAdBinder.class) {
            ViewGroup tracked = sCurrentContainerRef != null ? sCurrentContainerRef.get() : null;
            if (tracked != null && tracked == container) {
                clearAttachedBanner();
            } else {
                // No tracked banner for this container, still ensure container is cleared
                try {
                    container.removeAllViews();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void clearClickListenersRecursively(View v) {
        if (v == null) return;
        try {
            v.setOnClickListener(null);
            v.setClickable(false);
        } catch (Exception ignored) {
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                clearClickListenersRecursively(vg.getChildAt(i));
            }
        }
    }

    private static void clearGlideImagesInView(Context ctx, View v) {
        if (v == null || ctx == null) return;
        try {
            if (v instanceof ImageView) {
                try {
                    Glide.with(ctx.getApplicationContext()).clear((ImageView) v);
                } catch (Exception ignored) {
                }
            } else if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    clearGlideImagesInView(ctx, vg.getChildAt(i));
                }
            }
        } catch (Exception ignored) {
        }
    }
}
