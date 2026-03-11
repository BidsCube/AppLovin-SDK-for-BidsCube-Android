package com.bidscube.sdk.utils;

import android.util.Log;

/**
 * SDK Logger utility that respects the enableLogging flag from SDKConfig
 * This ensures that when enableLogging is false, no log messages are shown
 */
public class SDKLogger {

    private static boolean isLoggingEnabled = true; // Default to true for backward compatibility
    private static String defaultTag = "BidscubeSDK";

    /**
     * Set the logging enabled state
     * 
     * @param enabled true to enable logging, false to disable
     */
    public static void setLoggingEnabled(boolean enabled) {
        isLoggingEnabled = enabled;
    }

    /**
     * Set the default tag for logging
     * 
     * @param tag Default tag to use for log messages
     */
    public static void setDefaultTag(String tag) {
        defaultTag = tag;
    }

    /**
     * Check if logging is enabled
     * 
     * @return true if logging is enabled, false otherwise
     */
    public static boolean isLoggingEnabled() {
        return isLoggingEnabled;
    }

    /**
     * Log a debug message
     * 
     * @param tag     Tag for the log message
     * @param message Message to log
     */
    public static void d(String tag, String message) {
        if (isLoggingEnabled) {
            Log.d(tag, message);
        }
    }

    /**
     * Log a debug message with default tag
     * 
     * @param message Message to log
     */
    public static void d(String message) {
        d(defaultTag, message);
    }

    /**
     * Log an info message
     * 
     * @param tag     Tag for the log message
     * @param message Message to log
     */
    public static void i(String tag, String message) {
        if (isLoggingEnabled) {
            Log.i(tag, message);
        }
    }

    /**
     * Log an info message with default tag
     * 
     * @param message Message to log
     */
    public static void i(String message) {
        i(defaultTag, message);
    }

    /**
     * Log a warning message
     * 
     * @param tag     Tag for the log message
     * @param message Message to log
     */
    public static void w(String tag, String message) {
        if (isLoggingEnabled) {
            Log.w(tag, message);
        }
    }

    /**
     * Log a warning message with default tag
     * 
     * @param message Message to log
     */
    public static void w(String message) {
        w(defaultTag, message);
    }

    /**
     * Log an error message
     * 
     * @param tag     Tag for the log message
     * @param message Message to log
     */
    public static void e(String tag, String message) {
        if (isLoggingEnabled) {
            Log.e(tag, message);
        }
    }

    /**
     * Log an error message with default tag
     * 
     * @param message Message to log
     */
    public static void e(String message) {
        e(defaultTag, message);
    }

    /**
     * Log an error message with throwable
     * 
     * @param tag       Tag for the log message
     * @param message   Message to log
     * @param throwable Throwable to log
     */
    public static void e(String tag, String message, Throwable throwable) {
        if (isLoggingEnabled) {
            Log.e(tag, message, throwable);
        }
    }

    /**
     * Log an error message with throwable and default tag
     * 
     * @param message   Message to log
     * @param throwable Throwable to log
     */
    public static void e(String message, Throwable throwable) {
        e(defaultTag, message, throwable);
    }

    /**
     * Log a verbose message
     * 
     * @param tag     Tag for the log message
     * @param message Message to log
     */
    public static void v(String tag, String message) {
        if (isLoggingEnabled) {
            Log.v(tag, message);
        }
    }

    /**
     * Log a verbose message with default tag
     * 
     * @param message Message to log
     */
    public static void v(String message) {
        v(defaultTag, message);
    }

    /**
     * Log a warning message with throwable
     * 
     * @param tag       Tag for the log message
     * @param message   Message to log
     * @param throwable Throwable to log
     */
    public static void w(String tag, String message, Throwable throwable) {
        if (isLoggingEnabled) {
            Log.w(tag, message, throwable);
        }
    }

    /**
     * Log a warning message with throwable and default tag
     * 
     * @param message   Message to log
     * @param throwable Throwable to log
     */
    public static void w(String message, Throwable throwable) {
        w(defaultTag, message, throwable);
    }
}
