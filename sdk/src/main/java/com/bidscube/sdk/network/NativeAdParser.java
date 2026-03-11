package com.bidscube.sdk.network;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bidscube.sdk.models.natives.NativeAd;
import com.bidscube.sdk.models.natives.NativeAsset;
import com.bidscube.sdk.models.natives.NativeLink;
import com.bidscube.sdk.models.natives.Title;
import com.bidscube.sdk.models.natives.Image;
import com.bidscube.sdk.models.natives.NativeData;
import com.bidscube.sdk.models.natives.Video;
import com.bidscube.sdk.models.natives.EventTracker;
import com.bidscube.sdk.utils.SDKLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for native ad JSON responses from Bidscube
 * Handles the adm field which contains the actual native ad data
 */
public class NativeAdParser {
    
    private static final String TAG = "NativeAdParser";
    
    /**
     * Parse native ad from BidscubeResponse adm field
     * @param adm The adm field content (JSON string)
     * @return NativeAd object or null if parsing fails
     */
    public static NativeAd parseFromAdm(String adm) {
        if (adm == null || adm.trim().isEmpty()) {
            SDKLogger.e(TAG, "ADM field is null or empty");
            return null;
        }
        
        SDKLogger.d(TAG, "Parsing ADM field: " + adm.substring(0, Math.min(100, adm.length())) + "...");
        
        try {

            JSONObject admJson = new JSONObject(adm);
            SDKLogger.d(TAG, "Successfully parsed ADM JSON");

            if (!admJson.has("native")) {
                SDKLogger.e(TAG, "ADM does not contain native ad data");
                return null;
            }
            
            JSONObject nativeJson = admJson.getJSONObject("native");
            SDKLogger.d(TAG, "Found native object with keys: " + getJsonKeys(nativeJson));

            NativeAd nativeAd = new NativeAd();

            if (nativeJson.has("ver")) {
                nativeAd.ver = nativeJson.getString("ver");
                SDKLogger.d(TAG, "Native ad version: " + nativeAd.ver);
            }

            if (nativeJson.has("assets")) {
                nativeAd.assets = parseAssets(nativeJson.getJSONArray("assets"));
                SDKLogger.d(TAG, "Parsed " + (nativeAd.assets != null ? nativeAd.assets.size() : 0) + " assets");
            }

            if (nativeJson.has("link")) {
                nativeAd.link = parseLink(nativeJson.getJSONObject("link"));
                SDKLogger.d(TAG, "Parsed link: " + (nativeAd.link != null ? nativeAd.link.url : "null"));
            }

            if (nativeJson.has("imptrackers")) {
                nativeAd.imptrackers = parseStringArray(nativeJson.getJSONArray("imptrackers"));
                SDKLogger.d(TAG, "Parsed " + (nativeAd.imptrackers != null ? nativeAd.imptrackers.size() : 0) + " impression trackers");
            }

            if (nativeJson.has("eventtrackers")) {
                nativeAd.eventtrackers = parseEventTrackers(nativeJson.getJSONArray("eventtrackers"));
                SDKLogger.d(TAG, "Parsed " + (nativeAd.eventtrackers != null ? nativeAd.eventtrackers.size() : 0) + " event trackers");
            }
            
            SDKLogger.d(TAG, "Successfully parsed native ad with " + 
                  (nativeAd.assets != null ? nativeAd.assets.size() : 0) + " assets");
            
            return nativeAd;
            
        } catch (JSONException e) {
            SDKLogger.e(TAG, "Failed to parse native ad from ADM: " + e.getMessage());
            return null;
        } catch (Exception e) {
            SDKLogger.e(TAG, "Unexpected error parsing native ad: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse assets array
     */
    private static List<NativeAsset> parseAssets(JSONArray assetsArray) throws JSONException {
        List<NativeAsset> assets = new ArrayList<>();
        
        SDKLogger.d(TAG, "Parsing " + assetsArray.length() + " assets");
        
        for (int i = 0; i < assetsArray.length(); i++) {
            JSONObject assetJson = assetsArray.getJSONObject(i);
            NativeAsset asset = new NativeAsset();
            
            SDKLogger.d(TAG, "Parsing asset " + i + " with keys: " + getJsonKeys(assetJson));

            if (assetJson.has("id")) {
                asset.id = assetJson.getInt("id");
                SDKLogger.d(TAG, "Asset " + i + " ID: " + asset.id);
            }

            if (assetJson.has("required")) {
                asset.required = assetJson.getBoolean("required");
                SDKLogger.d(TAG, "Asset " + i + " required: " + asset.required);
            }

            if (assetJson.has("title")) {
                asset.title = parseTitle(assetJson.getJSONObject("title"));
                SDKLogger.d(TAG, "Asset " + i + " title: " + (asset.title != null ? asset.title.text : "null"));
            }

            if (assetJson.has("img")) {
                asset.img = parseImage(assetJson.getJSONObject("img"));
                SDKLogger.d(TAG, "Asset " + i + " image: " + (asset.img != null ? asset.img.url : "null"));
            }

            if (assetJson.has("data")) {
                asset.data = parseData(assetJson.getJSONObject("data"));
                SDKLogger.d(TAG, "Asset " + i + " data: " + (asset.data != null ? asset.data.value : "null"));
            }

            if (assetJson.has("video")) {
                asset.video = parseVideo(assetJson.getJSONObject("video"));
                SDKLogger.d(TAG, "Asset " + i + " video: " + (asset.video != null ? asset.video.vasttag : "null"));
            }

            if (assetJson.has("link")) {
                asset.link = parseLink(assetJson.getJSONObject("link"));
                SDKLogger.d(TAG, "Asset " + i + " link: " + (asset.link != null ? asset.link.url : "null"));
            }
            
            assets.add(asset);
        }
        
        SDKLogger.d(TAG, "Successfully parsed " + assets.size() + " assets");
        return assets;
    }
    
    /**
     * Parse title object
     */
    private static Title parseTitle(JSONObject titleJson) throws JSONException {
        Title title = new Title();
        
        if (titleJson.has("text")) {
            title.text = titleJson.getString("text");
        }
        
        if (titleJson.has("len")) {
            title.len = titleJson.getInt("len");
        }
        
        return title;
    }
    
    /**
     * Parse image object
     */
    private static Image parseImage(JSONObject imageJson) throws JSONException {
        Image image = new Image();
        
        if (imageJson.has("url")) {
            image.url = imageJson.getString("url");
        }
        
        if (imageJson.has("w")) {
            image.w = imageJson.getInt("w");
        }
        
        if (imageJson.has("h")) {
            image.h = imageJson.getInt("h");
        }
        
        if (imageJson.has("type")) {
            try {
                image.type = com.bidscube.sdk.models.enums.ImageType.from(imageJson.getInt("type"));
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse image type: " + e.getMessage());
            }
        }
        
        return image;
    }
    
    /**
     * Parse data object
     */
    private static NativeData parseData(JSONObject dataJson) throws JSONException {
        NativeData data = new NativeData();
        
        if (dataJson.has("type")) {
            try {
                data.type = com.bidscube.sdk.models.enums.NativeDataType.from(dataJson.getInt("type"));
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse data type: " + e.getMessage());
            }
        }
        
        if (dataJson.has("len")) {
            data.len = dataJson.getInt("len");
        }
        
        if (dataJson.has("value")) {
            data.value = dataJson.getString("value");
        }
        
        return data;
    }
    
    /**
     * Parse video object
     */
    private static Video parseVideo(JSONObject videoJson) throws JSONException {
        Video video = new Video();
        
        if (videoJson.has("vasttag")) {
            video.vasttag = videoJson.getString("vasttag");
        }
        
        return video;
    }
    
    /**
     * Parse link object
     */
    private static NativeLink parseLink(JSONObject linkJson) throws JSONException {
        NativeLink link = new NativeLink();
        
        if (linkJson.has("url")) {
            link.url = linkJson.getString("url");
        }
        
        if (linkJson.has("fallback")) {
            link.fallback = linkJson.getString("fallback");
        }
        
        if (linkJson.has("clicktrackers")) {
            link.clicktrackers.addAll(parseStringArray(linkJson.getJSONArray("clicktrackers")));
        }
        
        return link;
    }
    
    /**
     * Parse event trackers array
     */
    private static List<EventTracker> parseEventTrackers(JSONArray eventTrackersArray) throws JSONException {
        List<EventTracker> eventTrackers = new ArrayList<>();
        
        for (int i = 0; i < eventTrackersArray.length(); i++) {
            JSONObject eventTrackerJson = eventTrackersArray.getJSONObject(i);
            EventTracker eventTracker = new EventTracker();
            
            if (eventTrackerJson.has("event")) {
                eventTracker.setEvent(eventTrackerJson.getInt("event"));
            }
            
            if (eventTrackerJson.has("method")) {
                eventTracker.setMethod(eventTrackerJson.getInt("method"));
            }
            
            if (eventTrackerJson.has("url")) {
                eventTracker.setUrl(parseStringArray(eventTrackerJson.getJSONArray("url")));
            }
            
            eventTrackers.add(eventTracker);
        }
        
        return eventTrackers;
    }
    
    /**
     * Parse string array
     */
    private static List<String> parseStringArray(JSONArray array) throws JSONException {
        List<String> strings = new ArrayList<>();
        
        for (int i = 0; i < array.length(); i++) {
            strings.add(array.getString(i));
        }
        
        return strings;
    }
    
    /**
     * Helper method to get JSON object keys for debugging
     */
    private static String getJsonKeys(JSONObject json) {
        try {
            JSONArray names = json.names();
            if (names == null) return "[]";
            
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < names.length(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(names.getString(i));
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "error getting keys";
        }
    }
}
