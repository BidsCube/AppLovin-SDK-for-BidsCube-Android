package com.bidscube.sdk.network;

import android.util.Log;

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
        try {
            JSONObject json = new JSONObject(jsonString);
            String adm = json.optString("adm", "");
            int position = json.optInt("position", 0);
            
            return new BidscubeResponse(adm, position);
            
        } catch (JSONException e) {
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
            int position = json.optInt("position", 0);
            
            return new BidscubeResponse(adm, position);
            
        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to parse JSON object: " + e.getMessage());
            return null;
        }
    }
}

