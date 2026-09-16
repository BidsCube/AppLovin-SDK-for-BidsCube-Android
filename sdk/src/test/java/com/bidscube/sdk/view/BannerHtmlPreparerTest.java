package com.bidscube.sdk.view;

import com.bidscube.sdk.utils.AdmSanitizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BannerHtmlPreparerTest {

    private static final String CREATIVE =
            "<a href=\"https://click.example\"><img width=\"320\" height=\"50\" "
                    + "src=\"https://cdn.example/creative.jpg\" alt=\"ad\"></a>";
    private static final String SSP_PIXEL =
            "<img width=\"1\" height=\"1\" src=\"https://ssp-bcc-ads.com/sdk?c=b&amp;m=i&amp;h=abc123token\">";
    private static final String AUDIENCE_PIXEL =
            "<img width=\"1\" height=\"1\" src=\"https://tracking.onaudience.com/pixel\">";
    private static final String IMG_W100 =
            "<img width=\"100\" height=\"50\" src=\"https://cdn.example/w100.jpg\">";
    private static final String IMG_H150 =
            "<img width=\"50\" height=\"150\" src=\"https://cdn.example/h150.jpg\">";
    private static final String EXTERNAL_SCRIPT =
            "<script src=\"https://cdn.example/track.js\"></script>";
    private static final String INLINE_SCRIPT =
            "<script>window.__bannerId='bid-99';</script>";

    @Test
    public void legacyStrip_removesOneByOnePixels_butNotMainCreative() {
        String sanitized = CREATIVE + SSP_PIXEL + AUDIENCE_PIXEL;
        assertEquals(3, BannerHtmlPreparer.countImgTags(sanitized));

        String stripped = BannerHtmlPreparer.applyLegacyTrackingStrip(sanitized);
        assertEquals(1, BannerHtmlPreparer.countImgTags(stripped));
        assertTrue(stripped.contains("creative.jpg"));
        assertFalse(stripped.contains("m=i"));
        assertFalse(stripped.contains("onaudience.com"));
    }

    @Test
    public void legacyStrip_falsePositive_removesWidth100DueToMissingBoundary() {
        String sanitized = CREATIVE + IMG_W100 + IMG_H150 + SSP_PIXEL;
        String stripped = BannerHtmlPreparer.applyLegacyTrackingStrip(sanitized);
        // Confirmed legacy defect: width=100 matches width=1 substring in regex.
        assertFalse(stripped.contains("w100.jpg"));
    }

    @Test
    public void prepareFinalHtml_preservesAllThreeImagesFromSampleAdm() {
        String adm = CREATIVE + SSP_PIXEL + AUDIENCE_PIXEL + EXTERNAL_SCRIPT + INLINE_SCRIPT;
        String finalHtml = BannerHtmlPreparer.prepareFinalHtml(adm);

        assertTrue(finalHtml.contains("creative.jpg"));
        assertTrue(finalHtml.contains("m=i"));
        assertTrue(finalHtml.contains("abc123token"));
        assertTrue(finalHtml.contains("onaudience.com"));
        assertTrue(finalHtml.contains("track.js"));
        assertTrue(finalHtml.contains("__bannerId"));
        assertEquals(3, BannerHtmlPreparer.countImgTags(finalHtml));
        assertFalse(finalHtml.contains("display:none !important;width:0"));
    }

    @Test
    public void prepareFinalHtml_preservesWidth100AndHeight150Images() {
        String adm = CREATIVE + IMG_W100 + IMG_H150 + SSP_PIXEL;
        String finalHtml = BannerHtmlPreparer.prepareFinalHtml(adm);
        assertTrue(finalHtml.contains("w100.jpg"));
        assertTrue(finalHtml.contains("h150.jpg"));
        assertTrue(finalHtml.contains("m=i"));
    }

    @Test
    public void documentWriteForm_unpacksAndPreservesPixels() {
        String docWrite = "document.write(\"" + CREATIVE + SSP_PIXEL + "\");";
        String sanitized = BannerHtmlPreparer.prepareSanitizedContent(docWrite);
        assertTrue(sanitized.contains("creative.jpg"));
        assertTrue(sanitized.contains("m=i"));

        String finalHtml = BannerHtmlPreparer.prepareFinalHtml(docWrite);
        assertTrue(finalHtml.contains("creative.jpg"));
        assertTrue(finalHtml.contains("m=i"));
    }

    @Test
    public void documentWritePrefix_preservesMarkupAfterStatement() {
        String html = "document.write('<span id=\"pos\"></span>');" + SSP_PIXEL
                + "<div id=\"wrapper_x\">" + CREATIVE + "</div>";
        String sanitized = AdmSanitizer.sanitize(html);
        assertFalse(sanitized.contains("document.write"));
        assertTrue(sanitized.contains("m=i"));
        assertTrue(sanitized.contains("creative.jpg"));
    }

    @Test
    public void wrapperDiv_preservesTrackingAfterWrapperBlock() {
        String html = "<div id=\"wrapper_abc\">" + CREATIVE + "</div>" + SSP_PIXEL + INLINE_SCRIPT;
        String sanitized = AdmSanitizer.sanitize(html);
        assertTrue("sanitized should keep post-wrapper pixel", sanitized.contains("m=i"));
        assertTrue(sanitized.contains("creative.jpg"));

        String finalHtml = BannerHtmlPreparer.prepareFinalHtml(html);
        assertTrue(finalHtml.contains("m=i"));
        assertTrue(finalHtml.contains("creative.jpg"));
    }

    @Test
    public void wrapperDiv_preservesTrackingBeforeWrapperBlock() {
        String docWriteShell = "document.write('<span id=\"pos\"></span>');";
        String html = docWriteShell + SSP_PIXEL + AUDIENCE_PIXEL
                + "<div id=\"wrapper_abc\">" + CREATIVE + "</div>" + INLINE_SCRIPT;
        String sanitized = AdmSanitizer.sanitize(html);
        assertTrue("sanitized should keep pre-wrapper SSP pixel", sanitized.contains("m=i"));
        assertTrue("sanitized should keep pre-wrapper audience pixel", sanitized.contains("onaudience.com"));
        assertTrue(sanitized.contains("creative.jpg"));
        assertFalse("document.write shell should be dropped", sanitized.contains("document.write"));

        String finalHtml = BannerHtmlPreparer.prepareFinalHtml(html);
        assertTrue(finalHtml.contains("m=i"));
        assertTrue(finalHtml.contains("onaudience.com"));
        assertTrue(finalHtml.contains("creative.jpg"));
        assertEquals(3, BannerHtmlPreparer.countImgTags(finalHtml));
    }

    @Test
    public void wrapperDiv_doesNotDuplicateMainCreativeFromPrefix() {
        String html = CREATIVE + "<div id=\"wrapper_abc\">" + CREATIVE + "</div>" + SSP_PIXEL;
        String sanitized = AdmSanitizer.sanitize(html);
        assertTrue(sanitized.contains("creative.jpg"));
        assertTrue(sanitized.contains("m=i"));
        assertEquals("only wrapper creative + pixels, not prefix banner img",
                2, BannerHtmlPreparer.countImgTags(sanitized));
    }

    @Test
    public void looksLikeVast_detectsVastMarkup() {
        assertTrue(BannerHtmlPreparer.looksLikeVast("<?xml version=\"1.0\"?><VAST version=\"3.0\"></VAST>"));
        assertFalse(BannerHtmlPreparer.looksLikeVast(CREATIVE + SSP_PIXEL));
    }

    @Test
    public void isOneByOneTrackingImgTag_distinguishesOneFromTen() {
        assertTrue(BannerHtmlPreparer.isOneByOneTrackingImgTag("<img width=\"1\" height=\"1\" src=\"x\">"));
        assertFalse(BannerHtmlPreparer.isOneByOneTrackingImgTag("<img width=\"100\" height=\"50\" src=\"x\">"));
        assertFalse(BannerHtmlPreparer.isOneByOneTrackingImgTag("<img width=\"50\" height=\"150\" src=\"x\">"));
    }
}
