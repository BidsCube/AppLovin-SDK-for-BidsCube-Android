package com.applovin.mediation.adapters;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Parses Bidscube init Server Parameters for {@link com.bidscube.sdk.config.SDKConfig}.
 */
final class BidscubeServerParameters {

    private static final String TAG = "BidscubeMediationAdapter";

    private BidscubeServerParameters() {
    }

    static boolean readAutoClose(@Nullable Bundle serverParameters) {
        Boolean parsed = readBoolean(serverParameters, "auto_close");
        if (parsed == null) {
            parsed = readBoolean(serverParameters, "autoClose");
        }
        return parsed != null && parsed;
    }

    @Nullable
    static Boolean readBoolean(@Nullable Bundle serverParameters, String key) {
        if (serverParameters == null || key == null || !serverParameters.containsKey(key)) {
            return null;
        }
        Object raw = serverParameters.get(key);
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        String value = serverParameters.getString(key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
            return Boolean.FALSE;
        }
        Log.w(TAG, "Invalid boolean server parameter '" + key + "': '" + value + "'; using false");
        return null;
    }
}
