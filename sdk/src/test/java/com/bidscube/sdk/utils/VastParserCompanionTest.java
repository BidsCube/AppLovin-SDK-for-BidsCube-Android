package com.bidscube.sdk.utils;

import com.bidscube.sdk.models.CompanionAd;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VastParserCompanionTest {

    private static final String VAST_STATIC = ""
            + "<?xml version=\"1.0\"?><VAST version=\"3.0\"><Ad><InLine><Creatives>"
            + "<Creative><Linear><Duration>00:00:15</Duration></Linear>"
            + "<CompanionAds><Companion width=\"300\" height=\"250\">"
            + "<StaticResource creativeType=\"image/jpeg\">https://cdn.example/static.jpg</StaticResource>"
            + "<CompanionClickThrough>https://click.example/static</CompanionClickThrough>"
            + "<CompanionClickTracking>https://track.example/click1</CompanionClickTracking>"
            + "<CompanionClickTracking>https://track.example/click2</CompanionClickTracking>"
            + "<TrackingEvents><Tracking event=\"creativeView\">https://track.example/view</Tracking></TrackingEvents>"
            + "</Companion></CompanionAds></Creative></Creatives></InLine></Ad></VAST>";

    private static final String VAST_HTML = ""
            + "<?xml version=\"1.0\"?><VAST version=\"3.0\"><Ad><InLine><Creatives>"
            + "<Creative><Linear><Duration>00:00:15</Duration></Linear>"
            + "<CompanionAds><Companion width=\"320\" height=\"480\">"
            + "<HTMLResource><![CDATA[<html><body>Game</body></html>]]></HTMLResource>"
            + "<CompanionClickThrough>https://click.example/html</CompanionClickThrough>"
            + "<TrackingEvents><Tracking event=\"creativeView\">https://track.example/html-view</Tracking></TrackingEvents>"
            + "</Companion></CompanionAds></Creative></Creatives></InLine></Ad></VAST>";

    private static final String VAST_IFRAME = ""
            + "<?xml version=\"1.0\"?><VAST version=\"3.0\"><Ad><InLine><Creatives>"
            + "<Creative><Linear><Duration>00:00:15</Duration></Linear>"
            + "<CompanionAds><Companion width=\"320\" height=\"480\">"
            + "<IFrameResource>https://cdn.example/frame.html</IFrameResource>"
            + "</Companion></CompanionAds></Creative></Creatives></InLine></Ad></VAST>";

    private static final String VAST_HTML_AND_STATIC = ""
            + "<?xml version=\"1.0\"?><VAST version=\"3.0\"><Ad><InLine><Creatives>"
            + "<Creative><Linear><Duration>00:00:15</Duration></Linear>"
            + "<CompanionAds>"
            + "<Companion width=\"300\" height=\"250\">"
            + "<StaticResource creativeType=\"image/jpeg\">https://cdn.example/static.jpg</StaticResource>"
            + "<CompanionClickTracking>https://track.example/static-only</CompanionClickTracking>"
            + "</Companion>"
            + "<Companion width=\"320\" height=\"480\">"
            + "<HTMLResource><![CDATA[<html><body>HTML wins</body></html>]]></HTMLResource>"
            + "<CompanionClickTracking>https://track.example/html-only</CompanionClickTracking>"
            + "</Companion>"
            + "</CompanionAds></Creative></Creatives></InLine></Ad></VAST>";

    @Test
    public void staticCompanion_parsesResourceAndTracking() {
        CompanionAd companion = VastParser.selectPostVideoCompanion(VAST_STATIC);
        assertNotNull(companion);
        assertEquals(CompanionAd.ResourceType.STATIC, companion.getResourceType());
        assertEquals("https://cdn.example/static.jpg", companion.getResource());
        assertEquals("https://click.example/static", companion.getClickThroughUrl());
        assertEquals(2, companion.getClickTrackingUrls().size());
        assertEquals(1, companion.getCreativeViewTrackingUrls().size());
        assertTrue(VastParser.hasCompanionPreview(VAST_STATIC));
        assertFalse(VastParser.hasHtmlCompanion(VAST_STATIC));
    }

    @Test
    public void htmlCompanion_parsesHtmlResource() {
        CompanionAd companion = VastParser.selectPostVideoCompanion(VAST_HTML);
        assertNotNull(companion);
        assertEquals(CompanionAd.ResourceType.HTML, companion.getResourceType());
        assertTrue(companion.getResource().contains("Game"));
        assertTrue(VastParser.hasHtmlCompanion(VAST_HTML));
    }

    @Test
    public void iframeCompanion_parsesIframeResource() {
        CompanionAd companion = VastParser.selectPostVideoCompanion(VAST_IFRAME);
        assertNotNull(companion);
        assertEquals(CompanionAd.ResourceType.IFRAME, companion.getResourceType());
        assertEquals("https://cdn.example/frame.html", companion.getResource());
    }

    @Test
    public void htmlCompanion_isPreferredOverStatic() {
        CompanionAd companion = VastParser.selectPostVideoCompanion(VAST_HTML_AND_STATIC);
        assertNotNull(companion);
        assertEquals(CompanionAd.ResourceType.HTML, companion.getResourceType());
        assertTrue(companion.getResource().contains("HTML wins"));
        assertEquals(1, companion.getClickTrackingUrls().size());
        assertEquals("https://track.example/html-only", companion.getClickTrackingUrls().get(0));
    }

    @Test
    public void noCompanion_returnsNull() {
        String vast = "<?xml version=\"1.0\"?><VAST version=\"3.0\"><Ad><InLine><Creatives>"
                + "<Creative><Linear><Duration>00:00:15</Duration></Linear></Creative>"
                + "</Creatives></InLine></Ad></VAST>";
        assertNull(VastParser.selectPostVideoCompanion(vast));
        assertFalse(VastParser.hasCompanionPreview(vast));
        assertFalse(VastParser.hasHtmlCompanion(vast));
    }
}
