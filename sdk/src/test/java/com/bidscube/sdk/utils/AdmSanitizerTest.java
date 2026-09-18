package com.bidscube.sdk.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdmSanitizerTest {

    private static final String CREATIVE =
            "<a href=\"https://click.example\"><img width=\"320\" height=\"50\" "
                    + "src=\"https://cdn.example/creative.jpg\"></a>";
    private static final String SSP_PIXEL =
            "<img width=\"1\" height=\"1\" src=\"https://ssp-bcc-ads.com/sdk?c=b&amp;m=i&amp;h=testtoken\">";

    @Test
    public void sanitize_openMyGamesLikeAdm_preservesImpPixel() {
        String adm = CREATIVE + SSP_PIXEL
                + "<img width=\"1\" height=\"1\" src=\"https://tracking.onaudience.com/pixel\">";
        String sanitized = AdmSanitizer.sanitize(adm);
        assertTrue(sanitized.contains("creative.jpg"));
        assertTrue(sanitized.contains("m=i"));
        assertTrue(sanitized.contains("testtoken"));
    }

    @Test
    public void sanitize_documentWriteWithWrapperAndPixels() {
        String html = "document.write('<span></span>');" + SSP_PIXEL
                + "<div id=\"wrapper_abc\">" + CREATIVE + "</div>" + SSP_PIXEL;
        String sanitized = AdmSanitizer.sanitize(html);
        assertFalse(sanitized.contains("document.write"));
        assertTrue(sanitized.contains("creative.jpg"));
        assertTrue(sanitized.contains("m=i"));
    }
}
