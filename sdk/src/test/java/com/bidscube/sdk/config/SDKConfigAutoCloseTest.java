package com.bidscube.sdk.config;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SDKConfigAutoCloseTest {

    @Test
    public void defaultAutoCloseIsFalse() {
        SDKConfig config = new SDKConfig.Builder(null).appId("test").build();
        assertFalse(config.isAutoClose());
    }

    @Test
    public void explicitAutoCloseTrue() {
        SDKConfig config = new SDKConfig.Builder(null).appId("test").autoClose(true).build();
        assertTrue(config.isAutoClose());
    }

    @Test
    public void explicitAutoCloseFalse() {
        SDKConfig config = new SDKConfig.Builder(null).appId("test").autoClose(false).build();
        assertFalse(config.isAutoClose());
    }

    @Test
    public void withUserIdPreservesAutoClose() {
        SDKConfig original = new SDKConfig.Builder(null).appId("test").autoClose(true).build();
        SDKConfig copy = original.withUserId("user-1");
        assertTrue(copy.isAutoClose());
        assertTrue(original.isAutoClose());
    }
}
