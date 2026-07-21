package com.bidscube.sdk.doctor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured runtime SDK Doctor report. Does not contain PII.
 */
public final class SdkDoctorReport {

    public final String sdkVersion;
    public final String adapterVersion;
    public final long generatedAtMillis;
    public final List<SdkDoctorCheck> checks;

    public SdkDoctorReport(String sdkVersion, String adapterVersion, long generatedAtMillis,
            List<SdkDoctorCheck> checks) {
        this.sdkVersion = sdkVersion != null ? sdkVersion : "";
        this.adapterVersion = adapterVersion != null ? adapterVersion : "";
        this.generatedAtMillis = generatedAtMillis;
        this.checks = checks != null
                ? Collections.unmodifiableList(new ArrayList<>(checks))
                : Collections.emptyList();
    }

    public boolean hasFailures() {
        for (SdkDoctorCheck check : checks) {
            if (check.status == SdkDoctorStatus.FAIL) {
                return true;
            }
        }
        return false;
    }

    public String toHumanReadableString() {
        return SdkDoctorFormatter.toHumanReadable(this);
    }

    public String toJson() {
        return SdkDoctorFormatter.toJson(this);
    }
}
