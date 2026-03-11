package com.bidscube.sdk.device.providers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.bidscube.sdk.utils.SDKLogger;

import androidx.core.util.Consumer;

import com.bidscube.sdk.config.SDKConfig;
import com.bidscube.sdk.consent.ConsentManager;
import com.bidscube.sdk.models.AdInfo;
import com.bidscube.sdk.models.DeviceInfo;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

public class DeviceInfoProvider {

    private final Context context;
    private final ConsentManager consentManager;
    private final SDKConfig config;

    public DeviceInfoProvider(Context context, SDKConfig config) {
        this.context = context.getApplicationContext();
        this.consentManager = new ConsentManager(context);
        this.config = config;
    }

    public void getDeviceInfoAsync(Consumer<DeviceInfo> callback) {
        String bundle = config.getAppId();
        String appName = config.getAppName();
        String appStoreUrl = "https://play.google.com/store/apps/details?id=" + bundle;
        String language = config.getLanguage();
        DisplayMetrics metrics = getDisplayMetrics();
        int deviceWidth = metrics.widthPixels;
        int deviceHeight = metrics.heightPixels;
        String userAgent = config.getUserAgent();
        String appVersion = config.getAppVersion();

        int gdprApplies = config.getGdpr() != null ? config.getGdpr() : consentManager.getGdprApplies();
        String consentString = config.getGdprConsent() != null ? config.getGdprConsent()
                : consentManager.getGdprConsentString();
        String addtlConsent = consentManager.getAdditionalConsent();
        String gppString = consentManager.getGppString();
        String usPrivacy = config.getUsPrivacy() != null ? config.getUsPrivacy() : consentManager.getUsPrivacyString();
        boolean coppa = config.getCoppa() != null ? config.getCoppa() : false;

        SDKLogger.d("DeviceInfoProvider", "GDPR Applies: " + gdprApplies);
        SDKLogger.d("DeviceInfoProvider", "Consent String: " + consentString);
        SDKLogger.d("DeviceInfoProvider", "Additional Consent: " + addtlConsent);
        SDKLogger.d("DeviceInfoProvider", "GPP String: " + gppString);
        SDKLogger.d("DeviceInfoProvider", "US Privacy: " + usPrivacy);
        SDKLogger.d("DeviceInfoProvider", "COPPA: " + coppa);

        new Thread(() -> {
            AdInfo adInfo;
            try {
                AdvertisingIdClient.Info idInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                adInfo = new AdInfo(idInfo.getId(), idInfo.isLimitAdTrackingEnabled());
            } catch (Exception e) {
                SDKLogger.e("DeviceInfoProvider", "Failed to get Advertising ID", e);
                adInfo = new AdInfo(null, false);
            }

            final int dnt = adInfo.isLimitAdTracking() ? 1 : 0;
            final String ifa = adInfo.getAdId();

            DeviceInfo deviceInfo = new DeviceInfo(
                    bundle,
                    appName,
                    appStoreUrl,
                    language,
                    deviceWidth,
                    deviceHeight,
                    userAgent,
                    ifa,
                    dnt,
                    appVersion,
                    gdprApplies,
                    consentString,
                    usPrivacy,
                    coppa);

            new Handler(Looper.getMainLooper()).post(() -> callback.accept(deviceInfo));
        }).start();
    }

    /**
     * Get device info with current consent status
     * This method can be called after consent has been updated
     */
    public void getDeviceInfoWithCurrentConsent(Consumer<DeviceInfo> callback) {

        getDeviceInfoAsync(callback);
    }

    /**
     * Get the ConsentManager instance for direct access
     *
     * @return ConsentManager instance
     */
    public ConsentManager getConsentManager() {
        return consentManager;
    }

    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null && wm.getDefaultDisplay() != null) {
            try {
                wm.getDefaultDisplay().getRealMetrics(metrics);
            } catch (Exception e) {
                wm.getDefaultDisplay().getMetrics(metrics);
            }
        } else {
            metrics.setTo(context.getResources().getDisplayMetrics());
        }
        return metrics;
    }
}