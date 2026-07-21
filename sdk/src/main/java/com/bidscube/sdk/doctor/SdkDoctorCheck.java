package com.bidscube.sdk.doctor;

/**
 * One diagnostic check in an {@link SdkDoctorReport}.
 */
public final class SdkDoctorCheck {

    public final String id;
    public final SdkDoctorStatus status;
    public final SdkDoctorSeverity severity;
    public final String title;
    public final String message;
    public final String fix;

    public SdkDoctorCheck(String id, SdkDoctorStatus status, SdkDoctorSeverity severity,
            String title, String message, String fix) {
        this.id = id != null ? id : "";
        this.status = status != null ? status : SdkDoctorStatus.INFO;
        this.severity = severity != null ? severity : SdkDoctorSeverity.LOW;
        this.title = title != null ? title : "";
        this.message = message != null ? message : "";
        this.fix = fix != null ? fix : "";
    }

    public static SdkDoctorCheck pass(String id, String title, String message) {
        return new SdkDoctorCheck(id, SdkDoctorStatus.PASS, SdkDoctorSeverity.LOW, title, message, "");
    }

    public static SdkDoctorCheck warn(String id, String title, String message, String fix) {
        return new SdkDoctorCheck(id, SdkDoctorStatus.WARN, SdkDoctorSeverity.MEDIUM, title, message, fix);
    }

    public static SdkDoctorCheck fail(String id, String title, String message, String fix) {
        return new SdkDoctorCheck(id, SdkDoctorStatus.FAIL, SdkDoctorSeverity.CRITICAL, title, message, fix);
    }

    public static SdkDoctorCheck info(String id, String title, String message) {
        return new SdkDoctorCheck(id, SdkDoctorStatus.INFO, SdkDoctorSeverity.LOW, title, message, "");
    }
}
