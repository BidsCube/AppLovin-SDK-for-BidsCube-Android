package com.bidscube.sdk.network;

/**
 * Unified callback interface for Bidscube ad requests
 * Supports both raw response body and parsed BidscubeResponse
 */
public interface BidscubeCallback {
    
    /**
     * Called when the request is successful with raw response
     * @param responseCode HTTP response code
     * @param responseBody Raw response body string
     */


    
    /**
     * Called when the request is successful with parsed response
     * @param responseCode HTTP response code
     * @param response Parsed BidscubeResponse object
     */
    default void onSuccess(int responseCode, BidscubeResponse response) {


    }
    
    /**
     * Called when the request fails
     * @param e Exception that occurred
     */
    void onFail(Exception e);
}
