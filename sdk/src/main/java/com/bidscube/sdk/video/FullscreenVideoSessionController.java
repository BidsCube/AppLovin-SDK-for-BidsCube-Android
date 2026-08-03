package com.bidscube.sdk.video;

import com.bidscube.sdk.models.CompanionAd;
import com.bidscube.sdk.utils.VastParser;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pure state machine for fullscreen video post-linear playback. Separates linear video completion,
 * ad session completion (IMA {@code ALL_ADS_COMPLETED}), and user-driven ad close.
 */
public final class FullscreenVideoSessionController {

    private final boolean autoClose;
    private final boolean playerManagesPostVideo;
    private final boolean hasStaticCompanion;
    private final boolean hasHtmlCompanion;

    private final AtomicBoolean linearCompleted = new AtomicBoolean(false);
    private final AtomicBoolean skipped = new AtomicBoolean(false);
    private final AtomicBoolean adSessionCompleted = new AtomicBoolean(false);
    private final AtomicBoolean adClosed = new AtomicBoolean(false);
    private final AtomicBoolean linearPostVideoHandled = new AtomicBoolean(false);
    private final AtomicBoolean sessionPostVideoHandled = new AtomicBoolean(false);
    private final AtomicBoolean manualCloseShown = new AtomicBoolean(false);
    private final AtomicBoolean staticCompanionShown = new AtomicBoolean(false);
    private final AtomicBoolean htmlCompanionShown = new AtomicBoolean(false);

    public FullscreenVideoSessionController(
            boolean autoClose,
            boolean playerManagesPostVideo,
            String vastXml) {
        this(autoClose, playerManagesPostVideo, VastParser.selectPostVideoCompanion(vastXml));
    }

    public FullscreenVideoSessionController(
            boolean autoClose,
            boolean playerManagesPostVideo,
            CompanionAd companion) {
        this.autoClose = autoClose;
        this.playerManagesPostVideo = playerManagesPostVideo;
        this.hasHtmlCompanion = companion != null && companion.isInteractive();
        this.hasStaticCompanion = companion != null && companion.isStaticImage();
    }

    FullscreenVideoSessionController(
            boolean autoClose,
            boolean playerManagesPostVideo,
            boolean hasStaticCompanion,
            boolean hasHtmlCompanion) {
        this.autoClose = autoClose;
        this.playerManagesPostVideo = playerManagesPostVideo;
        this.hasStaticCompanion = hasStaticCompanion;
        this.hasHtmlCompanion = hasHtmlCompanion;
    }

    public boolean isAutoClose() {
        return autoClose;
    }

    public boolean shouldFireLinearCompleted() {
        if (skipped.get()) {
            return false;
        }
        return linearCompleted.compareAndSet(false, true);
    }

    public boolean shouldFireSkipped() {
        if (linearCompleted.get()) {
            return false;
        }
        return skipped.compareAndSet(false, true);
    }

    public boolean shouldFireAdSessionCompleted() {
        return adSessionCompleted.compareAndSet(false, true);
    }

    public boolean isAdClosed() {
        return adClosed.get();
    }

    public FullscreenPostVideoAction onLinearCompleted() {
        linearCompleted.set(true);
        return onLinearPlaybackEnded(false);
    }

    public FullscreenPostVideoAction onSkipped() {
        skipped.set(true);
        return onLinearPlaybackEnded(true);
    }

    public FullscreenPostVideoAction onAdSessionCompleted() {
        adSessionCompleted.set(true);
        if (autoClose) {
            return adClosed.get() ? FullscreenPostVideoAction.NOOP : closeEntireAd();
        }
        if (adClosed.get()) {
            return FullscreenPostVideoAction.NOOP;
        }
        if (!playerManagesPostVideo) {
            return linearPostVideoHandled.get()
                    ? FullscreenPostVideoAction.NOOP
                    : onNonImaLinearPlaybackEnded(false);
        }
        return onImaSessionCompleted();
    }

    public FullscreenPostVideoAction onPlaybackFailed() {
        if (autoClose) {
            return adClosed.get() ? FullscreenPostVideoAction.NOOP : closeEntireAd();
        }
        if (adClosed.get()) {
            return FullscreenPostVideoAction.NOOP;
        }
        linearPostVideoHandled.set(true);
        sessionPostVideoHandled.set(true);
        return finalManualCloseState(true, true);
    }

    public FullscreenPostVideoAction onUserClose() {
        return closeEntireAd();
    }

    private FullscreenPostVideoAction onLinearPlaybackEnded(boolean wasSkipped) {
        if (autoClose) {
            return closeEntireAd();
        }
        if (wasSkipped) {
            return onSkippedManualMode();
        }
        if (playerManagesPostVideo) {
            return onImaLinearCompleted();
        }
        return onNonImaLinearPlaybackEnded(false);
    }

    private FullscreenPostVideoAction onImaLinearCompleted() {
        if (!linearPostVideoHandled.compareAndSet(false, true)) {
            return FullscreenPostVideoAction.NOOP;
        }
        return FullscreenPostVideoAction.builder()
                .removeSkipOverlay(true)
                .keepPlayerVisible(true)
                .showManualCloseButton(shouldShowManualCloseButton())
                .build();
    }

    private FullscreenPostVideoAction onImaSessionCompleted() {
        if (!sessionPostVideoHandled.compareAndSet(false, true)) {
            return FullscreenPostVideoAction.NOOP;
        }
        return companionOrFinalState(true, true);
    }

    private FullscreenPostVideoAction onNonImaLinearPlaybackEnded(boolean ignoredWasSkipped) {
        if (!linearPostVideoHandled.compareAndSet(false, true)) {
            return FullscreenPostVideoAction.NOOP;
        }
        if (hasHtmlCompanion || hasStaticCompanion) {
            return companionOrFinalState(true, true);
        }
        return FullscreenPostVideoAction.builder()
                .removeSkipOverlay(true)
                .keepPlayerVisible(true)
                .showManualCloseButton(shouldShowManualCloseButton())
                .build();
    }

    private FullscreenPostVideoAction onSkippedManualMode() {
        if (!linearPostVideoHandled.compareAndSet(false, true)) {
            return FullscreenPostVideoAction.NOOP;
        }
        sessionPostVideoHandled.set(true);
        return finalManualCloseState(true, true);
    }

    private FullscreenPostVideoAction companionOrFinalState(boolean releasePlayer, boolean hidePlayer) {
        if (hasHtmlCompanion) {
            return FullscreenPostVideoAction.builder()
                    .removeSkipOverlay(true)
                    .releasePlayer(releasePlayer)
                    .hidePlayer(hidePlayer)
                    .showHtmlCompanionEndCard(htmlCompanionShown.compareAndSet(false, true))
                    .build();
        }
        if (hasStaticCompanion) {
            return FullscreenPostVideoAction.builder()
                    .removeSkipOverlay(true)
                    .releasePlayer(releasePlayer)
                    .hidePlayer(hidePlayer)
                    .showStaticCompanionEndCard(staticCompanionShown.compareAndSet(false, true))
                    .build();
        }
        return finalManualCloseState(releasePlayer, hidePlayer);
    }

    private FullscreenPostVideoAction finalManualCloseState(boolean releasePlayer, boolean hidePlayer) {
        return FullscreenPostVideoAction.builder()
                .removeSkipOverlay(true)
                .releasePlayer(releasePlayer)
                .hidePlayer(hidePlayer)
                .showManualCloseButton(shouldShowManualCloseButton())
                .build();
    }

    private boolean shouldShowManualCloseButton() {
        return manualCloseShown.compareAndSet(false, true);
    }

    private FullscreenPostVideoAction closeEntireAd() {
        if (!adClosed.compareAndSet(false, true)) {
            return FullscreenPostVideoAction.NOOP;
        }
        return FullscreenPostVideoAction.builder()
                .removeSkipOverlay(true)
                .releasePlayer(true)
                .hidePlayer(true)
                .dismissDialog(true)
                .fireAdClosed(true)
                .build();
    }
}
