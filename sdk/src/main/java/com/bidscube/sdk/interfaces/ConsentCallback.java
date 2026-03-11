package com.bidscube.sdk.interfaces;



/**
 * Callback interface for consent management events
 * Implement this interface to handle consent-related events in your application
 */
public interface ConsentCallback {

    /**
     * Called when consent information is successfully updated
     */
    void onConsentInfoUpdated();

    /**
     * Called when consent information update fails
     * @param error The error that occurred during consent info update
     */
    void onConsentInfoUpdateFailed(Exception error);

    /**
     * Called when consent form is successfully shown
     */
    void onConsentFormShown();

    /**
     * Called when consent form fails to show
     * @param formError The error that occurred with the consent form
     */
    void onConsentFormError(Exception formError);

    /**
     * Called when user grants consent
     */
    void onConsentGranted();

    /**
     * Called when user denies consent
     */
    void onConsentDenied();

    /**
     * Called when consent is not required for the current user
     */
    void onConsentNotRequired();

    /**
     * Called when consent status changes
     * @param hasConsent true if user has given consent, false otherwise
     */
    void onConsentStatusChanged(boolean hasConsent);
}
