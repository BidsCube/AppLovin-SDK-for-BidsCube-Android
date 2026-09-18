package com.bidscube.sdk.qa;

import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.utils.AdmSanitizer;
import com.bidscube.sdk.utils.SDKLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debug-only injected ADM for QA (skips HTTP). Active only when {@link SDKConfig#isEnableDebugMode()}.
 */
public final class QaAdmOverride {

    private static final String TAG = "QaAdmOverride";
    private static final Map<String, String> OVERRIDES = new ConcurrentHashMap<>();

    private QaAdmOverride() {
    }

    public static void set(String placementId, String admOrJson) {
        if (placementId == null || placementId.trim().isEmpty()) {
            return;
        }
        if (admOrJson == null || admOrJson.trim().isEmpty()) {
            OVERRIDES.remove(placementId.trim());
            return;
        }
        OVERRIDES.put(placementId.trim(), admOrJson);
        SDKLogger.d(TAG, "set override placement=" + placementId.trim()
                + " len=" + admOrJson.length());
    }

    public static void clearAll() {
        OVERRIDES.clear();
        SDKLogger.d(TAG, "cleared all overrides");
    }

    public static void clear(String placementId) {
        if (placementId != null) {
            OVERRIDES.remove(placementId.trim());
        }
    }

    /**
     * @return sanitized ADM markup, or {@code null} when no override or debug mode off.
     */
    public static String resolveAdm(SDKConfig config, String placementId) {
        if (config == null || !config.isEnableDebugMode()) {
            return null;
        }
        if (placementId == null) {
            return null;
        }
        String raw = OVERRIDES.get(placementId.trim());
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            String extracted = AdmSanitizer.extractMarkupFromResponseBody(trimmed);
            return extracted != null && !extracted.isEmpty() ? extracted : null;
        }
        return AdmSanitizer.sanitize(trimmed);
    }
}
