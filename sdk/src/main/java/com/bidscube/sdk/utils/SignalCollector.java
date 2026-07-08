package com.bidscube.sdk.utils;

import org.json.JSONObject;

/**
 * Builds structured mediation signals without PII or raw IFA.
 */
public final class SignalCollector {

    private static final String ADAPTER_NAME = "applovin-max-android";

    private SignalCollector() {
    }

    /**
     * @param sdkVersion     Bidscube SDK artifact version
     * @param adapterVersion mediation adapter version, or empty when unknown
     */
    public static String collectSignal(String sdkVersion, String adapterVersion) {
        try {
            JSONObject signal = new JSONObject();
            signal.put("sdk_version", sdkVersion != null ? sdkVersion : "");
            signal.put("adapter_version", adapterVersion != null ? adapterVersion : "");
            signal.put("adapter", ADAPTER_NAME);
            signal.put("openrtb_2_6_response_parsing", false);
            signal.put("pod_bidding", false);
            return signal.toString();
        } catch (Exception e) {
            SDKLogger.e("SignalCollector", "Failed to build signal JSON: " + e.getMessage(), e);
            return emptyStructuredSignal();
        }
    }

    public static String emptyStructuredSignal() {
        return "{\"sdk_version\":\"\",\"adapter_version\":\"\",\"adapter\":\"applovin-max-android\","
                + "\"openrtb_2_6_response_parsing\":false,\"pod_bidding\":false}";
    }
}
