# Bidscube SDK consumer ProGuard rules: keep public API for app consumers
-keep class com.bidscube.sdk.BidscubeSDK { *; }
-keep class com.bidscube.sdk.BuildConfig { *; }
-keep class com.bidscube.sdk.config.SDKConfig { *; }
-keep class com.bidscube.sdk.config.SDKConfig$Builder { *; }
-keep interface com.bidscube.sdk.interfaces.** { *; }

# Glide (SDK loads creatives); safe keeps for R8 in consuming apps
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideImpl { *; }
