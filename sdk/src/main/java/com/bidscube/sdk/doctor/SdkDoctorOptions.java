package com.bidscube.sdk.doctor;

/**
 * Options for {@link SdkDoctor#run(android.content.Context, SdkDoctorOptions)}.
 */
public final class SdkDoctorOptions {

    /** When true, missing AppLovin / adapter classes are FAIL instead of WARN. */
    public final boolean expectMaxMediation;

    /** When true, run consent-related checks (requires initialized SDK). */
    public final boolean includeConsentChecks;

    private SdkDoctorOptions(boolean expectMaxMediation, boolean includeConsentChecks) {
        this.expectMaxMediation = expectMaxMediation;
        this.includeConsentChecks = includeConsentChecks;
    }

    public static SdkDoctorOptions defaults() {
        return new SdkDoctorOptions(false, true);
    }

    public static SdkDoctorOptions maxMediation() {
        return new SdkDoctorOptions(true, true);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean expectMaxMediation = false;
        private boolean includeConsentChecks = true;

        public Builder expectMaxMediation(boolean value) {
            this.expectMaxMediation = value;
            return this;
        }

        public Builder includeConsentChecks(boolean value) {
            this.includeConsentChecks = value;
            return this;
        }

        public SdkDoctorOptions build() {
            return new SdkDoctorOptions(expectMaxMediation, includeConsentChecks);
        }
    }
}
