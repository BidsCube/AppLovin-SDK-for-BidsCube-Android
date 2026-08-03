package com.bidscube.sdk.utils;

import com.bidscube.sdk.interfaces.AdCallback;
import com.bidscube.sdk.models.CompanionAd;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class CompanionClickHandlerTest {

    @Test
    public void clickCallbackFiresOnce() {
        CompanionAd companion = new CompanionAd(
                CompanionAd.ResourceType.HTML,
                "<html></html>",
                320,
                480,
                "https://click.example",
                Arrays.asList("https://track.example/click1", "https://track.example/click2"),
                Arrays.asList("https://track.example/view"));
        CompanionClickHandler handler = new CompanionClickHandler(companion);
        AtomicInteger clicks = new AtomicInteger();
        AdCallback callback = new AdCallback() {
            @Override
            public void onAdLoading(String placementId) {
            }

            @Override
            public void onAdLoaded(String placementId) {
            }

            @Override
            public void onAdDisplayed(String placementId) {
            }

            @Override
            public void onAdClicked(String placementId) {
                clicks.incrementAndGet();
            }

            @Override
            public void onAdClosed(String placementId) {
            }

            @Override
            public void onAdFailed(String placementId, int errorCode, String errorMessage) {
            }
        };
        handler.handleClick(null, "p1", callback);
        handler.handleClick(null, "p1", callback);
        assertEquals(1, clicks.get());
    }

    @Test
    public void creativeViewTrackingFiresOnce() {
        CompanionAd companion = new CompanionAd(
                CompanionAd.ResourceType.STATIC,
                "https://cdn.example/img.jpg",
                300,
                250,
                "https://click.example",
                Arrays.asList("https://track.example/click"),
                Arrays.asList("https://track.example/view1", "https://track.example/view2"));
        CompanionClickHandler handler = new CompanionClickHandler(companion);
        handler.fireCreativeViewOnce();
        handler.fireCreativeViewOnce();
    }
}
