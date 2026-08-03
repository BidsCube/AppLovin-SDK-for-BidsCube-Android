package com.applovin.mediation.adapters;

import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class BidscubeServerParametersTest {

    @Test
    public void missingParameterDefaultsFalse() {
        assertFalse(BidscubeServerParameters.readAutoClose(null));
        assertFalse(BidscubeServerParameters.readAutoClose(new Bundle()));
    }

    @Test
    public void autoCloseTrueVariants() {
        Bundle bundle = new Bundle();
        bundle.putString("auto_close", "true");
        assertTrue(BidscubeServerParameters.readAutoClose(bundle));

        bundle = new Bundle();
        bundle.putString("auto_close", "1");
        assertTrue(BidscubeServerParameters.readAutoClose(bundle));
    }

    @Test
    public void autoCloseFalseVariants() {
        Bundle bundle = new Bundle();
        bundle.putString("auto_close", "false");
        assertFalse(BidscubeServerParameters.readAutoClose(bundle));

        bundle = new Bundle();
        bundle.putString("auto_close", "0");
        assertFalse(BidscubeServerParameters.readAutoClose(bundle));
    }

    @Test
    public void autoCloseAlias() {
        Bundle bundle = new Bundle();
        bundle.putString("autoClose", "true");
        assertTrue(BidscubeServerParameters.readAutoClose(bundle));
    }

    @Test
    public void invalidValueDefaultsFalse() {
        Bundle bundle = new Bundle();
        bundle.putString("auto_close", "maybe");
        assertFalse(BidscubeServerParameters.readAutoClose(bundle));
    }

    @Test
    public void emptyStringDefaultsFalse() {
        Bundle bundle = new Bundle();
        bundle.putString("auto_close", "");
        assertFalse(BidscubeServerParameters.readAutoClose(bundle));
    }
}
