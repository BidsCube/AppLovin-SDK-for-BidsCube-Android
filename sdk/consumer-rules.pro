# Bidscube SDK consumer ProGuard rules: keep public API for app consumers
-keep class com.bidscube.sdk.BidscubeSDK { *; }
-keep class com.bidscube.sdk.config.SDKConfig { *; }
-keep interface com.bidscube.sdk.interfaces.** { *; }
