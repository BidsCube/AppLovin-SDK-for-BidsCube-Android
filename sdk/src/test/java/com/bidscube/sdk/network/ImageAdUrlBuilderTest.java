package com.bidscube.sdk.network;

import com.bidscube.sdk.models.DeviceInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ImageAdUrlBuilderTest {

    @Test
    public void build_includesHostPackageAsBundle() {
        DeviceInfo deviceInfo = new DeviceInfo(
                "com.publisher.game",
                "Test Game",
                "https://play.google.com/store/apps/details?id=com.publisher.game",
                "en",
                1080,
                1920,
                "Mozilla/5.0 test",
                "test-ifa",
                0,
                "1.0.0",
                0,
                "",
                "",
                false,
                DeviceInfo.DEFAULT_AD_REQUEST_AUTHORITY,
                null);

        String url = new ImageAdUrlBuilder("21980", deviceInfo).build().toString();

        assertTrue(url.contains("bundle=com.publisher.game"));
        assertTrue(url.contains("placementId=21980"));
        assertTrue(url.contains("c=b"));
        assertTrue(url.contains("m=api"));
    }
}
