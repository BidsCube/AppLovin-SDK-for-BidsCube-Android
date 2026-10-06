package com.bidscube.sdk.video;

import com.bidscube.sdk.ads.VideoAdFormat;

/**
 * Device Back and leaving the app during a fullscreen video.
 */
public final class FullscreenBackPolicy {

    public enum Action {
        /** Interstitial still inside the skip countdown. */
        IGNORE,
        /** Rewarded video is still playing; confirm that leaving drops the reward. */
        CONFIRM_REWARD_FORFEIT,
        /** Interstitial skip offset has elapsed. */
        SKIP,
        /** Linear video already finished (end card or close). */
        CLOSE
    }

    private FullscreenBackPolicy() {
    }

    public static Action onBack(VideoAdFormat format, boolean linearCompleted, boolean skippable) {
        if (linearCompleted) {
            return Action.CLOSE;
        }
        if (format == VideoAdFormat.REWARDED) {
            return Action.CONFIRM_REWARD_FORFEIT;
        }
        if (!skippable) {
            return Action.IGNORE;
        }
        return Action.SKIP;
    }

    /** Home or switching away before the linear video ends voids the view and any reward. */
    public static boolean leavingVoidsView(boolean linearCompleted) {
        return !linearCompleted;
    }
}
