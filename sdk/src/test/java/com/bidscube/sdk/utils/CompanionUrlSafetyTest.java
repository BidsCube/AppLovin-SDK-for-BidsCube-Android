package com.bidscube.sdk.utils;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CompanionUrlSafetyTest {

    @Test
    public void blocksUnsafeSchemes() {
        assertTrue(CompanionUrlSafety.isBlockedScheme(Uri.parse("javascript:alert(1)")));
        assertTrue(CompanionUrlSafety.isBlockedScheme(Uri.parse("file:///etc/passwd")));
        assertTrue(CompanionUrlSafety.isBlockedScheme(Uri.parse("content://media/external")));
        assertTrue(CompanionUrlSafety.isBlockedScheme(Uri.parse("data:text/html,hello")));
    }

    @Test
    public void allowsHttpHttps() {
        assertTrue(CompanionUrlSafety.isHttpOrHttps(Uri.parse("https://example.com")));
        assertTrue(CompanionUrlSafety.isHttpOrHttps(Uri.parse("http://example.com")));
        assertFalse(CompanionUrlSafety.isHttpOrHttps(Uri.parse("javascript:void(0)")));
    }
}
