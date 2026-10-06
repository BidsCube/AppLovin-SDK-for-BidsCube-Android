package com.bidscube.sdk.video;

import com.bidscube.sdk.ads.VideoAdFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FullscreenBackPolicyTest {

    @Test
    public void rewardedBackBeforeCompleteAsksToForfeitReward() {
        assertEquals(FullscreenBackPolicy.Action.CONFIRM_REWARD_FORFEIT,
                FullscreenBackPolicy.onBack(VideoAdFormat.REWARDED, false, false));
        assertEquals(FullscreenBackPolicy.Action.CONFIRM_REWARD_FORFEIT,
                FullscreenBackPolicy.onBack(VideoAdFormat.REWARDED, false, true));
    }

    @Test
    public void interstitialBackIsBlockedUntilSkippable() {
        assertEquals(FullscreenBackPolicy.Action.IGNORE,
                FullscreenBackPolicy.onBack(VideoAdFormat.INTERSTITIAL, false, false));
        assertEquals(FullscreenBackPolicy.Action.SKIP,
                FullscreenBackPolicy.onBack(VideoAdFormat.INTERSTITIAL, false, true));
    }

    @Test
    public void backAfterCompleteCloses() {
        assertEquals(FullscreenBackPolicy.Action.CLOSE,
                FullscreenBackPolicy.onBack(VideoAdFormat.REWARDED, true, false));
        assertEquals(FullscreenBackPolicy.Action.CLOSE,
                FullscreenBackPolicy.onBack(VideoAdFormat.INTERSTITIAL, true, true));
    }

    @Test
    public void leavingTheAppBeforeCompleteVoidsTheView() {
        assertTrue(FullscreenBackPolicy.leavingVoidsView(false));
        assertFalse(FullscreenBackPolicy.leavingVoidsView(true));
    }
}
