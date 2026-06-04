package com.bidscube.sdk.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bidscube.sdk.utils.VastParser;

/**
 * Top-end overlay: countdown until skip is allowed, then a small semi-transparent close control.
 */
public final class VideoSkipCloseOverlay {

    public interface OnCloseListener {
        void onCloseClicked();
    }

    private static final int DEFAULT_SKIP_SECONDS = 15;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView countdownView;
    private final Button closeButton;
    private final OnCloseListener closeListener;
    private int secondsLeft;
    private boolean destroyed;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (destroyed) {
                return;
            }
            if (secondsLeft > 0) {
                countdownView.setText(String.valueOf(secondsLeft));
                countdownView.setVisibility(View.VISIBLE);
                closeButton.setVisibility(View.GONE);
                secondsLeft--;
                handler.postDelayed(this, 1000L);
                return;
            }
            countdownView.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);
            closeButton.setAlpha(0f);
            closeButton.animate().alpha(0.85f).setDuration(200L).start();
        }
    };

    public VideoSkipCloseOverlay(Context context, String vastXml, OnCloseListener closeListener) {
        this.closeListener = closeListener;
        secondsLeft = resolveSkipSeconds(vastXml);

        countdownView = new TextView(context);
        styleChip(context, countdownView);
        countdownView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        countdownView.setTextColor(Color.WHITE);
        countdownView.setGravity(Gravity.CENTER);
        countdownView.setMinWidth(dp(context, 36));
        countdownView.setMinHeight(dp(context, 36));
        countdownView.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));

        closeButton = new Button(context);
        styleChip(context, closeButton);
        closeButton.setText("✕");
        closeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        closeButton.setTextColor(Color.WHITE);
        closeButton.setAllCaps(false);
        closeButton.setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4));
        closeButton.setMinWidth(dp(context, 36));
        closeButton.setMinHeight(dp(context, 36));
        closeButton.setVisibility(View.GONE);
        closeButton.setOnClickListener(v -> {
            if (closeListener != null) {
                closeListener.onCloseClicked();
            }
        });
    }

    public void attach(FrameLayout parent) {
        FrameLayout.LayoutParams lp = overlayLayoutParams(parent.getContext());
        countdownView.setLayoutParams(lp);
        closeButton.setLayoutParams(lp);
        parent.addView(countdownView);
        parent.addView(closeButton);
        handler.post(tick);
    }

    public void destroy() {
        destroyed = true;
        handler.removeCallbacks(tick);
        try {
            countdownView.animate().cancel();
            closeButton.animate().cancel();
        } catch (Throwable ignored) {
        }
    }

    private static int resolveSkipSeconds(String vastXml) {
        long skipMs = VastParser.getSkipOffsetMs(vastXml);
        if (skipMs > 0L) {
            return Math.max(1, (int) Math.ceil(skipMs / 1000.0));
        }
        return DEFAULT_SKIP_SECONDS;
    }

    private static void styleChip(Context context, View view) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x99000000);
        bg.setCornerRadius(dp(context, 18));
        view.setBackground(bg);
    }

    private static FrameLayout.LayoutParams overlayLayoutParams(Context context) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        int margin = dp(context, 16);
        int top = dp(context, 20);
        lp.setMargins(0, top, margin, 0);
        return lp;
    }

    private static int dp(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
