package com.bidscube.sdk.doctor;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.bidscube.sdk.BidscubeSDK;
import com.bidscube.sdk.BuildConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime SDK Doctor — local diagnostics only. No network, no PII, no advertising ID.
 */
public final class SdkDoctor {

    private SdkDoctor() {
    }

    public static SdkDoctorReport run(Context context, SdkDoctorOptions options) {
        if (context == null) {
            List<SdkDoctorCheck> checks = new ArrayList<>();
            checks.add(SdkDoctorCheck.fail("context", "Context missing",
                    "Context is null.", "Pass a valid Application or Activity context."));
            return new SdkDoctorReport(BuildConfig.SDK_VERSION_NAME, "", System.currentTimeMillis(), checks);
        }

        Context appContext = context.getApplicationContext();
        SdkDoctorOptions opts = options != null ? options : SdkDoctorOptions.defaults();
        List<SdkDoctorCheck> checks = new ArrayList<>();

        addEnvironmentChecks(appContext, checks);
        addManifestChecks(appContext, checks);
        addInitializationChecks(appContext, checks);
        addMediationClassChecks(opts, checks);
        addVideoModeChecks(checks);
        addPreloadApiChecks(checks);
        addOpenRtbStatusChecks(checks);
        if (opts.includeConsentChecks) {
            addConsentChecks(checks);
        }

        String adapterVersion = BidscubeSDK.getMediationAdapterVersion();
        return new SdkDoctorReport(BidscubeSDK.getSdkVersion(), adapterVersion,
                System.currentTimeMillis(), checks);
    }

    private static void addEnvironmentChecks(Context context, List<SdkDoctorCheck> checks) {
        checks.add(SdkDoctorCheck.info("env_android_sdk",
                "Android SDK level",
                "API " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ")"));

        try {
            String packageName = context.getPackageName();
            checks.add(SdkDoctorCheck.info("env_package", "Package name", packageName));
        } catch (Exception e) {
            checks.add(SdkDoctorCheck.warn("env_package", "Package name unavailable",
                    e.getMessage(), "Ensure context is attached to an application."));
        }

        checks.add(SdkDoctorCheck.info("env_sdk_version", "SDK artifact version",
                BidscubeSDK.getSdkVersion()));

        String adapterVersion = BidscubeSDK.getMediationAdapterVersion();
        if (adapterVersion != null && !adapterVersion.isEmpty()) {
            checks.add(SdkDoctorCheck.info("env_adapter_version", "Mediation adapter version",
                    adapterVersion));
        } else {
            checks.add(SdkDoctorCheck.info("env_adapter_version", "Mediation adapter version",
                    "Not set (direct SDK mode or adapter did not call setMediationAdapterVersion)."));
        }

        if (context instanceof Activity) {
            checks.add(SdkDoctorCheck.pass("env_context", "Context type", "Activity context available."));
        } else {
            checks.add(SdkDoctorCheck.info("env_context", "Context type",
                    "Application context — set display Activity before showing full-screen ads in MAX."));
        }
    }

    private static void addManifestChecks(Context context, List<SdkDoctorCheck> checks) {
        boolean internet = hasPermission(context, "android.permission.INTERNET");
        if (internet) {
            checks.add(SdkDoctorCheck.pass("manifest_internet", "INTERNET permission",
                    "android.permission.INTERNET is declared."));
        } else {
            checks.add(SdkDoctorCheck.fail("manifest_internet", "INTERNET permission missing",
                    "android.permission.INTERNET is required.",
                    "Add <uses-permission android:name=\"android.permission.INTERNET\" /> to AndroidManifest.xml."));
        }

        boolean networkState = hasPermission(context, "android.permission.ACCESS_NETWORK_STATE");
        if (networkState) {
            checks.add(SdkDoctorCheck.pass("manifest_network_state", "ACCESS_NETWORK_STATE permission",
                    "Optional network state permission is declared."));
        } else {
            checks.add(SdkDoctorCheck.info("manifest_network_state", "ACCESS_NETWORK_STATE permission",
                    "Not declared (optional for most integrations)."));
        }
    }

    private static void addInitializationChecks(Context context, List<SdkDoctorCheck> checks) {
        if (BidscubeSDK.isInitialized()) {
            checks.add(SdkDoctorCheck.pass("sdk_initialized", "SDK initialized",
                    "BidscubeSDK.isInitialized() returned true."));
        } else {
            checks.add(SdkDoctorCheck.fail("sdk_initialized", "SDK not initialized",
                    "BidscubeSDK.isInitialized() returned false.",
                    "Call BidscubeSDK.initialize(context, config) before loading ads."));
        }

        if (context instanceof Activity) {
            checks.add(SdkDoctorCheck.pass("display_activity", "Display Activity",
                    "Activity context can be used for full-screen ads."));
        } else {
            checks.add(SdkDoctorCheck.warn("display_activity", "Display Activity not bound",
                    "Use BidscubeSDK.setDisplayActivity(activity) before show in MAX mediation.",
                    "Pass the hosting Activity from MAX show/load callbacks."));
        }
    }

    private static void addMediationClassChecks(SdkDoctorOptions opts, List<SdkDoctorCheck> checks) {
        boolean applovin = classPresent("com.applovin.sdk.AppLovinSdk");
        if (applovin) {
            checks.add(SdkDoctorCheck.pass("class_applovin_sdk", "AppLovin SDK class",
                    "com.applovin.sdk.AppLovinSdk found."));
        } else if (opts.expectMaxMediation) {
            checks.add(SdkDoctorCheck.fail("class_applovin_sdk", "AppLovin SDK class missing",
                    "com.applovin.sdk.AppLovinSdk not on classpath.",
                    "Add com.applovin:applovin-sdk for MAX mediation."));
        } else {
            checks.add(SdkDoctorCheck.warn("class_applovin_sdk", "AppLovin SDK class missing",
                    "Not required for direct Bidscube SDK integration.",
                    "Add AppLovin SDK only for MAX mediation."));
        }

        boolean maxAd = classPresent("com.applovin.mediation.MaxAd");
        if (maxAd) {
            checks.add(SdkDoctorCheck.pass("class_max_ad", "MAX mediation API",
                    "com.applovin.mediation.MaxAd found."));
        } else if (opts.expectMaxMediation) {
            checks.add(SdkDoctorCheck.warn("class_max_ad", "MAX mediation API missing",
                    "com.applovin.mediation.MaxAd not found.",
                    "Verify AppLovin MAX SDK dependency."));
        }

        boolean adapter = classPresent("com.applovin.mediation.adapters.BidscubeMediationAdapter");
        if (adapter) {
            checks.add(SdkDoctorCheck.pass("class_bidscube_adapter", "BidscubeMediationAdapter",
                    "com.applovin.mediation.adapters.BidscubeMediationAdapter found."));
        } else if (opts.expectMaxMediation) {
            checks.add(SdkDoctorCheck.fail("class_bidscube_adapter", "BidscubeMediationAdapter missing",
                    "MAX custom adapter class not on classpath.",
                    "Add com.bidscube:applovin-bidscube-max-adapter-* matching your video mode."));
        } else {
            checks.add(SdkDoctorCheck.info("class_bidscube_adapter", "BidscubeMediationAdapter",
                    "Not on classpath (expected for direct SDK-only apps)."));
        }
    }

    private static void addVideoModeChecks(List<SdkDoctorCheck> checks) {
        boolean noOp = classPresent("com.bidscube.sdk.video.NoOpVastVideoPlayer");
        boolean webView = classPresent("com.bidscube.sdk.video.WebViewVastVideoPlayer");
        boolean legacy = classPresent("com.bidscube.sdk.video.ProgressiveMp4VastVideoPlayer");
        boolean ima = classPresent("com.bidscube.sdk.view.IMAPlayerHandler");
        boolean imaSdk = classPresent("com.google.ads.interactivemedia.v3.api.ImaSdkFactory");

        if (noOp && !webView && !legacy && !ima) {
            checks.add(SdkDoctorCheck.info("video_mode", "Video mode artifact",
                    "liteNoVideo — video ads fail gracefully; use image/banner formats."));
        } else if (webView) {
            checks.add(SdkDoctorCheck.pass("video_mode", "Video mode artifact",
                    "webViewVideo — WebView VAST player classes present."));
        } else if (legacy && !ima) {
            checks.add(SdkDoctorCheck.pass("video_mode", "Video mode artifact",
                    "legacyMediaVideo — progressive MP4 / legacy media player path."));
        } else if (ima || imaSdk) {
            checks.add(SdkDoctorCheck.pass("video_mode", "Video mode artifact",
                    "fullVideo — Google IMA-based VAST playback classes present."));
            if (!imaSdk) {
                checks.add(SdkDoctorCheck.warn("video_mode_ima_sdk", "Google IMA SDK",
                        "IMAPlayerHandler present but com.google.ads.interactivemedia.v3.api.ImaSdkFactory not found.",
                        "Ensure Google IMA dependency is bundled for tag-based VAST."));
            }
            checks.add(SdkDoctorCheck.warn("video_mode_desugar", "Desugaring risk",
                    "Full video mode may require core library desugaring depending on dependency graph.",
                    "If Gradle reports checkReleaseAarMetadata error, enable coreLibraryDesugaring."));
        } else {
            checks.add(SdkDoctorCheck.warn("video_mode", "Video mode unknown",
                    "Could not classify video artifact from classpath.",
                    "Verify you ship one Bidscube SDK video flavor only."));
        }
    }

    private static void addPreloadApiChecks(List<SdkDoctorCheck> checks) {
        checkStaticMethod(checks, "preload_image_ad", "preloadImageAd", String.class,
                com.bidscube.sdk.interfaces.AdCallback.class);
        checkStaticMethod(checks, "preload_interstitial", "preloadInterstitialVideoAd", String.class,
                com.bidscube.sdk.interfaces.AdCallback.class);
        checkStaticMethod(checks, "preload_rewarded", "preloadRewardedVideoAd", String.class,
                com.bidscube.sdk.interfaces.AdCallback.class);
        checkStaticMethod(checks, "clear_preload_cache", "clearPreloadCache");
        checkStaticMethod(checks, "collect_signal", "collectSignal");
    }

    private static void addOpenRtbStatusChecks(List<SdkDoctorCheck> checks) {
        boolean parserPresent = classPresent("com.bidscube.sdk.openrtb.OpenRtbResponseNormalizer")
                || classPresent("com.bidscube.sdk.openrtb.VideoAdPayloadResolver");
        if (parserPresent) {
            checks.add(SdkDoctorCheck.info("openrtb", "OpenRTB parser classes",
                    "OpenRTB response parser package detected in this SDK build."));
        } else {
            checks.add(SdkDoctorCheck.warn("openrtb_response", "OpenRTB 2.6 response parsing",
                    "Not implemented in this SDK build.",
                    "Do not expect bids[] / seatbid[] / podded video parsing."));
            checks.add(SdkDoctorCheck.info("openrtb_post", "Full OpenRTB POST bid requests",
                    "Not implemented — SDK uses regular GET /sdk request flow."));
        }
    }

    private static void addConsentChecks(List<SdkDoctorCheck> checks) {
        if (!BidscubeSDK.isInitialized()) {
            checks.add(SdkDoctorCheck.info("consent", "Consent API",
                    "Skipped — SDK not initialized."));
            return;
        }
        try {
            boolean required = BidscubeSDK.isConsentRequired();
            boolean ads = BidscubeSDK.hasAdsConsent();
            boolean analytics = BidscubeSDK.hasAnalyticsConsent();
            String summary = BidscubeSDK.getConsentStatusSummary();
            checks.add(SdkDoctorCheck.info("consent_status", "Consent summary",
                    "required=" + required + " adsConsent=" + ads + " analyticsConsent=" + analytics
                            + (summary != null ? " (" + summary + ")" : "")));
        } catch (Exception e) {
            checks.add(SdkDoctorCheck.warn("consent_status", "Consent API error",
                    e.getMessage(), "Initialize SDK and UMP before reading consent."));
        }
    }

    private static boolean hasPermission(Context context, String permission) {
        try {
            PackageInfo info = getPackageInfo(context);
            if (info == null || info.requestedPermissions == null) {
                return false;
            }
            for (String p : info.requestedPermissions) {
                if (permission.equals(p)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static PackageInfo getPackageInfo(Context context) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        String pkg = context.getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        } else {
            //noinspection deprecation
            return pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
        }
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void checkStaticMethod(List<SdkDoctorCheck> checks, String id, String methodName,
            Class<?>... paramTypes) {
        try {
            Method m = BidscubeSDK.class.getMethod(methodName, paramTypes);
            if (m != null) {
                checks.add(SdkDoctorCheck.pass("api_" + id, "BidscubeSDK." + methodName + "()",
                        "Method available on runtime SDK."));
            }
        } catch (NoSuchMethodException e) {
            checks.add(SdkDoctorCheck.fail("api_" + id, "Missing SDK API",
                    "BidscubeSDK." + methodName + " not found.",
                    "Upgrade to a current Bidscube SDK artifact (adapter/SDK version mismatch)."));
        }
    }
}
