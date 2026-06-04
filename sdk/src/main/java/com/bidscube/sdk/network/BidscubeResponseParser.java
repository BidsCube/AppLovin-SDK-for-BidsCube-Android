package com.bidscube.sdk.network;

import android.util.Log;

import com.bidscube.sdk.utils.AdmSanitizer;
import com.bidscube.sdk.utils.SDKLogger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class to parse JSON responses into BidscubeResponse objects
 */
public class BidscubeResponseParser {
    
    private static final String TAG = "BidscubeResponseParser";
    
    /**
     * Parse JSON string into BidscubeResponse
     * @param jsonString JSON response string
     * @return BidscubeResponse object or null if parsing fails
     */
    public static BidscubeResponse parse(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            String adm = AdmSanitizer.extractMarkupFromResponseBody(jsonString);
            if (adm != null) {
                adm = AdmSanitizer.sanitize(adm);
            }
            if (adm == null || adm.isEmpty()) {
                SDKLogger.e(TAG, "Failed to parse JSON response: empty adm");
                return null;
            }
            int position = AdmSanitizer.extractPositionFromResponseBody(jsonString, 0);
            return new BidscubeResponse(adm, position);
        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to parse JSON response: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse JSON object into BidscubeResponse
     * @param json JSON object
     * @return BidscubeResponse object or null if parsing fails
     */
    public static BidscubeResponse parse(JSONObject json) {
        try {
            String adm = json.optString("adm", "");
            adm = AdmSanitizer.sanitize(adm);
            int position = json.optInt("position", 0);
            
            return new BidscubeResponse(adm, position);
            
        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to parse JSON object: " + e.getMessage());
            return null;
        }
    }
}

