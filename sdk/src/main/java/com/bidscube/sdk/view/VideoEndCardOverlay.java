package com.bidscube.sdk.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.models.CompanionAd;
import com.bidscube.sdk.utils.CompanionClickHandler;
import com.bidscube.sdk.utils.VastParser;
import com.bumptech.glide.Glide;

/**
 * Post-video static image VAST Companion.
 */
public final class VideoEndCardOverlay {

    public interface OnClosedListener {
        void onClosed();
    }

    private final FrameLayout rootView;
    private final ImageView previewView;
    private final CompanionClickHandler clickHandler;

    public VideoEndCardOverlay(Context context, String vastXml, String placementId, AdCallback callback,
            OnClosedListener closedListener) {
        this(context, VastParser.selectPostVideoCompanion(vastXml), placementId, callback, closedListener);
    }

    public VideoEndCardOverlay(Context context, CompanionAd companionAd, String placementId, AdCallback callback,
            OnClosedListener closedListener) {
        rootView = new FrameLayout(context);
        rootView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        rootView.setBackgroundColor(0xFF121212);

        clickHandler = new CompanionClickHandler(companionAd);
        String companionImageUrl = companionAd != null ? companionAd.getResource() : null;

        previewView = new ImageView(context);
        previewView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        previewView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        previewView.setContentDescription("Ad preview");
        previewView.setOnClickListener(v -> clickHandler.handleClick(context, placementId, callback));
        rootView.addView(previewView);
        if (!TextUtils.isEmpty(companionImageUrl)) {
            try {
                Glide.with(context.getApplicationContext())
                        .load(companionImageUrl)
                        .centerCrop()
                        .into(previewView);
            } catch (Throwable ignored) {
            }
        }

        rootView.addView(buildCloseButton(context, closedListener));
    }

    public void attach(FrameLayout parent) {
        parent.addView(rootView);
        clickHandler.fireCreativeViewOnce();
    }

    public void destroy() {
        if (previewView != null) {
            try {
                Glide.with(previewView.getContext().getApplicationContext()).clear(previewView);
            } catch (Throwable ignored) {
            }
        }
        try {
            if (rootView.getParent() instanceof FrameLayout) {
                ((FrameLayout) rootView.getParent()).removeView(rootView);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Button buildCloseButton(Context context, OnClosedListener closedListener) {
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
        closeButton.setLayoutParams(closeLp);
        return closeButton;
    }

    static void styleChipPublic(Context context, View view) {
        styleChip(context, view);
    }

    static int dpPublic(Context context, int dp) {
        return dp(context, dp);
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
