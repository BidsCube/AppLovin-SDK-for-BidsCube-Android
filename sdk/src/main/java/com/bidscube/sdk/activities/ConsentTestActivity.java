package com.bidscube.sdk.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.interfaces.ConsentCallback;
import com.bidscube.sdk.utils.SDKLogger;

/**
 * Test activity demonstrating consent management features
 * This shows how to properly handle GDPR and CCPA compliance
 */
public class ConsentTestActivity extends Activity {

    private static final String TAG = "ConsentTestActivity";
    private TextView statusText;
    private LinearLayout buttonContainer;
    private EditText placementIdInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        statusText = new TextView(this);
        statusText.setText("SDK Status: Not Initialized");
        statusText.setTextSize(18);
        statusText.setPadding(0, 0, 0, 32);
        layout.addView(statusText);

        Button initButton = new Button(this);
        initButton.setText("Initialize SDK");
        initButton.setOnClickListener(v -> initializeSDK());
        layout.addView(initButton);


        addTextView("Placement ID Input:", layout);
        placementIdInput = new EditText(this);
        placementIdInput.setHint("Enter placement ID:");
        placementIdInput.setText("");
        placementIdInput.setPadding(16, 16, 16, 16);
        layout.addView(placementIdInput);

        buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        layout.addView(buttonContainer);

        setContentView(layout);
    }

    private void initializeSDK() {
        try {

            SDKConfig config = new SDKConfig.Builder(this)
                    .enableLogging(true)
                    .enableDebugMode(true)
                    .defaultAdTimeout(30000)
                    .defaultAdPosition("UNKNOWN")
                    .build();

            BidscubeSDK.initialize(this, config);

            statusText.setText("SDK Status: Initializing...");

            new Thread(() -> {
                while (!BidscubeSDK.isInitialized()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                runOnUiThread(() -> {
                    statusText.setText("SDK Status: Initialized");
                    setupConsentButtons();
                });
            }).start();

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to initialize SDK: " + e.getMessage(), e);
            statusText.setText("SDK Status: Initialization Failed");
            Toast.makeText(this, "SDK initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupConsentButtons() {
        buttonContainer.removeAllViews();

        addTextView("Consent Management:");
        addButton("Request Consent Info Update", v -> requestConsentInfoUpdate());
        addButton("Show Consent Form", v -> showConsentForm());
        addButton("Check if Consent Required", v -> checkConsentRequired());
        addButton("Check Ads Consent", v -> checkAdsConsent());
        addButton("Check Analytics Consent", v -> checkAnalyticsConsent());
        addButton("Get Consent Summary", v -> getConsentSummary());
        addButton("Enable Debug Mode", v -> enableDebugMode());
        addButton("Reset Consent", v -> resetConsent());

        addTextView("");

        addTextView("Ad Testing (requires consent amn placementId):");
        addButton("Show Image Ad (if consent)", v -> showImageAdIfConsent());
        addButton("Show Video Ad (if consent)", v -> showVideoAdIfConsent());
        addButton("Show Native Ad (if consent)", v -> showNativeAdIfConsent());

        addTextView("");

        Button cleanupButton = new Button(this);
        cleanupButton.setText("Cleanup SDK");
        cleanupButton.setOnClickListener(v -> cleanupSDK());
        buttonContainer.addView(cleanupButton);
    }

    private void addTextView(String text) {
        addTextView(text, buttonContainer);
    }

    private void addTextView(String text, LinearLayout parent) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setPadding(0, 16, 0, 8);
        parent.addView(tv);
    }

    private void addButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(listener);
        buttonContainer.addView(button);
    }

    private String getPlacementId() {
        String placementId = placementIdInput.getText().toString().trim();
        return placementId.isEmpty() ? null : placementId;
    }

    private void showPlacementIdErrorDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Placement ID is required. Please enter a valid placement ID.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void requestConsentInfoUpdate() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Requesting consent info update...", Toast.LENGTH_SHORT).show();

        BidscubeSDK.requestConsentInfoUpdate(new ConsentCallback() {
            @Override
            public void onConsentInfoUpdated() {
                SDKLogger.d(TAG, "Consent info updated successfully");
                runOnUiThread(() -> {
                    Toast.makeText(ConsentTestActivity.this, "Consent info updated", Toast.LENGTH_SHORT).show();
                    updateConsentStatus();
                });
            }

            @Override
            public void onConsentInfoUpdateFailed(Exception error) {

            }

            @Override
            public void onConsentFormShown() {
                SDKLogger.d(TAG, "Consent form shown");
                runOnUiThread(() -> Toast.makeText(ConsentTestActivity.this, "Consent form shown", Toast.LENGTH_SHORT)
                        .show());
            }

            @Override
            public void onConsentFormError(Exception formError) {

            }

            @Override
            public void onConsentGranted() {
                SDKLogger.d(TAG, "Consent granted");
                runOnUiThread(() -> {
                    Toast.makeText(ConsentTestActivity.this, "Consent granted! Can show ads.", Toast.LENGTH_LONG)
                            .show();
                    updateConsentStatus();
                });
            }

            @Override
            public void onConsentDenied() {
                SDKLogger.d(TAG, "Consent denied");
                runOnUiThread(() -> {
                    Toast.makeText(ConsentTestActivity.this, "Consent denied. Cannot show ads.", Toast.LENGTH_LONG)
                            .show();
                    updateConsentStatus();
                });
            }

            @Override
            public void onConsentNotRequired() {
                SDKLogger.d(TAG, "Consent not required");
                runOnUiThread(() -> {
                    Toast.makeText(ConsentTestActivity.this, "Consent not required. Can show ads.", Toast.LENGTH_LONG)
                            .show();
                    updateConsentStatus();
                });
            }

            @Override
            public void onConsentStatusChanged(boolean hasConsent) {
                SDKLogger.d(TAG, "Consent status changed: " + hasConsent);
                runOnUiThread(() -> {
                    Toast.makeText(ConsentTestActivity.this, "Consent status: " + (hasConsent ? "Granted" : "Denied"),
                            Toast.LENGTH_SHORT).show();
                    updateConsentStatus();
                });
            }
        });
    }

    private void showConsentForm() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Showing consent form...", Toast.LENGTH_SHORT).show();

        BidscubeSDK.showConsentForm(new ConsentCallback() {
            @Override
            public void onConsentInfoUpdated() {

            }

            @Override
            public void onConsentInfoUpdateFailed(Exception error) {

            }

            @Override
            public void onConsentFormShown() {
                SDKLogger.d(TAG, "Consent form displayed");
                runOnUiThread(() -> Toast
                        .makeText(ConsentTestActivity.this, "Consent form displayed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onConsentFormError(Exception formError) {

            }

            @Override
            public void onConsentGranted() {
                SDKLogger.d(TAG, "Consent granted through form");
                runOnUiThread(() -> {
                    Toast.makeText(ConsentTestActivity.this, "Consent granted through form!", Toast.LENGTH_LONG).show();
                    updateConsentStatus();
                });
            }

            @Override
            public void onConsentDenied() {
                SDKLogger.d(TAG, "Consent denied through form");
                runOnUiThread(() -> {
                    Toast.makeText(ConsentTestActivity.this, "Consent denied through form.", Toast.LENGTH_LONG).show();
                    updateConsentStatus();
                });
            }

            @Override
            public void onConsentNotRequired() {
                SDKLogger.d(TAG, "Consent not required");
                runOnUiThread(() -> {
                    Toast.makeText(ConsentTestActivity.this, "Consent not required", Toast.LENGTH_SHORT).show();
                    updateConsentStatus();
                });
            }

            @Override
            public void onConsentStatusChanged(boolean hasConsent) {
                SDKLogger.d(TAG, "Consent status changed: " + hasConsent);
                runOnUiThread(() -> {
                    Toast.makeText(ConsentTestActivity.this,
                            "Consent status changed: " + (hasConsent ? "Granted" : "Denied"), Toast.LENGTH_SHORT)
                            .show();
                    updateConsentStatus();
                });
            }
        });
    }

    private void checkConsentRequired() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isRequired = BidscubeSDK.isConsentRequired();
        String message = "Consent required: " + isRequired;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        SDKLogger.d(TAG, message);
    }

    private void checkAdsConsent() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasConsent = BidscubeSDK.hasAdsConsent();
        String message = "Ads consent: " + hasConsent;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        SDKLogger.d(TAG, message);
    }

    private void checkAnalyticsConsent() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasConsent = BidscubeSDK.hasAnalyticsConsent();
        String message = "Analytics consent: " + hasConsent;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        SDKLogger.d(TAG, message);
    }

    private void getConsentSummary() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        String summary = BidscubeSDK.getConsentStatusSummary();
        SDKLogger.d(TAG, "Consent Summary:\n" + summary);

        String shortSummary = summary.length() > 100 ? summary.substring(0, 100) + "..." : summary;
        Toast.makeText(this, "Consent Summary (see logs): " + shortSummary, Toast.LENGTH_LONG).show();
    }

    private void enableDebugMode() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        BidscubeSDK.enableConsentDebugMode("test_device_123");
        Toast.makeText(this, "Debug mode enabled for test device", Toast.LENGTH_SHORT).show();
        SDKLogger.d(TAG, "Consent debug mode enabled");
    }

    private void resetConsent() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        BidscubeSDK.resetConsent();
        Toast.makeText(this, "Consent information reset", Toast.LENGTH_SHORT).show();
        SDKLogger.d(TAG, "Consent information reset");

        updateConsentStatus();
    }

    private void showImageAdIfConsent() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!BidscubeSDK.hasAdsConsent()) {
            Toast.makeText(this, "No ads consent. Request consent first.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Showing image ad...", Toast.LENGTH_SHORT).show();

        AdCallback callback = new AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
                SDKLogger.d(TAG, "Image ad loading: " + placementId);
            }

            @Override
            public void onAdLoaded(String placementId) {
                SDKLogger.d(TAG, "Image ad loaded: " + placementId);
                runOnUiThread(() -> Toast
                        .makeText(ConsentTestActivity.this, "Image ad loaded successfully", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onAdDisplayed(String placementId) {
                SDKLogger.d(TAG, "Image ad displayed: " + placementId);
            }

            @Override
            public void onAdClicked(String placementId) {
                SDKLogger.d(TAG, "Image ad clicked: " + placementId);
            }

            @Override
            public void onAdClosed(String placementId) {
                SDKLogger.d(TAG, "Image ad closed: " + placementId);
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                SDKLogger.e(TAG, "Image ad failed: " + placementId + " - " + errorMessage);
                runOnUiThread(() -> Toast
                        .makeText(ConsentTestActivity.this, "Image ad failed: " + errorMessage, Toast.LENGTH_LONG)
                        .show());
            }
        };

        BidscubeSDK.showImageAd("20212", callback);
    }

    private void showVideoAdIfConsent() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!BidscubeSDK.hasAdsConsent()) {
            Toast.makeText(this, "No ads consent. Request consent first.", Toast.LENGTH_LONG).show();
            return;
        }

        String placementId = getPlacementId();
        if (placementId == null || placementId.trim().isEmpty()) {
            showPlacementIdErrorDialog();
            return;
        }

        Toast.makeText(this, "Showing video ad...", Toast.LENGTH_SHORT).show();

        AdCallback callback = new AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
                SDKLogger.d(TAG, "Video ad loading: " + placementId);
            }

            @Override
            public void onAdLoaded(String placementId) {
                SDKLogger.d(TAG, "Video ad loaded: " + placementId);
                runOnUiThread(() -> Toast
                        .makeText(ConsentTestActivity.this, "Video ad loaded successfully", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onAdDisplayed(String placementId) {
                SDKLogger.d(TAG, "Video ad displayed: " + placementId);
            }

            @Override
            public void onAdClicked(String placementId) {
                SDKLogger.d(TAG, "Video ad clicked: " + placementId);
            }

            @Override
            public void onAdClosed(String placementId) {
                SDKLogger.d(TAG, "Video ad closed: " + placementId);
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                SDKLogger.e(TAG, "Video ad failed: " + placementId + " - " + errorMessage);
                runOnUiThread(() -> Toast
                        .makeText(ConsentTestActivity.this, "Video ad failed: " + errorMessage, Toast.LENGTH_LONG)
                        .show());
            }
        };

        BidscubeSDK.showVideoAd(placementId, callback);
    }

    private void showNativeAdIfConsent() {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!BidscubeSDK.hasAdsConsent()) {
            Toast.makeText(this, "No ads consent. Request consent first.", Toast.LENGTH_LONG).show();
            return;
        }

        String placementId = getPlacementId();
        if (placementId == null || placementId.trim().isEmpty()) {
            showPlacementIdErrorDialog();
            return;
        }

        Toast.makeText(this, "Showing native ad...", Toast.LENGTH_SHORT).show();

        AdCallback callback = new AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
                SDKLogger.d(TAG, "Native ad loading: " + placementId);
            }

            @Override
            public void onAdLoaded(String placementId) {
                SDKLogger.d(TAG, "Native ad loaded: " + placementId);
                runOnUiThread(() -> Toast
                        .makeText(ConsentTestActivity.this, "Native ad loaded successfully", Toast.LENGTH_SHORT)
                        .show());
            }

            @Override
            public void onAdDisplayed(String placementId) {
                SDKLogger.d(TAG, "Native ad displayed: " + placementId);
            }

            @Override
            public void onAdClicked(String placementId) {
                SDKLogger.d(TAG, "Native ad clicked: " + placementId);
            }

            @Override
            public void onAdClosed(String placementId) {
                SDKLogger.d(TAG, "Native ad closed: " + placementId);
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                SDKLogger.e(TAG, "Native ad failed: " + placementId + " - " + errorMessage);
                runOnUiThread(() -> Toast
                        .makeText(ConsentTestActivity.this, "Native ad failed: " + errorMessage, Toast.LENGTH_LONG)
                        .show());
            }
        };

        BidscubeSDK.showNativeAd(placementId, callback);
    }

    private void updateConsentStatus() {
        if (!BidscubeSDK.isInitialized()) {
            return;
        }

        StringBuilder status = new StringBuilder();
        status.append("SDK Status: Initialized\n");
        status.append("Consent Required: ").append(BidscubeSDK.isConsentRequired()).append("\n");
        status.append("Ads Consent: ").append(BidscubeSDK.hasAdsConsent()).append("\n");
        status.append("Analytics Consent: ").append(BidscubeSDK.hasAnalyticsConsent());

        statusText.setText(status.toString());
    }

    private void cleanupSDK() {
        if (BidscubeSDK.isInitialized()) {
            BidscubeSDK.cleanup();
            statusText.setText("SDK Status: Cleaned Up");
            buttonContainer.removeAllViews();
            Toast.makeText(this, "SDK cleaned up", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BidscubeSDK.isInitialized()) {
            BidscubeSDK.cleanup();
        }
    }
}
