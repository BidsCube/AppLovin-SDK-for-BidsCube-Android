package com.bidscube.sdk.qa;

/**
 * Hardcoded VAST payloads for QA manual testing (no SSP / backend required).
 * Use with {@link com.bidscube.sdk.BidscubeSDK#showVideoAdFromVastMarkup}.
 */
public final class QaVastFixtures {

    private QaVastFixtures() {
    }

    public static final String PLACEMENT_NO_PREVIEW = "qa-vast-no-preview";
    public static final String PLACEMENT_WITH_PREVIEW = "qa-vast-with-preview";

    /** Doordash video, no Companion / StaticResource. */
    public static final String VAST_WITHOUT_PREVIEW =
            "<VAST version=\"3.0\">"
                    + "<Ad id=\"20\"><InLine>"
                    + "<AdSystem version=\"3.0\">Bidscube</AdSystem>"
                    + "<AdTitle><![CDATA[Doordash-35min-burger-3-1x1.mp4]]></AdTitle>"
                    + "<Creatives><Creative><Linear>"
                    + "<Duration>00:00:12.867</Duration>"
                    + "<MediaFiles>"
                    + "<MediaFile delivery=\"progressive\" type=\"video/mp4\" bitrate=\"800\" "
                    + "width=\"1024\" height=\"1024\">"
                    + "<![CDATA[https://assets.remerge.io/ad_assets/files/003/411/782/"
                    + "1024x1024_800_mp4/Doordash-35min-burger-3-1x1.mp4]]>"
                    + "</MediaFile>"
                    + "</MediaFiles>"
                    + "</Linear></Creative></Creatives>"
                    + "</InLine></Ad>"
                    + "</VAST>";

    /** Skippable video with companion preview image and click-through. */
    public static final String VAST_WITH_PREVIEW =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<VAST version=\"4.2\">"
                    + "<Ad id=\"12345\"><InLine>"
                    + "<AdSystem version=\"1.0\">BidscubeTest</AdSystem>"
                    + "<AdTitle>Sample Skippable VAST Ad With Preview</AdTitle>"
                    + "<Impression><![CDATA[https://example.com/impression]]></Impression>"
                    + "<Creatives>"
                    + "<Creative id=\"1\" sequence=\"1\">"
                    + "<Linear skipoffset=\"00:00:05\">"
                    + "<Duration>00:00:30</Duration>"
                    + "<TrackingEvents>"
                    + "<Tracking event=\"start\"><![CDATA[https://example.com/start]]></Tracking>"
                    + "<Tracking event=\"complete\"><![CDATA[https://example.com/complete]]></Tracking>"
                    + "<Tracking event=\"skip\"><![CDATA[https://example.com/skip]]></Tracking>"
                    + "</TrackingEvents>"
                    + "<VideoClicks>"
                    + "<ClickThrough><![CDATA[https://www.google.com]]></ClickThrough>"
                    + "<ClickTracking><![CDATA[https://example.com/clicktracking]]></ClickTracking>"
                    + "</VideoClicks>"
                    + "<MediaFiles>"
                    + "<MediaFile delivery=\"progressive\" type=\"video/mp4\" width=\"1280\" height=\"720\" "
                    + "bitrate=\"1500\">"
                    + "<![CDATA[https://storage.googleapis.com/interactive-media-ads/media/big_buck_bunny.mp4]]>"
                    + "</MediaFile>"
                    + "</MediaFiles>"
                    + "</Linear>"
                    + "</Creative>"
                    + "<Creative sequence=\"2\">"
                    + "<CompanionAds>"
                    + "<Companion width=\"1280\" height=\"720\">"
                    + "<StaticResource creativeType=\"image/jpeg\">"
                    + "<![CDATA[https://www.gstatic.com/webp/gallery/3.jpg]]>"
                    + "</StaticResource>"
                    + "<CompanionClickThrough><![CDATA[https://www.google.com]]></CompanionClickThrough>"
                    + "</Companion>"
                    + "</CompanionAds>"
                    + "</Creative>"
                    + "</Creatives>"
                    + "</InLine></Ad>"
                    + "</VAST>";
}
