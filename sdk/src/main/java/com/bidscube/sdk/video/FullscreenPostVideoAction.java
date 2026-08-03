package com.bidscube.sdk.video;

/**
 * UI/cleanup steps to apply after a fullscreen video session event. Produced by
 * {@link FullscreenVideoSessionController}; applied by {@link AdDisplayManager}.
 */
public final class FullscreenPostVideoAction {

    public static final FullscreenPostVideoAction NOOP = new FullscreenPostVideoAction(false, false, false, false,
            false, false, false, false, false);

    private final boolean removeSkipOverlay;
    private final boolean releasePlayer;
    private final boolean hidePlayer;
    private final boolean keepPlayerVisible;
    private final boolean dismissDialog;
    private final boolean fireAdClosed;
    private final boolean showStaticCompanionEndCard;
    private final boolean showHtmlCompanionEndCard;
    private final boolean showManualCloseButton;

    private FullscreenPostVideoAction(
            boolean removeSkipOverlay,
            boolean releasePlayer,
            boolean hidePlayer,
            boolean keepPlayerVisible,
            boolean dismissDialog,
            boolean fireAdClosed,
            boolean showStaticCompanionEndCard,
            boolean showHtmlCompanionEndCard,
            boolean showManualCloseButton) {
        this.removeSkipOverlay = removeSkipOverlay;
        this.releasePlayer = releasePlayer;
        this.hidePlayer = hidePlayer;
        this.keepPlayerVisible = keepPlayerVisible;
        this.dismissDialog = dismissDialog;
        this.fireAdClosed = fireAdClosed;
        this.showStaticCompanionEndCard = showStaticCompanionEndCard;
        this.showHtmlCompanionEndCard = showHtmlCompanionEndCard;
        this.showManualCloseButton = showManualCloseButton;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isRemoveSkipOverlay() {
        return removeSkipOverlay;
    }

    public boolean isReleasePlayer() {
        return releasePlayer;
    }

    public boolean isHidePlayer() {
        return hidePlayer;
    }

    public boolean isKeepPlayerVisible() {
        return keepPlayerVisible;
    }

    public boolean isDismissDialog() {
        return dismissDialog;
    }

    public boolean isFireAdClosed() {
        return fireAdClosed;
    }

    public boolean isShowStaticCompanionEndCard() {
        return showStaticCompanionEndCard;
    }

    public boolean isShowHtmlCompanionEndCard() {
        return showHtmlCompanionEndCard;
    }

    public boolean isShowManualCloseButton() {
        return showManualCloseButton;
    }

    public boolean isNoop() {
        return this == NOOP;
    }

    public static final class Builder {
        private boolean removeSkipOverlay;
        private boolean releasePlayer;
        private boolean hidePlayer;
        private boolean keepPlayerVisible;
        private boolean dismissDialog;
        private boolean fireAdClosed;
        private boolean showStaticCompanionEndCard;
        private boolean showHtmlCompanionEndCard;
        private boolean showManualCloseButton;

        public Builder removeSkipOverlay(boolean value) {
            removeSkipOverlay = value;
            return this;
        }

        public Builder releasePlayer(boolean value) {
            releasePlayer = value;
            return this;
        }

        public Builder hidePlayer(boolean value) {
            hidePlayer = value;
            return this;
        }

        public Builder keepPlayerVisible(boolean value) {
            keepPlayerVisible = value;
            return this;
        }

        public Builder dismissDialog(boolean value) {
            dismissDialog = value;
            return this;
        }

        public Builder fireAdClosed(boolean value) {
            fireAdClosed = value;
            return this;
        }

        public Builder showStaticCompanionEndCard(boolean value) {
            showStaticCompanionEndCard = value;
            return this;
        }

        public Builder showHtmlCompanionEndCard(boolean value) {
            showHtmlCompanionEndCard = value;
            return this;
        }

        public Builder showManualCloseButton(boolean value) {
            showManualCloseButton = value;
            return this;
        }

        public FullscreenPostVideoAction build() {
            return new FullscreenPostVideoAction(removeSkipOverlay, releasePlayer, hidePlayer, keepPlayerVisible,
                    dismissDialog, fireAdClosed, showStaticCompanionEndCard, showHtmlCompanionEndCard,
                    showManualCloseButton);
        }
    }
}
