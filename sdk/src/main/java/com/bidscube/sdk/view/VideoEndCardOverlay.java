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

import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.utils.VastParser;
import com.bumptech.glide.Glide;

/**
 * Post-video end card shown only when VAST includes a companion preview image.
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
