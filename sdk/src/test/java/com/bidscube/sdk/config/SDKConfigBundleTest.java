package com.bidscube.sdk.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SDKConfigBundleTest {

    @Test
    public void appIdOverride_keepsHostPackageForBundleParam() {
        SDKConfig config = new SDKConfig.Builder(null)
                .appId("5421")
                .packageName("com.openmygames.magicwordsearch")
                .appName("Magic Word Search")
                .build();

        assertEquals("5421", config.getAppId());
        assertEquals("com.openmygames.magicwordsearch", config.getPackageName());
        assertEquals("com.openmygames.magicwordsearch", config.getRequestBundle());
        assertNotEquals(config.getAppId(), config.getRequestBundle());
    }

    @Test
    public void requestBundle_fallsBackToAppIdWhenPackageUnknown() {
        SDKConfig config = new SDKConfig.Builder(null).appId("5421").build();
        assertEquals("5421", config.getRequestBundle());
    }
}
