package com.bidscube.sdk.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.ads.AdType;
import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.models.AdRenderContext;
import com.bidscube.sdk.models.enums.AdPosition;
import com.bidscube.sdk.utils.SDKLogger;
import com.bidscube.sdk.view.BannerViewFactory;
import com.bidscube.sdk.view.NativeAdBinder;

/**
 * Test activity demonstrating SDK usage
 * This shows how to properly initialize and use the SDK in a real application
 */
public class SDKTestActivity extends Activity {

    private static final String TAG = "SDKTestActivity";
    private TextView statusText;
    private LinearLayout buttonContainer;
    private LinearLayout mainLayout;
    private EditText placementIdInput;
    private TextView currentAdPositionText;
    // Clear button needs to be a field so we can disable/enable it from other methods
    private Button clearInputButton;

    // controls for hosting ads
    private Spinner positionSpinner;
    private EditText widthInput;
    private EditText heightInput;

    // root container to support overlaying ad container
    private FrameLayout rootContainer;
    private FrameLayout adContainer; // frame that hosts the banner (child) sized/positioned in rootContainer

    // Default placements to use when the text field is empty
    private static final int DEFAULT_IMAGE_PLACEMENT = 20212;
    private static final int DEFAULT_VIDEO_PLACEMENT = 20213;
    private static final int DEFAULT_NATIVE_PLACEMENT = 20214;

    // keep reference so we can disable scrolling while overlay banner is shown
    private ScrollView scrollViewRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Root frame so we can add an overlay ad container later
        rootContainer = new FrameLayout(this);

        ScrollView scrollView = new ScrollView(this);
        // keep reference so we can disable scrolling while overlay banner is shown
        this.scrollViewRef = scrollView;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        scrollView.addView(layout);
        // keep reference for later UI setup
        this.mainLayout = layout;

        statusText = new TextView(this);
        statusText.setText("SDK Status: Not Initialized");
        statusText.setTextSize(18);
        statusText.setPadding(0, 0, 0, 32);
        layout.addView(statusText);

        Button initButton = new Button(this);
        initButton.setText("Initialize SDK");
        initButton.setOnClickListener(v -> initializeSDK());
        layout.addView(initButton);

        rootContainer.addView(scrollView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(rootContainer);

        // Initially controls are disabled until SDK is initialized
        setControlsEnabled(false);
    }

    // Enable/disable placement input, clear button and all action buttons
    private void setControlsEnabled(boolean enabled) {
        if (placementIdInput != null) {
            placementIdInput.setEnabled(enabled);
        }
        if (clearInputButton != null) {
            clearInputButton.setEnabled(enabled);
        }
        if (positionSpinner != null) {
            positionSpinner.setEnabled(enabled);
        }
        if (widthInput != null) {
            widthInput.setEnabled(enabled);
        }
        if (heightInput != null) {
            heightInput.setEnabled(enabled);
        }
        if (buttonContainer != null) {
            for (int i = 0; i < buttonContainer.getChildCount(); i++) {
                android.view.View child = buttonContainer.getChildAt(i);
                if (child != null) {
                    child.setEnabled(enabled);
                }
            }
        }
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
                    setupAdButtons();
                    // enable the controls after buttons are created
                    setControlsEnabled(true);
                });
            }).start();

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to initialize SDK: " + e.getMessage());
            statusText.setText("SDK Status: Initialization Failed");
            Toast.makeText(this, "SDK initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupAdButtons() {
        // Create inputs and buttons now that SDK is initialized
        // Ensure mainLayout is available (should be set in onCreate)
        LinearLayout parent = this.mainLayout;
        if (parent == null) {
            // Fallback: try to find the first LinearLayout inside the root container's ScrollView
            try {
                if (rootContainer != null && rootContainer.getChildCount() > 0 && rootContainer.getChildAt(0) instanceof android.widget.ScrollView) {
                    android.widget.ScrollView sv = (android.widget.ScrollView) rootContainer.getChildAt(0);
                    if (sv.getChildCount() > 0 && sv.getChildAt(0) instanceof LinearLayout) {
                        parent = (LinearLayout) sv.getChildAt(0);
                        this.mainLayout = parent;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (parent == null) {
            // As last resort create a new main layout and attach it
            parent = new LinearLayout(this);
            parent.setOrientation(LinearLayout.VERTICAL);
            parent.setPadding(32, 32, 32, 32);
            rootContainer.addView(parent, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            this.mainLayout = parent;
        }

        // Create or reuse placement input and controls
        if (placementIdInput == null) {
            addTextView("Placement ID Input:", parent);
            placementIdInput = new EditText(this);
            placementIdInput.setHint("Enter placement ID:");
            placementIdInput.setText("");
            placementIdInput.setPadding(16, 16, 16, 16);
            parent.addView(placementIdInput);

            clearInputButton = new Button(this);
            clearInputButton.setText("Clear Input");
            clearInputButton.setOnClickListener(v -> placementIdInput.setText(""));
            parent.addView(clearInputButton);

            addTextView("Ad Position:", parent);
            positionSpinner = new Spinner(this);
            ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                    new String[]{"", "FULL_SCREEN", "HEADER", "FOOTER", "SIDEBAR"});
            posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            positionSpinner.setAdapter(posAdapter);
            positionSpinner.setSelection(0);
            positionSpinner.setPrompt("Select ad position (optional)");
            parent.addView(positionSpinner);

            addTextView("Optional Ad Width (dp):", parent);
            widthInput = new EditText(this);
            widthInput.setHint("e.g. 300");
            parent.addView(widthInput);

            addTextView("Optional Ad Height (dp):", parent);
            heightInput = new EditText(this);
            heightInput.setHint("e.g. 250");
            parent.addView(heightInput);

            currentAdPositionText = new TextView(this);
            currentAdPositionText.setText("Current Ad Position: Not Set");
            currentAdPositionText.setTextSize(16);
            currentAdPositionText.setPadding(0, 16, 0, 16);
            currentAdPositionText.setTextColor(0xFF666666);
            parent.addView(currentAdPositionText);
        }

        // Create or clear the buttonContainer
        if (buttonContainer == null) {
            buttonContainer = new LinearLayout(this);
            buttonContainer.setOrientation(LinearLayout.VERTICAL);
            parent.addView(buttonContainer);
        } else {
            buttonContainer.removeAllViews();
        }

        // Add ad type buttons
        Button imageAdButton = new Button(this);
        imageAdButton.setText("Image Ads");
        imageAdButton.setOnClickListener(v -> showAd(DEFAULT_IMAGE_PLACEMENT, AdType.Type.IMAGE));
        buttonContainer.addView(imageAdButton);


        Button videoAdButton = new Button(this);
        videoAdButton.setText("Video Ads");
        videoAdButton.setOnClickListener(v -> showAd(DEFAULT_VIDEO_PLACEMENT, AdType.Type.VIDEO));
        buttonContainer.addView(videoAdButton);


        Button nativeAdButton = new Button(this);
        nativeAdButton.setText("Native Ads");
        nativeAdButton.setOnClickListener(v -> showAd(DEFAULT_NATIVE_PLACEMENT, AdType.Type.NATIVE));
        buttonContainer.addView(nativeAdButton);

        addTextView("", parent);


        addTextView("Logging Control:", buttonContainer);
        Button enableLoggingButton = new Button(this);
        enableLoggingButton.setText("Enable Logging");
        enableLoggingButton.setOnClickListener(v -> enableLogging(true));
        buttonContainer.addView(enableLoggingButton);

        Button disableLoggingButton = new Button(this);
        disableLoggingButton.setText("Disable Logging");
        disableLoggingButton.setOnClickListener(v -> enableLogging(false));
        buttonContainer.addView(disableLoggingButton);

        Button testLoggingButton = new Button(this);
        testLoggingButton.setText("Test Logging");
        testLoggingButton.setOnClickListener(v -> testLogging());
        buttonContainer.addView(testLoggingButton);

        addTextView("", parent);


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

    private String getPlacementId() {
        String placementId = placementIdInput.getText().toString().trim();
        return placementId.isEmpty() ? null : placementId;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void removeAdContainer() {
        try {
            if (adContainer != null) {
                // Ensure any banner attached to this container is properly detached and cleaned
                try { NativeAdBinder.detachBannerFromContainer(adContainer); } catch (Throwable ignored) {}

                rootContainer.removeView(adContainer);
            }
        } catch (Throwable ignored) {
        } finally {
            adContainer = null;
            // restore scroll handling
            try {
                if (scrollViewRef != null) scrollViewRef.setOnTouchListener(null);
            } catch (Throwable ignored) {}
        }
    }

    // Create an overlay container according to AdPosition enum.
    private void createAdContainerForPosition(AdPosition position, Integer wDp, Integer hDp) {
        // Before creating a new container ensure any previous banners are cleared
        try { NativeAdBinder.clearAttachedBanner(); } catch (Throwable ignored) {}

        removeAdContainer();

        AdPosition pos = (position == null) ? AdPosition.UNKNOWN : position;

        // Create adContainer sized and positioned according to AdPosition. We add this directly to
        // rootContainer with appropriate FrameLayout.LayoutParams (width/height/gravity) so only the
        // ad area overlays the UI and other buttons remain clickable.
        adContainer = new FrameLayout(this);
        FrameLayout.LayoutParams lp;

        // compute adContainer layout params based on position
        int w = ViewGroup.LayoutParams.MATCH_PARENT;
        int h = ViewGroup.LayoutParams.WRAP_CONTENT;
        int gravity = Gravity.CENTER;

        switch (pos) {
            case HEADER: {
                int headerH = (hDp != null) ? dpToPx(hDp) : dpToPx(100);
                w = ViewGroup.LayoutParams.MATCH_PARENT;
                h = headerH;
                gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                break;
            }
            case FOOTER: {
                int footerH = (hDp != null) ? dpToPx(hDp) : dpToPx(100);
                w = ViewGroup.LayoutParams.MATCH_PARENT;
                h = footerH;
                gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                break;
            }
            case SIDEBAR: {
                int sideW = (wDp != null) ? dpToPx(wDp) : dpToPx(120);
                w = sideW;
                h = ViewGroup.LayoutParams.MATCH_PARENT;
                gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                break;
            }
            case FULL_SCREEN: {
                w = ViewGroup.LayoutParams.MATCH_PARENT;
                h = ViewGroup.LayoutParams.MATCH_PARENT;
                gravity = Gravity.CENTER;
                break;
            }
            default: {
                int centerH = (hDp != null) ? dpToPx(hDp) : dpToPx(140);
                w = ViewGroup.LayoutParams.MATCH_PARENT;
                h = centerH;
                gravity = Gravity.CENTER;
                break;
            }
        }

        lp = new FrameLayout.LayoutParams(w, h, gravity);
        adContainer.setLayoutParams(lp);
        adContainer.setBackgroundColor(0x00000000);

        // Add the sized ad container to the activity root so only the ad area overlays the UI
        rootContainer.addView(adContainer);
        try {
            // Ensure the ad container is on top of the view stack and visible
            adContainer.bringToFront();
            adContainer.requestLayout();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                adContainer.setElevation(100f);
            }
            SDKLogger.d(TAG, "Ad container added to rootContainer with gravity=" + gravity + " size=" + w + "x" + h);
            // Relax scroll view clipping if present
            try { if (scrollViewRef != null) scrollViewRef.setClipToPadding(false); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        // Prevent the ScrollView from scrolling while ad is present
        try {
            if (scrollViewRef != null) {
                scrollViewRef.requestDisallowInterceptTouchEvent(true);
            }
        } catch (Throwable ignored) {}
    }

    // New: accept a default placement id; if the text field has a value it takes precedence
    private void showAd(int defaultPlacementId, AdType.Type adType) {
        if (!BidscubeSDK.isInitialized()) {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure any previously attached native/banner views are cleared before showing a new ad
        try { NativeAdBinder.clearAttachedBanner(); } catch (Throwable ignored) {}

        String placementIdFromField = getPlacementId();
        String placementToUse = (placementIdFromField == null) ? String.valueOf(defaultPlacementId) : placementIdFromField;

        if (placementToUse.trim().isEmpty()) {
            showPlacementIdErrorDialog();
            return;
        }

        // read optional sizes
        // Determine if user explicitly selected a position (spinner entries are AdPosition names or empty).
        AdPosition userSelectedPosition = null;
        if (positionSpinner != null) {
            Object sel = positionSpinner.getSelectedItem();
            if (sel instanceof String) {
                String s = ((String) sel).trim();
                if (!s.isEmpty()) userSelectedPosition = AdPosition.fromString(s);
            }
        }

        Integer wDp = null;
        Integer hDp = null;
        try {
            String wText = widthInput != null ? widthInput.getText().toString().trim() : "";
            String hText = heightInput != null ? heightInput.getText().toString().trim() : "";
            if (!wText.isEmpty()) wDp = Integer.parseInt(wText);
            if (!hText.isEmpty()) hDp = Integer.parseInt(hText);
        } catch (NumberFormatException ignored) {
        }

        // Create local ad container only if the user explicitly selected a position override.
        if (userSelectedPosition != null && userSelectedPosition != AdPosition.UNKNOWN) {
            createAdContainerForPosition(userSelectedPosition, wDp, hDp);
        } else {
            removeAdContainer();
        }

        // Call SDK — don't provide an override callback from the test activity so the SDK
        // uses its internal rendering. Host apps can still provide AdCallback implementations
        // to intercept rendering; we intentionally pass null here.
        AdCallback callback = createAdCallback();
        try {
            switch (adType) {
                case IMAGE:
                    BidscubeSDK.showImageAd(placementToUse, callback);
                    break;
                case VIDEO:
                    BidscubeSDK.showVideoAd(placementToUse, callback);
                    break;
                case NATIVE:
                    BidscubeSDK.showNativeAd(placementToUse, callback);
                    break;
                default:
                    SDKLogger.e(TAG, "Unknown ad type: " + adType);
                    Toast.makeText(this, "Unknown ad type: " + adType, Toast.LENGTH_SHORT).show();
                    removeAdContainer();
                    break;
            }

        } catch (Exception e) {
            SDKLogger.e(TAG, "Failed to show ad: " + e.getMessage());
            Toast.makeText(this, "Failed to show ad: " + e.getMessage(), Toast.LENGTH_LONG).show();
            removeAdContainer();
        }
    }

    // Create an AdCallback that uses onAdRenderOverride to render the ADM or parsed native model
    private AdCallback createAdCallback() {
        return new AdCallback() {
            @Override
            public boolean onAdRenderOverride(AdRenderContext context) {
                // Unified handler: handle ADM (HTML/URL) or parsed NativeAd model via AdRenderContext
                final String adm = context.getAdm();
                SDKLogger.d(TAG, "onAdRenderOverride called - placement=" + context.getPlacementId() + " pos=" + context.getPosition() + " admLen=" + (adm != null ? adm.length() : 0) + " native=" + (context.getNativeAd() != null));
                // At render time prefer any spinner override (mapped to AdPosition) otherwise use response
                AdPosition spinnerOverridePos = null;
                if (positionSpinner != null) {
                    Object sel = positionSpinner.getSelectedItem();
                    if (sel instanceof String) {
                        String s = ((String) sel).trim();
                        if (!s.isEmpty()) spinnerOverridePos = AdPosition.fromString(s);
                    }
                }
                final AdPosition responsePos = (context.getPosition() != null) ? context.getPosition() : AdPosition.UNKNOWN;
                final AdPosition pos = spinnerOverridePos != null && spinnerOverridePos != AdPosition.UNKNOWN ? spinnerOverridePos : responsePos;
                final com.bidscube.sdk.models.natives.NativeAd nativeAd = context.getNativeAd();

                // Perform rendering synchronously — AdDisplayManager invokes onAdRenderOverride on the UI thread,
                // so we can safely manipulate views directly here. If rendering succeeds, return true so the SDK
                // skips its internal rendering.
                try {
                    // Prepare native banner sizing before creating the ad container so header/footer get correct height
                    if (nativeAd != null) {
                        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
                        int heightPx = dpToPx(200); // fallback
                        try {
                            if (nativeAd.assets != null) {
                                for (com.bidscube.sdk.models.natives.NativeAsset a : nativeAd.assets) {
                                    if (a == null) continue;
                                    if (a.img != null && a.img.url != null && a.img.w > 0 && a.img.h > 0) {
                                        // infer natural banner height from first available image aspect
                                        heightPx = Math.max(dpToPx(80), Math.round((float) screenWidthPx * a.img.h / a.img.w));
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}

                        int max = getResources().getDisplayMetrics().heightPixels / 2;
                        // Only clamp very tall creatives when displaying full-screen. For header/footer/sidebar
                        // we want the banner to use the creative's natural height so it appears as a compact banner.
                        if (pos == AdPosition.FULL_SCREEN) {
                            if (heightPx > max) heightPx = max;
                        }

                        // Additional safeguard: for non-fullscreen placements limit to a reasonable banner height
                        // so a tall creative doesn't make header/footer take half the screen. Use 250dp as default max.
                        if (pos != AdPosition.FULL_SCREEN) {
                            int bannerMaxPx = dpToPx(250);
                            if (heightPx > bannerMaxPx) heightPx = bannerMaxPx;
                        }

                        // Create ad container now with computed height (in dp) so footer/header positions match
                        int heightDp = Math.round((float) heightPx / getResources().getDisplayMetrics().density);
                        createAdContainerForPosition(pos, null, heightDp);
                        if (adContainer == null) {
                            SDKLogger.d(TAG, "Ad container could not be created");
                            return false;
                        }

                        adContainer.removeAllViews();

                        // Use screen width for banner width; pass AdPosition so binder can align overlay correctly
                        int bannerWidthPx = screenWidthPx;
                        NativeAdBinder.attachBannerToContainer(SDKTestActivity.this, nativeAd, adContainer,
                                bannerWidthPx, heightPx, android.graphics.Color.WHITE, true, android.widget.ImageView.ScaleType.FIT_CENTER, false, com.bidscube.sdk.models.enums.AdPosition.valueOf(pos.name()));
                        SDKLogger.d(TAG, "Native ad attached to adContainer (height=" + heightPx + ")");
                        return true;
                    }

                    String admToLoad = adm;
                    if (admToLoad != null) admToLoad = stripDocumentWriteWrapper(admToLoad);

                    String admTrim = (admToLoad != null) ? admToLoad.trim() : "";
                    try {
                        admTrim = admTrim.replaceAll("(?i)\\sstyle=\"[^\"]*\"", "");
                        admTrim = admTrim.replaceAll("(?i)\\sstyle='[^']*'", "");
                    } catch (Throwable ignored) {}

                    // For native banner ADM path prefer a compact 100dp webview height so header/footer banners stay small
                    WebView bannerWeb = BannerViewFactory.createBanner(SDKTestActivity.this, admTrim, 100);
                    int bannerHeightPx = (pos == AdPosition.FULL_SCREEN) ? ViewGroup.LayoutParams.MATCH_PARENT : dpToPx(100);
                    FrameLayout.LayoutParams bannerLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, bannerHeightPx);
                    adContainer.addView(bannerWeb, bannerLp);
                    try {
                        // temporary visual aid to ensure container and webview are visible during debugging
                        adContainer.setBackgroundColor(0x10FF0000); // translucent red overlay to show bounds
                        bannerWeb.setBackgroundColor(android.graphics.Color.WHITE);
                        bannerWeb.setVisibility(android.view.View.VISIBLE);
                        bannerWeb.requestFocus();
                        bannerWeb.invalidate();
                    } catch (Throwable ignored) {}
                    SDKLogger.d(TAG, "Banner WebView added to adContainer (pos=" + pos + ") admTrimLen=" + (admTrim != null ? admTrim.length() : 0));
                    return true;
                } catch (Exception e) {
                    SDKLogger.e(TAG, "Failed to render ad override synchronously: " + e.getMessage());
                    return false;
                }
            }

            // Helper: if ADM is of the form document.write(...); extract the inner HTML/JS
            private String stripDocumentWriteWrapper(String adm) {
                if (adm == null) return null;
                String s = adm.trim();
                // look for document.write( ... ); wrapper
                if (s.startsWith("document.write(")) {
                    int p1 = s.indexOf('(');
                    int p2 = s.lastIndexOf(");");
                    if (p1 >= 0 && p2 > p1) {
                        String inner = s.substring(p1 + 1, p2).trim();
                        // If inner is quoted ('...' or "..."), strip surrounding quotes
                        if ((inner.startsWith("\"") && inner.endsWith("\"")) || (inner.startsWith("'") && inner.endsWith("'"))) {
                            inner = inner.substring(1, inner.length() - 1);
                        }
                        // Unescape common escape sequences
                        inner = inner.replace("\\\"", "\"").replace("\\'", "'").replace("\\/", "/");
                        return inner;
                    }
                }
                return adm;
            }

            @Override
            public void onAdLoading(String placementId) {
                SDKLogger.d(TAG, "Ad loading: " + placementId);
                Toast.makeText(SDKTestActivity.this, "Ad loading...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdLoaded(String placementId) {
                SDKLogger.d(TAG, "Ad loaded: " + placementId);
                Toast.makeText(SDKTestActivity.this, "Ad loaded successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdDisplayed(String placementId) {
                SDKLogger.d(TAG, "Ad displayed: " + placementId);
                Toast.makeText(SDKTestActivity.this, "Ad displayed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdClicked(String placementId) {
                SDKLogger.d(TAG, "Ad clicked: " + placementId);
                Toast.makeText(SDKTestActivity.this, "Ad clicked", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdClosed(String placementId) {
                SDKLogger.d(TAG, "Ad closed: " + placementId);
                Toast.makeText(SDKTestActivity.this, "Ad closed", Toast.LENGTH_SHORT).show();
                // remove the overlay when ad is closed
                runOnUiThread(() -> removeAdContainer());
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
                SDKLogger.e(TAG, "Ad failed: " + placementId + " - " + errorMessage);
                Toast.makeText(SDKTestActivity.this, "Ad failed: " + errorMessage, Toast.LENGTH_LONG).show();
                runOnUiThread(() -> removeAdContainer());
            }

            @Override
            public void onVideoAdStarted(String placementId) {
                SDKLogger.d(TAG, "Video ad started: " + placementId);
                Toast.makeText(SDKTestActivity.this, "Video started playing", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoAdCompleted(String placementId) {
                SDKLogger.d(TAG, "Video ad completed: " + placementId);
                Toast.makeText(SDKTestActivity.this, "Video completed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoAdSkipped(String placementId) {
                SDKLogger.d(TAG, "Video ad skipped: " + placementId);
                Toast.makeText(SDKTestActivity.this, "Video skipped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoAdSkippable(String placementId) {
                SDKLogger.d(TAG, "Video ad skippable: " + placementId);
                Toast.makeText(SDKTestActivity.this, "Video can now be skipped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInstallButtonClicked(String placementId, String buttonText) {
                SDKLogger.d(TAG, "Install button clicked: " + placementId + " - " + buttonText);
                Toast.makeText(SDKTestActivity.this, "Install button clicked: " + buttonText, Toast.LENGTH_SHORT)
                        .show();
            }
        };
    }

    private void showPlacementIdErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Placement ID is required. Please enter a valid placement ID.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void cleanupSDK() {
        if (BidscubeSDK.isInitialized()) {
            BidscubeSDK.cleanup();
            statusText.setText("SDK Status: Cleaned Up");
            // Remove the dynamically created buttons and any overlay
            if (buttonContainer != null) {
                buttonContainer.removeAllViews();
            }

            // Ensure attached banners are cleared
            try { NativeAdBinder.clearAttachedBanner(); } catch (Throwable ignored) {}

            removeAdContainer();
            currentAdPositionText.setText("Current Ad Position: Not Set");
            currentAdPositionText.setTextColor(0xFF666666);
            Toast.makeText(this, "SDK cleaned up", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "SDK not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableLogging(boolean enabled) {
        SDKLogger.setLoggingEnabled(enabled);
        String message = "Logging " + (enabled ? "enabled" : "disabled");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        SDKLogger.d(TAG, "Logging control: " + message);
    }

    private void testLogging() {
        SDKLogger.d(TAG, "=== LOGGING TEST ===");
        Log.i(TAG, "This is an INFO message");
        Log.w(TAG, "This is a WARNING message");
        SDKLogger.e(TAG, "This is an ERROR message");
        Log.v(TAG, "This is a VERBOSE message");

        SDKLogger.d(TAG, "=== SDK LOGGER TEST ===");
        SDKLogger.i(TAG, "This is an SDK INFO message");
        SDKLogger.w(TAG, "This is an SDK WARNING message");
        SDKLogger.e(TAG, "This is an SDK ERROR message");
        SDKLogger.v(TAG, "This is an SDK VERBOSE message");

        String message = "Logging test completed. Check logs to see which messages appear.";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        SDKLogger.d(TAG, "Logging test completed - check if SDKLogger messages are filtered");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BidscubeSDK.isInitialized()) {
            BidscubeSDK.cleanup();
        }
    }

}
