package com.bidscube.sdk.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.bidscube.sdk.models.natives.NativeAd;
import com.bidscube.sdk.models.natives.NativeAsset;
import com.bidscube.sdk.models.natives.Title;
import com.bidscube.sdk.models.natives.Image;
import com.bidscube.sdk.models.natives.NativeData;
import com.bidscube.sdk.models.natives.Video;
import com.bidscube.sdk.models.natives.NativeLink;
import com.bidscube.sdk.network.NativeImpressionTracker;
import com.bidscube.sdk.utils.SDKLogger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A custom view for displaying native ads with a modern, professional design.
 * Supports all native ad asset types: title, image, data, video, and CTA buttons.
 * Features:
 * - Image loading from URLs with URL decoding support
 * - Click handling to redirect to ad links
 * - Customizable CTA button text
 * - Professional card-based design
 * - Full-size image display
 */
public class NativeAdView extends CardView {

    private NativeAd nativeAd;
    private OnNativeAdClickListener clickListener;
    private boolean impressionSent = false;

    private final ExecutorService imageExecutor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private LinearLayout contentContainer;
    private ImageView adIcon;
    private TextView adTitle;
    private TextView adDescription;
    private TextView adSubTitle;
    private TextView adRating;
    private RatingBar ratingBar;
    private Button ctaButton;
    private ImageView adImage;
    private LinearLayout mediaContainer;

    public interface OnNativeAdClickListener {
        void onAdClicked(NativeAd ad);

        void onAdImageClicked(NativeAd ad);
    }

    public NativeAdView(Context context) {
        super(context);
        init(context);
    }

    public NativeAdView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NativeAdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        setCardElevation(8f);
        setRadius(12f);
        setCardBackgroundColor(Color.WHITE);

        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(16, 16, 16, 16);

        setupHeader(context);
        setupMediaContent(context);
        setupTextContent(context);
        setupCTAButton(context);

        addView(contentContainer);
    }

    private void setupHeader(Context context) {
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 0, 0, 12);

        adIcon = new ImageView(context);
        adIcon.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
        adIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        adIcon.setBackgroundResource(android.R.drawable.ic_menu_gallery);

        LinearLayout headerTextContainer = new LinearLayout(context);
        headerTextContainer.setOrientation(LinearLayout.VERTICAL);
        headerTextContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        headerTextContainer.setPadding(12, 0, 0, 0);

        adTitle = new TextView(context);
        adTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        adTitle.setTypeface(null, Typeface.BOLD);
        adTitle.setTextColor(Color.BLACK);
        adTitle.setMaxLines(3);
        adTitle.setEllipsize(TextUtils.TruncateAt.END);

        adSubTitle = new TextView(context);
        adSubTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        adSubTitle.setTextColor(Color.GRAY);

        headerTextContainer.addView(adTitle);
        headerTextContainer.addView(adSubTitle);

        headerLayout.addView(adIcon);
        headerLayout.addView(headerTextContainer);

        contentContainer.addView(headerLayout);
    }

    private void setupMediaContent(Context context) {
        mediaContainer = new LinearLayout(context);
        mediaContainer.setOrientation(LinearLayout.VERTICAL);
        mediaContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        adImage = new ImageView(context);
        adImage.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        adImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        adImage.setAdjustViewBounds(true);
        adImage.setBackgroundResource(android.R.drawable.ic_menu_gallery);

        mediaContainer.addView(adImage);
        contentContainer.addView(mediaContainer);
    }

    private void setupTextContent(Context context) {
        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        textContainer.setPadding(0, 12, 0, 12);

        adDescription = new TextView(context);
        adDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        adDescription.setTextColor(Color.DKGRAY);
        adDescription.setMaxLines(3);
        adDescription.setEllipsize(TextUtils.TruncateAt.END);
        adDescription.setLineSpacing(0, 1.2f);

        LinearLayout ratingContainer = new LinearLayout(context);
        ratingContainer.setOrientation(LinearLayout.HORIZONTAL);
        ratingContainer.setGravity(Gravity.CENTER_VERTICAL);
        ratingContainer.setPadding(0, 8, 0, 0);

        textContainer.addView(adDescription);
        textContainer.addView(ratingContainer);

        contentContainer.addView(textContainer);
    }

    private void setupCTAButton(Context context) {
        ctaButton = new Button(context);
        ctaButton.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        ctaButton.setBackgroundColor(Color.parseColor("#2196F3"));
        ctaButton.setTextColor(Color.WHITE);
        ctaButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        ctaButton.setTypeface(null, Typeface.BOLD);
        ctaButton.setPadding(24, 12, 24, 12);
        ctaButton.setGravity(Gravity.CENTER);

        contentContainer.addView(ctaButton);
    }

    /**
     * Sets the native ad data and populates the view
     */
    public void setNativeAd(NativeAd ad) {
        this.nativeAd = ad;
        this.impressionSent = false;
        if (ad == null) {
            Log.w("NativeAdView", "NativeAd is null");
            return;
        }

        SDKLogger.d("NativeAdView", "Setting native ad with " +
                (ad.assets != null ? ad.assets.size() : 0) + " assets");

        populateView();
        setupClickListeners();
        maybeFireNativeImpression();
    }

    private void populateView() {
        if (nativeAd == null || nativeAd.assets == null) {
            Log.w("NativeAdView", "Cannot populate view: nativeAd=" + (nativeAd != null) +
                    ", assets=" + (nativeAd != null && nativeAd.assets != null ? nativeAd.assets.size() : "null"));
            return;
        }

        SDKLogger.d("NativeAdView", "Populating view with " + nativeAd.assets.size() + " assets");

        Title titleAsset = null;
        Title subTitleAsset = null;
        Image imageAsset = null;
        Image iconAsset = null;
        NativeData descriptionAsset = null;
        NativeData priceAsset = null;
        NativeData ctaAsset = null;

        for (int i = 0; i < nativeAd.assets.size(); i++) {
            NativeAsset asset = nativeAd.assets.get(i);
            SDKLogger.d("NativeAdView", "Processing asset " + i + ": id=" + asset.id +
                    ", title=" + (asset.title != null) +
                    ", img=" + (asset.img != null) +
                    ", data=" + (asset.data != null));

            if (asset.title != null && titleAsset == null) {
                titleAsset = asset.title;
                SDKLogger.d("NativeAdView", "Found title asset: " + asset.title.text);
            }


            if (asset.img != null) {
                if (imageAsset == null) {
                    imageAsset = asset.img;
                    SDKLogger.d("NativeAdView", "Found main image asset: " + asset.img.url);
                } else if (iconAsset == null && asset.img.w != null && asset.img.h != null &&
                        asset.img.w <= 100 && asset.img.h <= 100) {

                    iconAsset = asset.img;
                    SDKLogger.d("NativeAdView", "Found icon asset: " + asset.img.url);
                }
            }

            if (asset.data != null) {
                SDKLogger.d("NativeAdView", "Data asset id: " + asset.id +
                        ", type: " + (asset.data.type != null ? asset.data.type.v : "null") +
                        ", value: " + asset.data.value);

                if (asset.data.type != null) {

                    switch (asset.data.type.v) {
                        case 2:
                            if (descriptionAsset == null) descriptionAsset = asset.data;
                            break;
                        case 6:
                            if (priceAsset == null) priceAsset = asset.data;
                            break;
                        case 12:
                            if (ctaAsset == null) ctaAsset = asset.data;
                            break;
                    }
                } else {

                    switch (asset.id) {
                        case 1:
                            if (ctaAsset == null) ctaAsset = asset.data;
                            SDKLogger.d("NativeAdView", "Found CTA asset by ID: " + asset.data.value);
                            break;
                        case 6:
                            if (priceAsset == null) priceAsset = asset.data;
                            SDKLogger.d("NativeAdView", "Found price asset by ID: " + asset.data.value);
                            break;
                        default:

                            if (descriptionAsset == null) descriptionAsset = asset.data;
                            break;
                    }
                }
            }
        }

        if (titleAsset != null) {
            SDKLogger.d("NativeAdView", "Setting title: " + titleAsset.text);
            setTitle(titleAsset);
        }

        if (imageAsset != null) {
            SDKLogger.d("NativeAdView", "Setting main image: " + imageAsset.url);
            setImage(imageAsset);
        }

        if (iconAsset != null) {
            SDKLogger.d("NativeAdView", "Setting icon: " + iconAsset.url);
            setAdIcon(iconAsset.url);
        }

        if (descriptionAsset != null) {
            SDKLogger.d("NativeAdView", "Setting description: " + descriptionAsset.value);
            setData(descriptionAsset);
        }

        if (ctaAsset != null && !TextUtils.isEmpty(ctaAsset.value)) {
            SDKLogger.d("NativeAdView", "Setting CTA: " + ctaAsset.value);
            ctaButton.setText(ctaAsset.value);
        } else {
            SDKLogger.d("NativeAdView", "Using default CTA: Learn More");
            ctaButton.setText("Learn More");
        }

        if (priceAsset != null && !TextUtils.isEmpty(priceAsset.value)) {
            adSubTitle.setText(priceAsset.value);
            SDKLogger.d("NativeAdView", "Price: " + priceAsset.value);

        }

        SDKLogger.d("NativeAdView", "View populated successfully");
    }

    private void setTitle(Title title) {
        if (title != null && !TextUtils.isEmpty(title.text)) {
            adTitle.setText(title.text);
        }
    }

    private void setImage(Image image) {
        if (image != null && !TextUtils.isEmpty(image.url)) {

            loadImageFromUrl(image.url, adImage);
        }
    }

    private void setData(NativeData data) {
        if (data != null && !TextUtils.isEmpty(data.value)) {
            adDescription.setText(data.value);
        }
    }

    private void setVideo(Video video) {
        if (video != null && !TextUtils.isEmpty(video.vasttag)) {


            adImage.setBackgroundColor(Color.BLACK);

        }
    }

    /**
     * Fixes malformed URLs with Unicode escape sequences
     */
    private String fixMalformedUrl(String url) {
        if (TextUtils.isEmpty(url)) return url;

        try {

            String fixedUrl = url
                    .replace("\\u0026", "&")
                    .replace("\\u0027", "'")
                    .replace("\\u0022", "\"")
                    .replace("\\u003C", "<")
                    .replace("\\u003E", ">");

            String decodedUrl = URLDecoder.decode(fixedUrl, StandardCharsets.UTF_8.name());

            SDKLogger.d("NativeAdView", "Original URL: " + url);
            SDKLogger.d("NativeAdView", "Fixed URL: " + fixedUrl);
            SDKLogger.d("NativeAdView", "Decoded URL: " + decodedUrl);

            return decodedUrl;
        } catch (Exception e) {
            Log.w("NativeAdView", "Error fixing malformed URL: " + e.getMessage());
            return url;
        }
    }

    /**
     * Modern image loading using ExecutorService with URL fixing
     */
    private void loadImageFromUrl(String imageUrl, ImageView imageView) {
        if (TextUtils.isEmpty(imageUrl)) return;

        String fixedUrl = fixMalformedUrl(imageUrl);

        SDKLogger.d("NativeAdView", "Starting to load image from fixed URL: " + fixedUrl);

        mainHandler.post(() -> {
            imageView.setBackgroundColor(Color.LTGRAY);
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        });

        imageExecutor.execute(() -> {
            try {
                SDKLogger.d("NativeAdView", "Connecting to image URL: " + fixedUrl);
                URL url = new URL(fixedUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)");
                connection.connect();

                int responseCode = connection.getResponseCode();
                SDKLogger.d("NativeAdView", "Image response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                    connection.disconnect();

                    SDKLogger.d("NativeAdView", "Image loaded successfully, bitmap: " + (bitmap != null ? "valid" : "null"));

                    mainHandler.post(() -> {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setBackgroundColor(Color.TRANSPARENT);
                            SDKLogger.d("NativeAdView", "Image set to ImageView successfully");
                        } else {

                            imageView.setBackgroundColor(Color.LTGRAY);
                            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                            Log.w("NativeAdView", "Failed to decode image, showing placeholder");
                        }
                    });
                } else {
                    SDKLogger.e("NativeAdView", "HTTP error: " + responseCode);
                    connection.disconnect();
                    mainHandler.post(() -> {
                        imageView.setBackgroundColor(Color.LTGRAY);
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                    });
                }

            } catch (Exception e) {
                SDKLogger.e("NativeAdView", "Error loading image: " + e.getMessage(), e);
                mainHandler.post(() -> {

                    imageView.setBackgroundColor(Color.LTGRAY);
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                });
            }
        });
    }

    private void setupClickListeners() {


        setOnClickListener(v -> {
            SDKLogger.d("NativeAdView", "Main ad clicked");
            if (nativeAd != null && nativeAd.link != null && !TextUtils.isEmpty(nativeAd.link.url)) {
                SDKLogger.d("NativeAdView", "Opening ad link: " + nativeAd.link.url);
                openAdLink(nativeAd.link.url);
            } else {
                Log.w("NativeAdView", "Cannot open ad link: nativeAd=" + (nativeAd != null) +
                        ", link=" + (nativeAd != null && nativeAd.link != null) +
                        ", url=" + (nativeAd != null && nativeAd.link != null ? nativeAd.link.url : "null"));
            }
            if (clickListener != null && nativeAd != null) {
                clickListener.onAdClicked(nativeAd);
            }
        });

        ctaButton.setOnClickListener(v -> {
            SDKLogger.d("NativeAdView", "CTA button clicked");
            if (nativeAd != null && nativeAd.link != null && !TextUtils.isEmpty(nativeAd.link.url)) {
                SDKLogger.d("NativeAdView", "Opening ad link from CTA: " + nativeAd.link.url);
                openAdLink(nativeAd.link.url);
            } else {
                Log.w("NativeAdView", "Cannot open ad link from CTA: nativeAd=" + (nativeAd != null) +
                        ", link=" + (nativeAd != null && nativeAd.link != null) +
                        ", url=" + (nativeAd != null && nativeAd.link != null ? nativeAd.link.url : "null"));
            }
            if (clickListener != null && nativeAd != null) {
                clickListener.onAdClicked(nativeAd);
            }
        });

        adImage.setOnClickListener(v -> {
            SDKLogger.d("NativeAdView", "Ad image clicked");
            if (nativeAd != null && nativeAd.link != null && !TextUtils.isEmpty(nativeAd.link.url)) {
                SDKLogger.d("NativeAdView", "Opening ad link from image: " + nativeAd.link.url);
                openAdLink(nativeAd.link.url);
            } else {
                Log.w("NativeAdView", "Cannot open ad link from image: nativeAd=" + (nativeAd != null) +
                        ", link=" + (nativeAd != null && nativeAd.link != null) +
                        ", url=" + (nativeAd != null && nativeAd.link != null ? nativeAd.link.url : "null"));
            }
            if (clickListener != null && nativeAd != null) {
                clickListener.onAdImageClicked(nativeAd);
            }
        });
    }

    /**
     * Opens the ad link in a browser or appropriate app
     */
    private void openAdLink(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            SDKLogger.d("NativeAdView", "Successfully opened ad link: " + url);
        } catch (Exception e) {
            SDKLogger.e("NativeAdView", "Error opening ad link: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets the click listener for ad interactions
     */
    public void setOnNativeAdClickListener(OnNativeAdClickListener listener) {
        this.clickListener = listener;
        setupClickListeners();
    }

    /**
     * Gets the current native ad
     */
    public NativeAd getNativeAd() {
        return nativeAd;
    }

    /**
     * Sets custom styling for the view
     */
    public void setCustomStyle(int backgroundColor, int textColor, int buttonColor) {
        setCardBackgroundColor(backgroundColor);
        adTitle.setTextColor(textColor);
        adDescription.setTextColor(textColor);
        ctaButton.setBackgroundColor(buttonColor);
    }

    /**
     * Sets custom CTA button text
     */
    public void setCTAText(String ctaText) {
        if (ctaText != null && !TextUtils.isEmpty(ctaText)) {
            ctaButton.setText(ctaText);
        }
    }

    /**
     * Sets custom CTA button with text and color
     */
    public void setCTAButton(String ctaText, int buttonColor) {
        setCTAText(ctaText);
        ctaButton.setBackgroundColor(buttonColor);
    }

    /**
     * Sets custom CTA button with text, color, and text color
     */
    public void setCTAButton(String ctaText, int buttonColor, int textColor) {
        setCTAText(ctaText);
        ctaButton.setBackgroundColor(buttonColor);
        ctaButton.setTextColor(textColor);
    }

    /**
     * Loads and displays an image from a URL
     */
    public void loadImage(String imageUrl) {
        if (!TextUtils.isEmpty(imageUrl)) {
            loadImageFromUrl(imageUrl, adImage);
        }
    }

    /**
     * Sets the ad icon from a URL
     */
    public void setAdIcon(String iconUrl) {
        if (!TextUtils.isEmpty(iconUrl)) {
            loadImageFromUrl(iconUrl, adIcon);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        maybeFireNativeImpression();
    }

    private synchronized void maybeFireNativeImpression() {
        if (impressionSent || nativeAd == null || !isAttachedToWindow()) {
            return;
        }

        impressionSent = NativeImpressionTracker.fireIfNeeded(nativeAd, "native_view_attached");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (imageExecutor != null && !imageExecutor.isShutdown()) {
            imageExecutor.shutdown();
        }
    }
}