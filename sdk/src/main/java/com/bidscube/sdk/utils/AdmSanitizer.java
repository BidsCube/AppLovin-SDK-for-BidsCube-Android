package com.bidscube.sdk.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes ad markup (ADM) from Bidscube responses: JSON wrappers, document.write shells, escapes.
 */
public final class AdmSanitizer {

    private static final Pattern ADM_KEY_PATTERN = Pattern.compile("(?is)\"adm\"\\s*:");
    private static final Pattern POSITION_PATTERN = Pattern.compile("(?is)\"position\"\\s*:\\s*(\\d+)");
    private static final Pattern BANNER_SPAN_PATTERN =
            Pattern.compile("(?is)<span[^>]*\\bid\\s*=\\s*[\"']banner_[^\"']*[\"'][^>]*>(.*?)</span>");
    private static final Pattern WRAPPER_DIV_START =
            Pattern.compile("(?is)<div\\s+id\\s*=\\s*[\"']wrapper_[^\"']*[\"'][^>]*>");
    private static final int TOP_LEVEL_JSON_ADM_SCAN_LIMIT = 80;

    private AdmSanitizer() {
    }

    public static String sanitize(String adm) {
        if (adm == null) {
            return null;
        }

        String current = peelBidscubeJsonEnvelope(adm);
        for (int iter = 0; iter < 6; iter++) {
            String trimmed = current.trim();
            String lower = trimmed.toLowerCase(Locale.US);
            if (!lower.contains("document.write") && !lower.contains("document.writeln")) {
                break;
            }

            Pattern p = Pattern.compile("(?is).*document\\.writeln?\\s*\\((.*)\\)\\s*;?\\s*$");
            Matcher m = p.matcher(trimmed);
            String extracted = null;
            if (m.matches()) {
                extracted = m.group(1);
            } else {
                int docIdx = lower.indexOf("document.write");
                if (docIdx == -1) {
                    docIdx = lower.indexOf("document.writeln");
                }
                if (docIdx != -1) {
                    int openIdx = trimmed.indexOf('(', docIdx);
                    if (openIdx >= 0) {
                        int depth = 0;
                        int closeIdx = -1;
                        for (int i = openIdx; i < trimmed.length(); i++) {
                            char c = trimmed.charAt(i);
                            if (c == '(') {
                                depth++;
                            } else if (c == ')') {
                                depth--;
                                if (depth == 0) {
                                    closeIdx = i;
                                    break;
                                }
                            }
                        }
                        if (closeIdx > openIdx) {
                            extracted = trimmed.substring(openIdx + 1, closeIdx);
                        }
                    }
                }
            }

            if (extracted == null) {
                break;
            }

            String inner = unwrapFunctionWrapping(extracted.trim());
            if (inner.length() >= 2) {
                char start = inner.charAt(0);
                char end = inner.charAt(inner.length() - 1);
                if ((start == '\'' && end == '\'') || (start == '"' && end == '"') || (start == '`' && end == '`')) {
                    inner = inner.substring(1, inner.length() - 1);
                }
            }

            inner = unescapeJsString(inner);
            inner = peelBidscubeJsonEnvelope(inner);
            inner = extractNestedAdmFromSpan(inner);

            if (!inner.equals(current)) {
                current = inner;
            } else {
                break;
            }
        }

        current = extractNestedAdmFromSpan(current);
        current = collapseToWrapperCreative(current);
        current = stripOrphanNestedJsonText(current);
        return current.trim();
    }

    /**
     * Extracts adm markup from a raw HTTP body (full JSON or partial).
     */
    public static String extractMarkupFromResponseBody(String responseBody) {
        if (responseBody == null) {
            return null;
        }
        String trimmed = responseBody.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            JSONObject json = new JSONObject(trimmed);
            if (json.has("adm")) {
                Object admValue = json.get("adm");
                if (admValue instanceof String) {
                    String adm = ((String) admValue).trim();
                    if (!adm.isEmpty()) {
                        return sanitize(adm);
                    }
                }
            }
        } catch (JSONException ignored) {
            // lenient extraction below
        }

        String extracted = extractAdmFieldLenient(trimmed);
        return extracted != null ? sanitize(extracted) : sanitize(trimmed);
    }

    public static int extractPositionFromResponseBody(String responseBody, int defaultValue) {
        if (responseBody == null) {
            return defaultValue;
        }
        try {
            JSONObject json = new JSONObject(responseBody.trim());
            return json.optInt("position", defaultValue);
        } catch (JSONException jsonError) {
            Matcher m = POSITION_PATTERN.matcher(responseBody);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException numberError) {
                    // keep default
                }
            }
        }
        return defaultValue;
    }

    private static String peelBidscubeJsonEnvelope(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return raw;
        }

        if (!looksLikeTopLevelBidscubeJsonEnvelope(s)) {
            return s;
        }

        try {
            JSONObject json = new JSONObject(s);
            if (json.has("adm")) {
                Object admValue = json.get("adm");
                if (admValue instanceof String) {
                    String adm = ((String) admValue).trim();
                    if (!adm.isEmpty()) {
                        return adm;
                    }
                }
            }
        } catch (JSONException ignored) {
            // lenient path
        }

        String extracted = extractAdmFieldLenient(s);
        if (extracted != null && !extracted.equals(s)) {
            String again = peelBidscubeJsonEnvelope(extracted);
            return again != null ? again : extracted;
        }
        return extracted != null ? extracted : s;
    }

    /**
     * True only for a top-level API object ({ "adm": ... }), not for "adm" keys embedded in HTML/JS.
     */
    private static boolean looksLikeTopLevelBidscubeJsonEnvelope(String s) {
        String t = s.trim();
        if (!t.startsWith("{")) {
            return false;
        }
        int scanEnd = Math.min(t.length(), TOP_LEVEL_JSON_ADM_SCAN_LIMIT);
        return ADM_KEY_PATTERN.matcher(t.substring(0, scanEnd)).find();
    }

    /**
     * Replaces &lt;span id="banner_..."&gt;{ "adm": "..." }&lt;/span&gt; with the decoded HTML creative.
     */
    private static String extractNestedAdmFromSpan(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        Matcher spanMatcher = BANNER_SPAN_PATTERN.matcher(html);
        if (!spanMatcher.find()) {
            return html;
        }
        String spanBody = spanMatcher.group(1);
        String creative = extractMarkupFromNestedJsonBlob(spanBody);
        if (creative == null || !creative.startsWith("<")) {
            return html;
        }
        String tail = extractMarkupAfterNestedJson(spanBody);
        if (tail != null && !tail.isEmpty()) {
            creative = creative + tail;
        }
        return spanMatcher.replaceFirst(Matcher.quoteReplacement(creative));
    }

    /**
     * When a wrapper_* div exists, drop document.write positioning shells and keep the actual ad unit.
     */
    private static String collapseToWrapperCreative(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        Matcher startMatcher = WRAPPER_DIV_START.matcher(html);
        if (!startMatcher.find()) {
            return html;
        }
        String extracted = extractFromWrapperDiv(html, startMatcher.start());
        return extracted != null ? extracted : html;
    }

    private static String extractFromWrapperDiv(String html, int start) {
        int pos = start;
        int depth = 0;
        int lastClose = -1;
        while (pos < html.length()) {
            int nextOpen = indexOfIgnoreCase(html, "<div", pos);
            int nextClose = indexOfIgnoreCase(html, "</div", pos);
            if (nextClose == -1 && nextOpen == -1) {
                break;
            }
            if (nextOpen != -1 && (nextClose == -1 || nextOpen < nextClose)) {
                depth++;
                pos = nextOpen + 4;
                continue;
            }
            depth--;
            lastClose = nextClose;
            pos = nextClose + 6;
            if (depth <= 0) {
                break;
            }
        }
        if (lastClose < 0) {
            return null;
        }
        int end = html.indexOf('>', lastClose);
        if (end < 0) {
            return null;
        }
        end++;
        StringBuilder out = new StringBuilder(html.substring(start, end));
        String remainder = html.substring(end);
        Matcher scriptMatcher = Pattern.compile("(?is)^\\s*(<script[^>]*>.*?</script>\\s*)+").matcher(remainder);
        if (scriptMatcher.find()) {
            out.append(scriptMatcher.group());
        }
        return out.toString().trim();
    }

    private static int indexOfIgnoreCase(String haystack, String needle, int from) {
        String lowerHay = haystack.toLowerCase(Locale.US);
        String lowerNeedle = needle.toLowerCase(Locale.US);
        return lowerHay.indexOf(lowerNeedle, from);
    }

    private static String extractMarkupFromNestedJsonBlob(String blob) {
        if (blob == null) {
            return null;
        }
        String trimmed = blob.trim();
        if (!ADM_KEY_PATTERN.matcher(trimmed).find()) {
            return null;
        }
        String extracted = extractAdmFieldLenient(trimmed);
        return extracted != null ? extracted.trim() : null;
    }

    /** Scripts/imgs placed after the nested {@code }} inside the banner span. */
    private static String extractMarkupAfterNestedJson(String spanBody) {
        if (spanBody == null) {
            return null;
        }
        int jsonEnd = spanBody.indexOf('}');
        if (jsonEnd < 0) {
            return null;
        }
        String tail = spanBody.substring(jsonEnd + 1).trim();
        if (tail.startsWith("<")) {
            return tail;
        }
        return null;
    }

    /** Removes leftover brace lines when nested JSON was rendered as plain text. */
    private static String stripOrphanNestedJsonText(String html) {
        if (html == null) {
            return null;
        }
        String s = html;
        s = s.replaceAll("(?is)\\{\\s*\"adm\"\\s*:\\s*\"", "");
        s = s.replaceAll("(?is)\"\\s*\\}\\s*", "");
        s = s.replaceAll("(?m)^\\s*\\{\\s*$", "");
        s = s.replaceAll("(?m)^\\s*\\}\\s*$", "");
        return s;
    }

    /**
     * Reads the JSON string value of "adm" even when JSONObject fails (unescaped quotes, multiline HTML).
     */
    private static String extractAdmFieldLenient(String jsonBody) {
        Matcher keyMatcher = ADM_KEY_PATTERN.matcher(jsonBody);
        if (!keyMatcher.find()) {
            return null;
        }

        int index = keyMatcher.end();
        while (index < jsonBody.length() && Character.isWhitespace(jsonBody.charAt(index))) {
            index++;
        }
        if (index >= jsonBody.length()) {
            return null;
        }

        char first = jsonBody.charAt(index);
        if (first == '"' || first == '\'') {
            int valueStart = index + 1;
            if (valueStart < jsonBody.length() && jsonBody.charAt(valueStart) == '<') {
                String htmlAdm = readHtmlAdmJsonString(jsonBody, index + 1);
                if (htmlAdm != null && !htmlAdm.isEmpty()) {
                    return htmlAdm;
                }
            }
            return readQuotedJsonString(jsonBody, index);
        }

        // Rare: adm without quotes — read until , "position" or closing brace
        int end = index;
        while (end < jsonBody.length()) {
            char c = jsonBody.charAt(end);
            if (c == ',' && end + 1 < jsonBody.length()) {
                String tail = jsonBody.substring(end).trim();
                if (tail.startsWith(",\"position\"") || tail.startsWith(", \"position\"")) {
                    break;
                }
            }
            if (c == '}' && end > index) {
                break;
            }
            end++;
        }
        String slice = jsonBody.substring(index, end).trim();
        return slice.isEmpty() ? null : slice;
    }

    /**
     * Bidscube sometimes returns HTML in "adm" with unescaped attribute quotes; read until {@code "</div>"}
     * before the closing {@code "} of the JSON string.
     */
    private static String readHtmlAdmJsonString(String source, int valueStart) {
        if (valueStart < 0 || valueStart >= source.length()) {
            return null;
        }
        int jsonClose = indexOfHtmlAdmJsonTerminator(source, valueStart);
        if (jsonClose > valueStart) {
            return source.substring(valueStart, jsonClose);
        }
        String marker = "</div>";
        int markerIdx = source.indexOf(marker, valueStart);
        if (markerIdx < 0) {
            return null;
        }
        int afterMarker = markerIdx + marker.length();
        while (afterMarker < source.length() && Character.isWhitespace(source.charAt(afterMarker))) {
            afterMarker++;
        }
        if (afterMarker < source.length() && source.charAt(afterMarker) == '"') {
            return source.substring(valueStart, markerIdx + marker.length());
        }
        return null;
    }

    /** Closing {@code "} of nested {@code "adm":"<html>..."} before {@code }}. */
    private static int indexOfHtmlAdmJsonTerminator(String source, int valueStart) {
        String[] terminators = {"\"\n}", "\"\r\n}", "\"}", "\" \n}", "\"\n }"};
        int best = -1;
        for (String term : terminators) {
            int idx = source.indexOf(term, valueStart);
            if (idx > valueStart && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }

    private static String readQuotedJsonString(String source, int quoteIndex) {
        char quote = source.charAt(quoteIndex);
        StringBuilder out = new StringBuilder();
        for (int i = quoteIndex + 1; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\\' && i + 1 < source.length()) {
                char esc = source.charAt(i + 1);
                switch (esc) {
                    case '"':
                    case '\'':
                    case '\\':
                    case '/':
                        out.append(esc);
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'b':
                        out.append('\b');
                        break;
                    case 'f':
                        out.append('\f');
                        break;
                    case 'u':
                        if (i + 5 < source.length()) {
                            try {
                                int code = Integer.parseInt(source.substring(i + 2, i + 6), 16);
                                out.append((char) code);
                                i += 5;
                                break;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        out.append('\\').append(esc);
                        break;
                    default:
                        out.append('\\').append(esc);
                        break;
                }
                i++;
                continue;
            }
            if (c == quote) {
                return out.toString();
            }
            out.append(c);
        }
        return out.length() > 0 ? out.toString() : null;
    }

    private static String unescapeJsString(String inner) {
        String s = inner.replace("\\'", "'")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\/", "/");
        if (s.contains("%3C") || s.contains("%3c") || s.contains("%22") || s.contains("%7B")) {
            try {
                s = URLDecoder.decode(s, StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {
            }
        }
        return s;
    }

    private static String unwrapFunctionWrapping(String s) {
        if (s == null) {
            return null;
        }
        String out = s.trim();
        Pattern pf = Pattern.compile("(?is)^(\\w+)\\s*\\((.*)\\)\\s*$");
        boolean peeled = true;
        while (peeled) {
            peeled = false;
            Matcher mf = pf.matcher(out);
            if (mf.matches()) {
                String fn = mf.group(1);
                String inner = Objects.requireNonNull(mf.group(2)).trim();
                if (fn.equalsIgnoreCase("unescape")
                        || fn.equalsIgnoreCase("decodeURIComponent")
                        || fn.equalsIgnoreCase("decodeURI")) {
                    out = inner;
                    peeled = true;
                }
            }
        }
        return out;
    }
}
