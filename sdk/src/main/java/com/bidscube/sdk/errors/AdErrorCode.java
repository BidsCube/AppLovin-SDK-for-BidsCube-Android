package com.bidscube.sdk.errors;

/**
 * Stable error codes returned via {@link com.bidscube.sdk.interfaces.AdCallback#onAdFailed}.
 * Messages are English and intended for logs / mediation adapters.
 */
public final class AdErrorCode {

    /** Unknown or unclassified failure. */
    public static final int UNKNOWN = -1;

    /** SSP returned HTTP 204 — no ad available for this request. */
    public static final int NO_FILL = 204;

    /** Non-2xx HTTP response from the ad server. */
    public static final int HTTP_ERROR = 1001;

    /** Response body could not be parsed into a Bidscube ad payload. */
    public static final int INVALID_RESPONSE = 1002;

    /** Ad markup (ADM) was empty after a successful HTTP response. */
    public static final int EMPTY_ADM = 1003;

    /** An Activity context is required but only Application context is available. */
    public static final int NO_ACTIVITY_CONTEXT = 1004;

    /** Network I/O failure (timeout, connection error, etc.). */
    public static final int NETWORK_ERROR = 1005;

    /** Unexpected error while building or displaying the ad UI. */
    public static final int DISPLAY_ERROR = 1006;

    private AdErrorCode() {
    }

    public static int fromException(Throwable error) {
        if (error instanceof BidscubeRequestException) {
            return ((BidscubeRequestException) error).getErrorCode();
        }
        if (error == null || error.getMessage() == null) {
            return UNKNOWN;
        }
        String message = error.getMessage();
        if (message.contains("HTTP error: 204")) {
            return NO_FILL;
        }
        if (message.startsWith("HTTP error:")) {
            return HTTP_ERROR;
        }
        if (message.contains("Failed to parse response")) {
            return INVALID_RESPONSE;
        }
        if (message.contains("Activity context")) {
            return NO_ACTIVITY_CONTEXT;
        }
        return UNKNOWN;
    }

    public static String messageFor(Throwable error) {
        if (error instanceof BidscubeRequestException) {
            return error.getMessage();
        }
        if (error == null || error.getMessage() == null || error.getMessage().isEmpty()) {
            return "Unknown ad request error";
        }
        return error.getMessage();
    }

    public static String describe(int errorCode) {
        switch (errorCode) {
            case NO_FILL:
                return "No ad fill (HTTP 204)";
            case HTTP_ERROR:
                return "Ad server HTTP error";
            case INVALID_RESPONSE:
                return "Invalid ad server response";
            case EMPTY_ADM:
                return "Empty ad markup";
            case NO_ACTIVITY_CONTEXT:
                return "Activity context required";
            case NETWORK_ERROR:
                return "Network error";
            case DISPLAY_ERROR:
                return "Ad display error";
            case UNKNOWN:
            default:
                return "Unknown error";
        }
    }
}
