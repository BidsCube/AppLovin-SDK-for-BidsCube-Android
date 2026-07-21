package com.bidscube.sdk.doctor;

/**
 * Formats {@link SdkDoctorReport} for logs and tooling.
 */
public final class SdkDoctorFormatter {

    private SdkDoctorFormatter() {
    }

    public static String toHumanReadable(SdkDoctorReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bidscube SDK Doctor\n");
        sb.append("SDK version: ").append(report.sdkVersion).append('\n');
        if (report.adapterVersion != null && !report.adapterVersion.isEmpty()) {
            sb.append("Adapter version: ").append(report.adapterVersion).append('\n');
        }
        sb.append('\n');
        for (SdkDoctorCheck check : report.checks) {
            sb.append('[').append(check.status.name()).append("] ").append(check.title).append('\n');
            if (check.message != null && !check.message.isEmpty()) {
                sb.append("       ").append(check.message).append('\n');
            }
            if (check.fix != null && !check.fix.isEmpty()) {
                sb.append("       Fix: ").append(check.fix).append('\n');
            }
        }
        return sb.toString().trim();
    }

    public static String toJson(SdkDoctorReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"sdkVersion\":").append(jsonString(report.sdkVersion)).append(',');
        sb.append("\"adapterVersion\":").append(jsonString(report.adapterVersion)).append(',');
        sb.append("\"generatedAtMillis\":").append(report.generatedAtMillis).append(',');
        sb.append("\"checks\":[");
        for (int i = 0; i < report.checks.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            SdkDoctorCheck c = report.checks.get(i);
            sb.append('{');
            sb.append("\"id\":").append(jsonString(c.id)).append(',');
            sb.append("\"status\":").append(jsonString(c.status.name())).append(',');
            sb.append("\"severity\":").append(jsonString(c.severity.name())).append(',');
            sb.append("\"title\":").append(jsonString(c.title)).append(',');
            sb.append("\"message\":").append(jsonString(c.message)).append(',');
            sb.append("\"fix\":").append(jsonString(c.fix));
            sb.append('}');
        }
        sb.append(']');
        sb.append('}');
        return sb.toString();
    }

    static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
