package com.bidscube.sdk.view;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BannerAdTraceTest {

    private static final String IMP_URL =
            "https://ssp-bcc-ads.com/sdk?c=b&m=i&h=abc123token456";

    @Test
    public void networkRequest_countsFirstImpRequest() {
        BannerAdTrace trace = BannerAdTrace.start("21980", "https://ssp-bcc-ads.com/sdk");
        trace.networkRequest(IMP_URL, false);
        assertEquals(1, trace.impRequestCount());
    }

    @Test
    public void networkRequest_deduplicatesSameImpToken() {
        BannerAdTrace trace = BannerAdTrace.start("21980", "https://ssp-bcc-ads.com/sdk");
        trace.networkRequest(IMP_URL, false);
        trace.networkRequest(IMP_URL + "&cacheBust=1", false);
        assertEquals(1, trace.impRequestCount());
    }

    @Test
    public void networkRequest_countsDistinctImpTokens() {
        BannerAdTrace trace = BannerAdTrace.start("21980", "https://ssp-bcc-ads.com/sdk");
        trace.networkRequest(IMP_URL, false);
        trace.networkRequest("https://ssp-bcc-ads.com/sdk?c=b&m=i&h=otherToken99", false);
        assertEquals(2, trace.impRequestCount());
    }

    @Test
    public void networkRequest_ignoresNonImpUrls() {
        BannerAdTrace trace = BannerAdTrace.start("21980", "https://ssp-bcc-ads.com/sdk");
        trace.networkRequest("https://cdn.example/creative.jpg", false);
        trace.networkRequest("https://ssp-bcc-ads.com/sdk?c=b&m=api", false);
        assertEquals(0, trace.impRequestCount());
    }
}
