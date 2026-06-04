package com.bidscube.sdk.errors;

/**
 * Structured failure for ad HTTP / parsing paths.
 */
public class BidscubeRequestException extends Exception {

    private final int errorCode;
    private final int httpStatus;

    public BidscubeRequestException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 0;
    }

    public BidscubeRequestException(int errorCode, int httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
