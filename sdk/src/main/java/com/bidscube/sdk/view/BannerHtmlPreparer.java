package com.bidscube.sdk.view;

import com.bidscube.sdk.utils.AdmSanitizer;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds final HTML for banner WebView loads. Keeps tracking pixels/scripts intact.
 */
public final class BannerHtmlPreparer {

    /** Matches width/height exactly 1 (not 10, 100, 150, …). */
    private static final Pattern ONE_PX_DIM = Pattern.compile(
            "(?i)(?:\\bwidth\\s*=\\s*[\"']?1(?![0-9])|\\bheight\\s*=\\s*[\"']?1(?![0-9]))");

    /** Legacy stripper — reproduces pre-fix behaviour for regression tests only. */
    private static final Pattern LEGACY_TRACKING_IMG_STRIP = Pattern.compile(
            "(?i)<img[^>]*(?:width\\s*=\\s*['\"]?1['\"]?|height\\s*=\\s*['\"]?1['\"]?)[^>]*>");

    private BannerHtmlPreparer() {
    }

    public static String sanitizeAdm(String rawAdm) {
        return AdmSanitizer.sanitize(rawAdm);
    }

    /**
     * ADM after {@link AdmSanitizer} only — no BannerViewFactory mutations.
     */
    public static String prepareSanitizedContent(String rawAdm) {
        String content = sanitizeAdm(rawAdm);
        return content != null ? content : "";
    }

    /**
     * Full HTML document passed to {@code WebView.loadDataWithBaseURL}.
     */
    public static String prepareFinalHtml(String rawAdm) {
        return wrapInDocument(prepareSanitizedContent(rawAdm));
    }

    /**
     * Applies the old BannerViewFactory img stripping (for defect reproduction in tests).
     */
    public static String applyLegacyTrackingStrip(String sanitizedContent) {
        if (sanitizedContent == null) {
            return "";
        }
        return LEGACY_TRACKING_IMG_STRIP.matcher(sanitizedContent).replaceAll("");
    }

    public static boolean isOneByOneTrackingImgTag(String imgTag) {
        if (imgTag == null || !imgTag.toLowerCase(Locale.US).contains("<img")) {
            return false;
        }
        return ONE_PX_DIM.matcher(imgTag).find();
    }

    public static boolean containsSubstring(String haystack, String needle) {
        return haystack != null && needle != null && !needle.isEmpty() && haystack.contains(needle);
    }

    public static int countImgTags(String html) {
        if (html == null || html.isEmpty()) {
            return 0;
        }
        Matcher m = Pattern.compile("(?is)<img\\b").matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    public static boolean looksLikeVast(String adm) {
        if (adm == null) {
            return false;
        }
        String t = adm.trim();
        if (t.isEmpty()) {
            return false;
        }
        String lower = t.toLowerCase(Locale.US);
        return lower.startsWith("<vast")
                || lower.contains("<vast ")
                || (lower.startsWith("<?xml") && lower.contains("vast"));
    }

    private static String wrapInDocument(String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head>");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\">");
        sb.append("<style>");
        sb.append("html,body{margin:0;padding:0;width:100%;overflow:hidden;}");
        sb.append("#ad-root{position:relative;width:100%;max-width:100%;overflow:hidden;box-sizing:border-box;}");
        // Main creative scales to slot; attributes on markup are preserved.
        sb.append("#ad-root img.bc-creative, #ad-root > a img, #ad-root > img {");
        sb.append("max-width:100%;height:auto;display:block;}");
        // Tracking beacons stay in DOM and remain loadable (no display:none).
        sb.append("#ad-root img.bc-track, #ad-root img[width=\"1\"], #ad-root img[height=\"1\"],");
        sb.append("#ad-root img[width='1'], #ad-root img[height='1'] {");
        sb.append("position:absolute!important;width:1px!important;height:1px!important;");
        sb.append("max-width:1px!important;max-height:1px!important;opacity:0.01!important;");
        sb.append("pointer-events:none!important;overflow:hidden!important;}");
        sb.append("</style></head><body><div id=\"ad-root\">");
        sb.append(tagTrackingImages(content));
        sb.append("</div></body></html>");
        return sb.toString();
    }

    /** Marks 1×1 imgs for CSS; does not remove tags or src URLs. */
    static String tagTrackingImages(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        Matcher m = Pattern.compile("(?is)<img\\b[^>]*>").matcher(content);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String tag = m.group();
            if (isOneByOneTrackingImgTag(tag) && !tag.toLowerCase(Locale.US).contains("bc-track")) {
                String tagged = tag.replaceFirst("(?i)<img\\b", "<img class=\"bc-track\"");
                m.appendReplacement(out, Matcher.quoteReplacement(tagged));
            } else if (!tag.toLowerCase(Locale.US).contains("bc-creative")
                    && !isOneByOneTrackingImgTag(tag)
                    && tag.toLowerCase(Locale.US).contains("width=\"320\"")) {
                String tagged = tag.replaceFirst("(?i)<img\\b", "<img class=\"bc-creative\"");
                m.appendReplacement(out, Matcher.quoteReplacement(tagged));
            } else {
                m.appendReplacement(out, Matcher.quoteReplacement(tag));
            }
        }
        m.appendTail(out);
        return out.toString();
    }
}
