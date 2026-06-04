package com.bidscube.sdk.httpProvider;

import android.util.Log;

import com.bidscube.sdk.errors.AdErrorCode;
import com.bidscube.sdk.errors.BidscubeRequestException;
import com.bidscube.sdk.network.BidscubeCallback;
import com.bidscube.sdk.utils.SDKLogger;
import com.bidscube.sdk.network.BidscubeResponse;
import com.bidscube.sdk.network.BidscubeResponseParser;

import java.net.HttpURLConnection;
import java.net.URL;

public class HttpProvider {

    /**
     * Send HTTP request with raw response callback
     */
    public static void sendGetRequest(String urlString, BidscubeCallback callback) {
        logFormattedUrl("Sending GET request to:", urlString);

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                SDKLogger.d("HttpProvider", "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    SDKLogger.w("HttpProvider", "No ad fill (HTTP 204) for URL: " + urlString);
                    callback.onFail(new BidscubeRequestException(
                            AdErrorCode.NO_FILL,
                            204,
                            "No ad fill: ad server returned HTTP 204 (No Content)"));
                    return;
                }

                String responseBody = "";
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    java.io.InputStream is = connection.getInputStream();
                    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                    responseBody = s.hasNext() ? s.next() : "";
                    is.close();
                    SDKLogger.d("HttpProvider", "Response body length: " + responseBody.length());
                    SDKLogger.v("HttpProvider", "Response body: " + responseBody);
                } else {
                    java.io.InputStream is = connection.getErrorStream();
                    if (is != null) {
                        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                        responseBody = s.hasNext() ? s.next() : "";
                        is.close();
                        SDKLogger.e("HttpProvider", "Error response: " + responseBody);
                    }
                    callback.onFail(new BidscubeRequestException(
                            AdErrorCode.HTTP_ERROR,
                            responseCode,
                            "HTTP error: " + responseCode
                                    + (responseBody.isEmpty() ? "" : " — " + truncate(responseBody, 200))));
                    return;
                }

                BidscubeResponse response = BidscubeResponseParser.parse(responseBody);
                if (response != null) {
                    SDKLogger.d("HttpProvider", "Successfully parsed response");
                    callback.onSuccess(responseCode, response);
                } else {
                    SDKLogger.e("HttpProvider", "Failed to parse response body");
                    callback.onFail(new BidscubeRequestException(
                            AdErrorCode.INVALID_RESPONSE,
                            "Failed to parse ad server response"));
                }

            } catch (Exception e) {
                SDKLogger.e("HttpProvider", "Request failed: " + e.getMessage(), e);
                if (e instanceof BidscubeRequestException) {
                    callback.onFail(e);
                } else {
                    callback.onFail(new BidscubeRequestException(
                            AdErrorCode.NETWORK_ERROR,
                            "Network error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * Log URL in a formatted, readable way
     */
    private static void logFormattedUrl(String prefix, String urlString) {
        try {
            URL url = new URL(urlString);
            String query = url.getQuery();

            if (query == null || query.isEmpty()) {
                SDKLogger.v("HttpProvider", prefix + " " + urlString);
                return;
            }

            String[] params = query.split("&");

            StringBuilder formattedUrl = new StringBuilder();
            formattedUrl.append(prefix).append("\n");
            formattedUrl.append("Base URL: ").append(url.getProtocol()).append("://").append(url.getHost())
                    .append(url.getPath()).append("\n");
            formattedUrl.append("Parameters:");

            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];

                    try {
                        value = java.net.URLDecoder.decode(value, "UTF-8");
                    } catch (Exception e) {
                        // ignore decode errors for logging
                    }

                    formattedUrl.append("\n  ").append(key).append(" = ").append(value);
                }
            }

            SDKLogger.v("HttpProvider", formattedUrl.toString());

        } catch (Exception e) {
            SDKLogger.v("HttpProvider", prefix + " " + urlString);
        }
    }
}
