package com.bidscube.sdk.activities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bidscube.sdk.models.enums.AdPosition;
import com.bidscube.sdk.utils.WindowedAdTest;
import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.interfaces.ConsentCallback;
import com.bidscube.sdk.utils.SDKLogger;

/**
 * Test activity for windowed ad positioning and layout testing
 * Now includes all available AdPosition values with proper padding
 */
public class WindowedAdTestActivity extends Activity {

    private static final String TAG = "WindowedAdTestActivity";

    private View testLayout;
    private View currentAdView;
    private View imageAdView;
    private View videoAdView;
    private View nativeAdView;

    private static final String IMAGE_AD_PLACEMENT_ID = "20212";
    private static final String VIDEO_AD_PLACEMENT_ID = "20213";
    private static final String NATIVE_AD_PLACEMENT_ID = "20214";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeBidscubeSDK();

        testLayout = WindowedAdTest.createWindowedAdTestLayout(this);

        addControlButtons();

        validateLayout();

        setContentView(testLayout);

        WindowedAdTest.testScrollBehavior(testLayout);
    }

    private void initializeBidscubeSDK() {
        try {
            // Initialize Bidscube Config with optional initialization of values(GDPR, CPA, COPPA)
            SDKConfig config = new SDKConfig.Builder(this)
                    .enableLogging(true)
                    .enableDebugMode(true)
                    .defaultAdPosition("UNKNOWN")
                    .build();

            BidscubeSDK.initialize(this, config);
            SDKLogger.d(TAG, "Bidscube SDK initialized successfully");

            BidscubeSDK.requestConsentInfoUpdate(new ConsentCallback() {
                @Override
                public void onConsentInfoUpdated() {
                    SDKLogger.d(TAG, "Consent info updated successfully");
                }

                @Override
                public void onConsentInfoUpdateFailed(Exception error) {
                    SDKLogger.w(TAG, "Consent info update failed: " + error.getMessage());
                }

                @Override
                public void onConsentFormShown() {
                    SDKLogger.d(TAG, "Consent form shown");
                }

                @Override
                public void onConsentFormError(Exception error) {
                    SDKLogger.w(TAG, "Consent form error: " + error.getMessage());
                }

                @Override
                public void onConsentGranted() {
                    SDKLogger.d(TAG, "Consent granted");
                }

                @Override
                public void onConsentDenied() {
                    SDKLogger.d(TAG, "Consent denied");
                }

                @Override
                public void onConsentNotRequired() {
                    SDKLogger.d(TAG, "Consent not required");
                }

                @Override
                public void onConsentStatusChanged(boolean hasConsent) {
                    SDKLogger.d(TAG, "Consent status changed: " + hasConsent);
                }
            });

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to initialize Bidscube SDK: " + e.getMessage());
            Toast.makeText(this, "SDK initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addControlButtons() {
        if (testLayout instanceof FrameLayout) {
            FrameLayout frameLayout = (FrameLayout) testLayout;

            LinearLayout controlPanel = new LinearLayout(this);
            FrameLayout.LayoutParams controlParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            controlParams.gravity = Gravity.TOP | Gravity.RIGHT;
            controlParams.topMargin = 100;
            controlParams.rightMargin = 16;
            controlPanel.setLayoutParams(controlParams);
            controlPanel.setOrientation(LinearLayout.VERTICAL);
            controlPanel.setBackgroundColor(Color.parseColor("#FF9800"));
            controlPanel.setPadding(16, 16, 16, 16);

            TextView controlTitle = new TextView(this);
            controlTitle.setText("AD POSITIONING");
            controlTitle.setTextColor(Color.WHITE);
            controlTitle.setTextSize(16);
            controlTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            controlTitle.setGravity(Gravity.CENTER);
            controlTitle.setPadding(0, 0, 0, 16);
            controlPanel.addView(controlTitle);

            AdPosition[] positions = AdPosition.values();
            for (AdPosition position : positions) {
                Button button = new Button(this);
                button.setText("Position: " + position.getDisplayName());
                button.setLayoutParams(new LinearLayout.LayoutParams(
                        300,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                button.setPadding(16, 8, 16, 8);

                final AdPosition pos = position;
                button.setOnClickListener(v -> testAdPositioning(pos));

                controlPanel.addView(button);
            }

            TextView separator = new TextView(this);
            separator.setText("---");
            separator.setTextColor(Color.WHITE);
            separator.setGravity(Gravity.CENTER);
            separator.setPadding(0, 8, 0, 8);
            controlPanel.addView(separator);

            Button imageAdButton = new Button(this);
            imageAdButton.setText("Create Image Ad");
            imageAdButton.setLayoutParams(new LinearLayout.LayoutParams(
                    200,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            imageAdButton.setPadding(16, 8, 16, 8);
            imageAdButton.setOnClickListener(v -> createImageAd());

            controlPanel.addView(imageAdButton);

            Button videoAdButton = new Button(this);
            videoAdButton.setText("Create Video Ad");
            videoAdButton.setLayoutParams(new LinearLayout.LayoutParams(
                    200,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            videoAdButton.setPadding(16, 8, 16, 8);
            videoAdButton.setOnClickListener(v -> createVideoAd());

            controlPanel.addView(videoAdButton);

            Button nativeAdButton = new Button(this);
            nativeAdButton.setText("Create Native Ad");
            nativeAdButton.setLayoutParams(new LinearLayout.LayoutParams(
                    200,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            nativeAdButton.setPadding(16, 8, 16, 8);
            nativeAdButton.setOnClickListener(v -> createNativeAd());

            controlPanel.addView(nativeAdButton);

            Button validateButton = new Button(this);
            validateButton.setText("Validate Layout");
            validateButton.setLayoutParams(new LinearLayout.LayoutParams(
                    200,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            validateButton.setPadding(16, 8, 16, 8);
            validateButton.setOnClickListener(v -> validateLayout());

            controlPanel.addView(validateButton);

            frameLayout.addView(controlPanel);

            SDKLogger.d(TAG, "Control buttons added to test layout with all AdPosition values");
        }
    }

    private void createTestAd() {
        SDKLogger.d(TAG, "Creating test ad view...");

        LinearLayout adView = new LinearLayout(this);
        adView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        adView.setOrientation(LinearLayout.VERTICAL);
        adView.setBackgroundColor(Color.parseColor("#9C27B0"));
        adView.setPadding(16, 16, 16, 16);

        TextView adTitle = new TextView(this);
        adTitle.setText("WINDOWED AD");
        adTitle.setTextColor(Color.WHITE);
        adTitle.setTextSize(18);
        adTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        adTitle.setGravity(Gravity.CENTER);
        adTitle.setPadding(0, 0, 0, 8);
        adView.addView(adTitle);

        TextView adContent = new TextView(this);
        adContent.setText("This is a test windowed ad.\n\nSize: Wrap Content\n\nPosition: Adjustable");
        adContent.setTextColor(Color.WHITE);
        adContent.setTextSize(14);
        adContent.setGravity(Gravity.CENTER);
        adView.addView(adContent);

        currentAdView = adView;

        WindowedAdTest.testWindowedAdPositioning(testLayout, adView, "CENTER");

        Toast.makeText(this, "Test ad created and positioned in center", Toast.LENGTH_SHORT).show();
        SDKLogger.d(TAG, "Test ad created and positioned");
    }

    private void createImageAd() {
        SDKLogger.d(TAG, "Creating real image ad...");

        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout adContainer = new LinearLayout(this);
        adContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        adContainer.setOrientation(LinearLayout.VERTICAL);
        adContainer.setBackgroundColor(Color.parseColor("#4CAF50"));
        adContainer.setPadding(16, 16, 16, 16);

        TextView adTitle = new TextView(this);
        adTitle.setText("IMAGE AD - LOADING...");
        adTitle.setTextColor(Color.WHITE);
        adTitle.setTextSize(18);
        adTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        adTitle.setGravity(Gravity.CENTER);
        adTitle.setPadding(0, 0, 0, 8);
        adContainer.addView(adTitle);

        TextView adStatus = new TextView(this);
        adStatus.setText("Loading image ad...");
        adStatus.setTextColor(Color.WHITE);
        adStatus.setTextSize(14);
        adStatus.setGravity(Gravity.CENTER);
        adContainer.addView(adStatus);

        imageAdView = adContainer;
        currentAdView = adContainer;

        loadImageAdContent(adContainer, adTitle, adStatus);

        WindowedAdTest.testWindowedAdPositioning(testLayout, adContainer, "CENTER");

        Toast.makeText(this, "Real image ad requested", Toast.LENGTH_SHORT).show();
        SDKLogger.d(TAG, "Real image ad creation initiated");
    }

    private void createVideoAd() {
        SDKLogger.d(TAG, "Creating real video ad...");

        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout adContainer = new LinearLayout(this);
        adContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        adContainer.setOrientation(LinearLayout.VERTICAL);
        adContainer.setBackgroundColor(Color.parseColor("#2196F3"));
        adContainer.setPadding(16, 16, 16, 16);

        TextView adTitle = new TextView(this);
        adTitle.setText("VIDEO AD - LOADING...");
        adTitle.setTextColor(Color.WHITE);
        adTitle.setTextSize(18);
        adTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        adTitle.setGravity(Gravity.CENTER);
        adTitle.setPadding(0, 0, 0, 8);
        adContainer.addView(adTitle);

        TextView adStatus = new TextView(this);
        adStatus.setText("Loading video ad...");
        adStatus.setTextColor(Color.WHITE);
        adStatus.setTextSize(14);
        adStatus.setGravity(Gravity.CENTER);
        adContainer.addView(adStatus);

        videoAdView = adContainer;
        currentAdView = adContainer;

        loadVideoAdContent(adContainer, adTitle, adStatus);

        WindowedAdTest.testWindowedAdPositioning(testLayout, adContainer, "CENTER");

        Toast.makeText(this, "Real video ad requested", Toast.LENGTH_SHORT).show();
        SDKLogger.d(TAG, "Real video ad creation initiated");
    }

    private void createNativeAd() {
        SDKLogger.d(TAG, "Creating real native ad...");

        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout adContainer = new LinearLayout(this);
        adContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        adContainer.setOrientation(LinearLayout.VERTICAL);
        adContainer.setBackgroundColor(Color.parseColor("#FF9800"));
        adContainer.setPadding(16, 16, 16, 16);

        TextView adTitle = new TextView(this);
        adTitle.setText("NATIVE AD - LOADING...");
        adTitle.setTextColor(Color.WHITE);
        adTitle.setTextSize(18);
        adTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        adTitle.setGravity(Gravity.CENTER);
        adTitle.setPadding(0, 0, 0, 8);
        adContainer.addView(adTitle);

        TextView adStatus = new TextView(this);
        adStatus.setText("Loading native ad...");
        adStatus.setTextColor(Color.WHITE);
        adStatus.setTextSize(14);
        adStatus.setGravity(Gravity.CENTER);
        adContainer.addView(adStatus);

        nativeAdView = adContainer;
        currentAdView = adContainer;

        loadNativeAdContent(adContainer, adTitle, adStatus);

        WindowedAdTest.testWindowedAdPositioning(testLayout, adContainer, "CENTER");

        Toast.makeText(this, "Real native ad requested", Toast.LENGTH_SHORT).show();
        SDKLogger.d(TAG, "Real native ad creation initiated");
    }

    private void loadImageAdContent(LinearLayout container, TextView title, TextView status) {

        try {
            status.setText("Loading real image ad from SDK...");

            View adView = BidscubeSDK.getImageAdView(IMAGE_AD_PLACEMENT_ID, new AdCallback() {
                @Override
                public void onAdLoading(String placementId) {
                    runOnUiThread(() -> {
                        status.setText("Loading image ad from server...");
                        SDKLogger.d(TAG, "Image ad loading: " + placementId);
                    });
                }

                @Override
                public void onAdLoaded(String placementId) {
                    runOnUiThread(() -> {
                        status.setText("Image ad loaded successfully!");
                        SDKLogger.d(TAG, "Image ad loaded: " + placementId);
                    });
                }

                @Override
                public void onAdDisplayed(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Image ad displayed: " + placementId);
                    });
                }

                @Override
                public void onAdClicked(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Image ad clicked: " + placementId);
                    });
                }

                @Override
                public void onAdClosed(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Image ad closed: " + placementId);
                    });
                }

                @Override
                public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                    runOnUiThread(() -> {
                        status.setText("Failed to load ad: " + errorMessage);
                        title.setText("IMAGE AD - FAILED");
                        SDKLogger.e(TAG, "Image ad failed: " + placementId + " - " + errorMessage);
                    });
                }
            });

            container.removeView(status);
            container.addView(adView);

            title.setText("IMAGE AD - LOADED & INTEGRATED");
            SDKLogger.d(TAG, "Real image ad integrated into layout");

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to load image ad: " + e.getMessage());
            status.setText("Failed to load ad: " + e.getMessage());
            title.setText("IMAGE AD - ERROR");
        }
    }

    private void loadVideoAdContent(LinearLayout container, TextView title, TextView status) {
        try {
            status.setText("Loading real video ad from SDK...");

            View adView = BidscubeSDK.getVideoAdView(VIDEO_AD_PLACEMENT_ID, new AdCallback() {
                @Override
                public void onAdLoading(String placementId) {
                    runOnUiThread(() -> {
                        status.setText("Loading video ad from server...");
                        SDKLogger.d(TAG, "Video ad loading: " + placementId);
                    });
                }

                @Override
                public void onAdLoaded(String placementId) {
                    runOnUiThread(() -> {
                        status.setText("Video ad loaded successfully!");
                        SDKLogger.d(TAG, "Video ad loaded: " + placementId);
                    });
                }

                @Override
                public void onAdDisplayed(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Video ad displayed: " + placementId);
                    });
                }

                @Override
                public void onAdClicked(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Video ad clicked: " + placementId);
                    });
                }

                @Override
                public void onAdClosed(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Video ad closed: " + placementId);
                    });
                }

                @Override
                public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                    runOnUiThread(() -> {
                        status.setText("Failed to load ad: " + errorMessage);
                        title.setText("VIDEO AD - FAILED");
                        SDKLogger.e(TAG, "Video ad failed: " + placementId + " - " + errorMessage);
                    });
                }

                @Override
                public void onVideoAdStarted(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Video ad started: " + placementId);
                    });
                }

                @Override
                public void onVideoAdCompleted(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Video ad completed: " + placementId);
                    });
                }

                @Override
                public void onVideoAdSkipped(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Video ad skipped: " + placementId);
                    });
                }
            });

            container.removeView(status);
            container.addView(adView);

            title.setText("VIDEO AD - LOADED & INTEGRATED");
            SDKLogger.d(TAG, "Real video ad integrated into layout");

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to load video ad: " + e.getMessage());
            status.setText("Failed to load ad: " + e.getMessage());
            title.setText("VIDEO AD - ERROR");
        }
    }

    private void loadNativeAdContent(LinearLayout container, TextView title, TextView status) {
        try {
            status.setText("Loading real native ad from SDK...");

            View adView = BidscubeSDK.getNativeAdView(NATIVE_AD_PLACEMENT_ID, new AdCallback() {
                @Override
                public void onAdLoading(String placementId) {
                    runOnUiThread(() -> {
                        status.setText("Loading native ad from server...");
                        SDKLogger.d(TAG, "Native ad loading: " + placementId);
                    });
                }

                @Override
                public void onAdLoaded(String placementId) {
                    runOnUiThread(() -> {
                        status.setText("Native ad loaded successfully!");
                        SDKLogger.d(TAG, "Native ad loaded: " + placementId);
                    });
                }

                @Override
                public void onAdDisplayed(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Native ad displayed: " + placementId);
                    });
                }

                @Override
                public void onAdClicked(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Native ad clicked: " + placementId);
                    });
                }

                @Override
                public void onAdClosed(String placementId) {
                    runOnUiThread(() -> {
                        SDKLogger.d(TAG, "Native ad closed: " + placementId);
                    });
                }

                @Override
                public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                    runOnUiThread(() -> {
                        status.setText("Failed to load ad: " + errorMessage);
                        title.setText("NATIVE AD - FAILED");
                        SDKLogger.e(TAG, "Native ad failed: " + placementId + " - " + errorMessage);
                    });
                }
            });

            container.removeView(status);
            container.addView(adView);

            title.setText("NATIVE AD - LOADED & INTEGRATED");
            SDKLogger.d(TAG, "Real native ad integrated into layout");

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to load native ad: " + e.getMessage());
            status.setText("Failed to load ad: " + e.getMessage());
            title.setText("NATIVE AD - ERROR");
        }
    }

    private void testAdPositioning(AdPosition position) {
        if (currentAdView == null) {
            Toast.makeText(this, "Please create a test ad first", Toast.LENGTH_SHORT).show();
            return;
        }

        SDKLogger.d(TAG, "Testing ad positioning: " + position.getDisplayName());

        removeAdFromCurrentParent();

        if (position == AdPosition.ABOVE_THE_FOLD || position == AdPosition.BELOW_THE_FOLD) {
            positionAdInContentArea(position);
        } else {

            String positionString = convertAdPositionToString(position);
            WindowedAdTest.testWindowedAdPositioning(testLayout, currentAdView, positionString);
        }

        Toast.makeText(this, "Ad positioned at: " + position.getDisplayName(), Toast.LENGTH_SHORT).show();

        logPositionDetails(position);
    }

    private void removeAdFromCurrentParent() {
        if (currentAdView != null && currentAdView.getParent() != null) {
            try {
                ((ViewGroup) currentAdView.getParent()).removeView(currentAdView);
                SDKLogger.d(TAG, "Ad removed from current parent");
            } catch (Exception e) {
                SDKLogger.w(TAG, "Error removing ad from parent: " + e.getMessage());
            }
        }
    }

    private void positionAdInContentArea(AdPosition position) {

        View contentArea = findContentArea(testLayout);
        if (contentArea == null) {
            SDKLogger.w(TAG, "Content area not found, using default positioning");
            WindowedAdTest.testWindowedAdPositioning(testLayout, currentAdView, "CENTER");
            return;
        }

        if (contentArea instanceof LinearLayout) {
            LinearLayout contentLayout = (LinearLayout) contentArea;

            if (position == AdPosition.ABOVE_THE_FOLD) {

                contentLayout.addView(currentAdView, 1);
                SDKLogger.d(TAG, "Ad positioned above the fold in content area");
            } else if (position == AdPosition.BELOW_THE_FOLD) {

                contentLayout.addView(currentAdView, contentLayout.getChildCount() - 1);
                SDKLogger.d(TAG, "Ad positioned below the fold in content area");
            }
        }
    }

    private View findContentArea(View rootView) {

        if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) rootView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout layout = (LinearLayout) child;

                    if (layout.getChildCount() > 3) {
                        return layout;
                    }
                }

                View found = findContentArea(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String convertAdPositionToString(AdPosition position) {
        switch (position) {
            case UNKNOWN:
                return "CENTER";
            case ABOVE_THE_FOLD:
                return "ABOVE_FOLD";
            case BELOW_THE_FOLD:
                return "BELOW_FOLD";
            case HEADER:
                return "TOP";
            case FOOTER:
                return "BOTTOM";
            case SIDEBAR:
                return "LEFT";
            case MAYBE_DEPENDING_ON_SCREEN_SIZE:
                return "CENTER";
            case FULL_SCREEN:
                return "CENTER";
            default:
                return "CENTER";
        }
    }

    private void logPositionDetails(AdPosition position) {
        switch (position) {
            case UNKNOWN:
                SDKLogger.d(TAG, "Ad positioned at UNKNOWN - natural display, no regulation");
                break;
            case ABOVE_THE_FOLD:
                SDKLogger.d(TAG,
                        "Ad positioned at ABOVE_THE_FOLD - placed in content area above the fold (visible without scrolling)");
                break;
            case BELOW_THE_FOLD:
                SDKLogger.d(TAG,
                        "Ad positioned at BELOW_THE_FOLD - placed in content area below the fold (requires scrolling to see)");
                break;
            case HEADER:
                SDKLogger.d(TAG, "Ad positioned at HEADER - top of screen, header area");
                break;
            case FOOTER:
                SDKLogger.d(TAG, "Ad positioned at FOOTER - bottom of screen, footer area");
                break;
            case SIDEBAR:
                SDKLogger.d(TAG, "Ad positioned at SIDEBAR - left/right side of screen");
                break;
            case MAYBE_DEPENDING_ON_SCREEN_SIZE:
                SDKLogger.d(TAG,
                        "Ad positioned at MAYBE_DEPENDING_ON_SCREEN_SIZE - smart positioning based on screen size");
                break;
            case FULL_SCREEN:
                SDKLogger.d(TAG, "Ad positioned at FULL_SCREEN - full screen display");
                break;
        }
    }

    private void validateLayout() {
        SDKLogger.d(TAG, "Validating windowed ad layout requirements...");

        boolean isValid = WindowedAdTest.validateWindowedAdRequirements(testLayout);

        if (isValid) {
            Toast.makeText(this, "✓ All requirements met!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "✗ Some requirements not met", Toast.LENGTH_LONG).show();
        }

        SDKLogger.d(TAG, "Layout validation completed. Check logs for details.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        SDKLogger.d(TAG, "Windowed ad test activity resumed");

        if (testLayout != null) {
            WindowedAdTest.testScrollBehavior(testLayout);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SDKLogger.d(TAG, "Windowed ad test activity destroyed");
    }
}
