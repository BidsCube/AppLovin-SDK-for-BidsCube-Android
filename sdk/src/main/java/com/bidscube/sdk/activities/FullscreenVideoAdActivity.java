package com.bidscube.sdk.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.bidscube.sdk.AdDisplayManager;
import com.bidscube.sdk.R;
import com.bidscube.sdk.ads.VideoAdFormat;
import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.video.FullscreenAdHost;

/**
 * Fullscreen interstitial / rewarded video. A separate activity so the host app
 * receives {@code onPause} and can suspend game audio while the ad plays.
 */
public class FullscreenVideoAdActivity extends Activity implements FullscreenAdHost {

    private static Launch pending;

    private Runnable dismissListener;
    private boolean dismissNotified;
    private Runnable backHandler;
    private Runnable forfeitHandler;
    private Runnable pauseHandler;
    private Runnable resumeHandler;
    private boolean forfeited;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean audioFocusHeld;

    public static void launch(Activity from, AdDisplayManager manager, String placementId, String adm,
            VideoAdFormat format, AdCallback callback) {
        if (from == null || manager == null) {
            return;
        }
        pending = new Launch(manager, placementId, adm, format, callback);
        Intent intent = new Intent(from, FullscreenVideoAdActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        from.startActivity(intent);
        from.overridePendingTransition(0, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Launch launch = pending;
        pending = null;
        if (launch == null) {
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        FrameLayout root = new FrameLayout(this);
        setContentView(root);
        View content = findViewById(android.R.id.content);
        if (content instanceof ViewGroup) {
            ((ViewGroup) content).setClipChildren(false);
            ((ViewGroup) content).setClipToPadding(false);
        }
        launch.manager.renderFullscreenVideo(this, root, launch.placementId, launch.adm, launch.format,
                launch.callback);
    }

    public void attachPlayback(Runnable pausePlayback, Runnable resumePlayback) {
        this.pauseHandler = pausePlayback;
        this.resumeHandler = resumePlayback;
    }

    public void attachBackHandler(Runnable backHandler) {
        this.backHandler = backHandler;
    }

    public void attachForfeitHandler(Runnable forfeitHandler) {
        this.forfeitHandler = forfeitHandler;
    }

    public void markForfeited() {
        forfeited = true;
    }

    public boolean isForfeited() {
        return forfeited;
    }

    public void showRewardForfeitWarning(Runnable onLeave) {
        if (isFinishing()) {
            return;
        }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.bidscube_reward_leave_title)
                .setMessage(R.string.bidscube_reward_leave_message)
                .setNegativeButton(R.string.bidscube_reward_leave_stay, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.bidscube_reward_leave_exit, (dialog, which) -> {
                    if (onLeave != null) {
                        onLeave.run();
                    }
                })
                .show();
    }

    public void debug(String line) {
        if (line != null) {
            Log.i("BidscubeIntegration", "video " + line);
        }
    }

    @Override
    public boolean isShowing() {
        return !isFinishing() && !isDestroyed();
    }

    @Override
    public void dismiss() {
        if (!isFinishing()) {
            finish();
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public void setOnDismissListener(Runnable listener) {
        this.dismissListener = listener;
    }

    @Override
    public void onBackPressed() {
        if (backHandler != null) {
            backHandler.run();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (forfeitHandler != null && !forfeited) {
            forfeitHandler.run();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestAdAudioFocus();
        if (!forfeited && resumeHandler != null) {
            resumeHandler.run();
        }
    }

    @Override
    protected void onPause() {
        if (pauseHandler != null) {
            pauseHandler.run();
        }
        abandonAdAudioFocus();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        notifyDismissed();
        super.onDestroy();
    }

    private void notifyDismissed() {
        if (dismissNotified) {
            return;
        }
        dismissNotified = true;
        Runnable listener = dismissListener;
        dismissListener = null;
        if (listener != null) {
            listener.run();
        }
    }

    private void requestAdAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null || audioFocusHeld) {
            return;
        }
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .build())
                    .setOnAudioFocusChangeListener(focusChange -> {
                    })
                    .build();
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
        audioFocusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAdAudioFocus() {
        if (audioManager == null || !audioFocusHeld) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(null);
        }
        audioFocusHeld = false;
    }

    private static final class Launch {
        final AdDisplayManager manager;
        final String placementId;
        final String adm;
        final VideoAdFormat format;
        final AdCallback callback;

        Launch(AdDisplayManager manager, String placementId, String adm, VideoAdFormat format, AdCallback callback) {
            this.manager = manager;
            this.placementId = placementId;
            this.adm = adm;
            this.format = format;
            this.callback = callback;
        }
    }
}
