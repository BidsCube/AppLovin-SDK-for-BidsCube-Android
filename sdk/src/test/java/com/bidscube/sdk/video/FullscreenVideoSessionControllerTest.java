package com.bidscube.sdk.video;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FullscreenVideoSessionControllerTest {

    private static final String VAST_NO_COMPANION = ""
            + "<?xml version=\"1.0\"?><VAST version=\"3.0\"><Ad><InLine><Creatives>"
            + "<Creative><Linear><Duration>00:00:15</Duration><MediaFiles>"
            + "<MediaFile delivery=\"progressive\" type=\"video/mp4\">https://example.com/v.mp4</MediaFile>"
            + "</MediaFiles></Linear></Creative></Creatives></InLine></Ad></VAST>";

    private static final String VAST_STATIC_COMPANION = ""
            + "<?xml version=\"1.0\"?><VAST version=\"3.0\"><Ad><InLine><Creatives>"
            + "<Creative><Linear><Duration>00:00:15</Duration><MediaFiles>"
            + "<MediaFile delivery=\"progressive\" type=\"video/mp4\">https://example.com/v.mp4</MediaFile>"
            + "</MediaFiles></Linear><CompanionAds><Companion width=\"300\" height=\"250\">"
            + "<StaticResource creativeType=\"image/jpeg\">https://example.com/end.jpg</StaticResource>"
            + "</Companion></CompanionAds></Creative></Creatives></InLine></Ad></VAST>";

    private static final String VAST_HTML_COMPANION = ""
            + "<?xml version=\"1.0\"?><VAST version=\"3.0\"><Ad><InLine><Creatives>"
            + "<Creative><Linear><Duration>00:00:15</Duration><MediaFiles>"
            + "<MediaFile delivery=\"progressive\" type=\"video/mp4\">https://example.com/v.mp4</MediaFile>"
            + "</MediaFiles></Linear><CompanionAds><Companion width=\"300\" height=\"250\">"
            + "<HTMLResource><![CDATA[<html><body>Play</body></html>]]></HTMLResource>"
            + "</Companion></CompanionAds></Creative></Creatives></InLine></Ad></VAST>";

    @Test
    public void autoCloseTrueDismissesWithoutCompanion() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(true, false,
                VAST_NO_COMPANION);
        assertTrue(session.shouldFireLinearCompleted());
        FullscreenPostVideoAction action = session.onLinearCompleted();
        assertTrue(action.isDismissDialog());
        assertTrue(action.isFireAdClosed());
        assertTrue(action.isReleasePlayer());
        assertFalse(action.isShowStaticCompanionEndCard());
    }

    @Test
    public void autoCloseTrueAlsoDismissesWithCompanion() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(true, false,
                VAST_STATIC_COMPANION);
        FullscreenPostVideoAction action = session.onLinearCompleted();
        assertTrue(action.isDismissDialog());
        assertTrue(action.isFireAdClosed());
        assertFalse(action.isShowStaticCompanionEndCard());
    }

    @Test
    public void autoCloseFalseWithoutCompanionKeepsOpenWithCloseButton() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, false,
                VAST_NO_COMPANION);
        assertTrue(session.shouldFireLinearCompleted());
        FullscreenPostVideoAction action = session.onLinearCompleted();
        assertFalse(action.isDismissDialog());
        assertFalse(action.isFireAdClosed());
        assertTrue(action.isShowManualCloseButton());
        assertTrue(action.isKeepPlayerVisible());
        assertFalse(action.isReleasePlayer());
    }

    @Test
    public void autoCloseFalseWithStaticCompanionShowsEndCard() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, false,
                VAST_STATIC_COMPANION);
        FullscreenPostVideoAction action = session.onLinearCompleted();
        assertTrue(action.isShowStaticCompanionEndCard());
        assertFalse(action.isShowManualCloseButton());
        assertFalse(action.isDismissDialog());
        assertTrue(action.isReleasePlayer());
    }

    @Test
    public void imaLinearComplete_keepsPlayerForPostVideoExperience() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, true,
                VAST_NO_COMPANION);
        FullscreenPostVideoAction action = session.onLinearCompleted();
        assertTrue(action.isKeepPlayerVisible());
        assertFalse(action.isReleasePlayer());
        assertTrue(action.isShowManualCloseButton());
        assertFalse(action.isDismissDialog());
    }

    @Test
    public void imaAllAdsCompleted_withHtmlCompanion_releasesPlayerAndShowsHtml() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, true,
                VAST_HTML_COMPANION);
        session.onLinearCompleted();
        FullscreenPostVideoAction action = session.onAdSessionCompleted();
        assertTrue(action.isReleasePlayer());
        assertTrue(action.isHidePlayer());
        assertTrue(action.isShowHtmlCompanionEndCard());
        assertFalse(action.isShowStaticCompanionEndCard());
        assertFalse(action.isDismissDialog());
    }

    @Test
    public void imaAllAdsCompleted_withStaticCompanion_releasesPlayerAndShowsStatic() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, true,
                VAST_STATIC_COMPANION);
        session.onLinearCompleted();
        FullscreenPostVideoAction action = session.onAdSessionCompleted();
        assertTrue(action.isReleasePlayer());
        assertTrue(action.isShowStaticCompanionEndCard());
        assertFalse(action.isShowHtmlCompanionEndCard());
    }

    @Test
    public void imaAllAdsCompleted_withoutCompanion_releasesPlayerAndShowsFinalCloseState() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, true,
                VAST_NO_COMPANION);
        session.onLinearCompleted();
        FullscreenPostVideoAction action = session.onAdSessionCompleted();
        assertTrue(action.isReleasePlayer());
        assertTrue(action.isHidePlayer());
        assertFalse(action.isShowHtmlCompanionEndCard());
        assertFalse(action.isShowStaticCompanionEndCard());
        assertFalse(action.isShowManualCloseButton());
    }

    @Test
    public void nonImaCompanion_linearThenSession_secondActionIsNoop() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, false,
                VAST_STATIC_COMPANION);
        FullscreenPostVideoAction linear = session.onLinearCompleted();
        assertTrue(linear.isShowStaticCompanionEndCard());
        FullscreenPostVideoAction sessionAction = session.onAdSessionCompleted();
        assertTrue(sessionAction.isNoop());
    }

    @Test
    public void nonImaCompanion_linearThenSession_doesNotRestoreReleasedPlayer() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, false,
                VAST_STATIC_COMPANION);
        FullscreenPostVideoAction linear = session.onLinearCompleted();
        assertTrue(linear.isReleasePlayer());
        assertFalse(linear.isKeepPlayerVisible());
        FullscreenPostVideoAction sessionAction = session.onAdSessionCompleted();
        assertTrue(sessionAction.isNoop());
    }

    @Test
    public void skipManualMode_doesNotRewardOrShowCompanion() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, false,
                VAST_STATIC_COMPANION);
        FullscreenPostVideoAction action = session.onSkipped();
        assertTrue(action.isReleasePlayer());
        assertTrue(action.isHidePlayer());
        assertTrue(action.isShowManualCloseButton());
        assertFalse(action.isShowStaticCompanionEndCard());
        assertFalse(action.isShowHtmlCompanionEndCard());
        assertFalse(action.isDismissDialog());
    }

    @Test
    public void duplicateCompleted_doesNotDuplicateCallbacks() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, false,
                VAST_NO_COMPANION);
        assertTrue(session.shouldFireLinearCompleted());
        assertFalse(session.shouldFireLinearCompleted());
        FullscreenPostVideoAction first = session.onLinearCompleted();
        FullscreenPostVideoAction second = session.onLinearCompleted();
        assertTrue(first.isShowManualCloseButton());
        assertTrue(second.isNoop());
    }

    @Test
    public void duplicateAllAdsCompleted_doesNotDuplicateOverlayOrTracking() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, true,
                VAST_HTML_COMPANION);
        session.onLinearCompleted();
        FullscreenPostVideoAction first = session.onAdSessionCompleted();
        FullscreenPostVideoAction second = session.onAdSessionCompleted();
        assertTrue(first.isShowHtmlCompanionEndCard());
        assertTrue(second.isNoop());
    }

    @Test
    public void userCloseAndBack_closeExactlyOnce() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, true,
                VAST_NO_COMPANION);
        session.onLinearCompleted();
        FullscreenPostVideoAction first = session.onUserClose();
        FullscreenPostVideoAction second = session.onUserClose();
        assertTrue(first.isFireAdClosed());
        assertTrue(second.isNoop());
    }

    @Test
    public void skipDoesNotFireLinearCompletedAfterSkip() {
        FullscreenVideoSessionController session = new FullscreenVideoSessionController(false, false,
                VAST_NO_COMPANION);
        assertTrue(session.shouldFireSkipped());
        assertFalse(session.shouldFireLinearCompleted());
    }
}
