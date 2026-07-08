package com.bidscube.sdk.interfaces;

/**
 * Callback for asynchronous SDK initialization completion.
 */
public interface InitializationCallback {

    void onInitialized();

    void onInitializationFailed(String message);
}
