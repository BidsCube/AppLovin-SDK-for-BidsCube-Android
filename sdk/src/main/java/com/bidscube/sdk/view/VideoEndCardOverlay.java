package com.bidscube.sdk.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.utils.VastParser;
import com.bumptech.glide.Glide;

/**
 * Post-video end card: companion preview image when available, otherwise a dark fallback card.
 */
public final class VideoEndCardOverlay {

    public interface OnClosedListener {
        void onClosed();
    }

    private final FrameLayout rootView;
    private final ImageView previewView;

    public VideoEndCardOverlay(Context context, String vastXml, String placementId, AdCallback callback,
            OnClosedListener closedListener) {
        rootView = new FrameLayout(context);
        rootView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        rootView.setBackgroundColor(0xFF121212);

        String companionImageUrl = VastParser.getCompanionImageUrl(vastXml);
        String companionClickUrl = VastParser.getCompanionClickThroughUrl(vastXml);
        String linearClickUrl = VastParser.getClickThroughUrl(vastXml);
        String clickUrl = !TextUtils.isEmpty(companionClickUrl) ? companionClickUrl : linearClickUrl;

        if (!TextUtils.isEmpty(companionImageUrl)) {
            previewView = new ImageView(context);
            previewView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            previewView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            previewView.setContentDescription("Ad preview");
            previewView.setOnClickListener(v -> handleClick(context, placementId, clickUrl, callback));
            rootView.addView(previewView);
            try {
                Glide.with(context.getApplicationContext())
                        .load(companionImageUrl)
                        .centerCrop()
                        .into(previewView);
            } catch (Throwable ignored) {
            }
        } else {
            previewView = null;
            TextView fallbackTitle = new TextView(context);
            fallbackTitle.setText("Ad ended");
            fallbackTitle.setTextColor(Color.WHITE);
            fallbackTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            fallbackTitle.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            titleLp.gravity = Gravity.CENTER;
            rootView.addView(fallbackTitle, titleLp);

            if (!TextUtils.isEmpty(linearClickUrl)) {
                Button ctaButton = new Button(context);
                ctaButton.setText("Learn More");
                ctaButton.setTextColor(Color.WHITE);
                ctaButton.setAllCaps(false);
                styleChip(context, ctaButton);
                ctaButton.setPadding(dp(context, 24), dp(context, 12), dp(context, 24), dp(context, 12));
                ctaButton.setOnClickListener(v -> handleClick(context, placementId, linearClickUrl, callback));
                FrameLayout.LayoutParams ctaLp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
                ctaLp.gravity = Gravity.CENTER;
                ctaLp.topMargin = dp(context, 48);
                rootView.addView(ctaButton, ctaLp);
            }
        }

        Button closeButton = new Button(context);
        closeButton.setText("✕");
        closeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        closeButton.setTextColor(Color.WHITE);
        closeButton.setAllCaps(false);
        styleChip(context, closeButton);
        closeButton.setMinWidth(dp(context, 36));
        closeButton.setMinHeight(dp(context, 36));
        closeButton.setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4));
        closeButton.setOnClickListener(v -> {
            if (closedListener != null) {
                closedListener.onClosed();
            }
        });
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        closeLp.gravity = Gravity.TOP | Gravity.END;
        int margin = dp(context, 16);
        closeLp.setMargins(0, dp(context, 20), margin, 0);
        rootView.addView(closeButton, closeLp);
    }

    public void attach(FrameLayout parent) {
        parent.addView(rootView);
    }

    public void destroy() {
        if (previewView != null) {
            try {
                Glide.with(previewView.getContext().getApplicationContext()).clear(previewView);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void handleClick(Context context, String placementId, String url, AdCallback callback) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable ignored) {
        }
        if (callback != null && placementId != null) {
            try {
                callback.onAdClicked(placementId);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void styleChip(Context context, View view) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x99000000);
        bg.setCornerRadius(dp(context, 18));
        view.setBackground(bg);
    }

    private static int dp(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
