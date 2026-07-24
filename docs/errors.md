# Bidscube SDK — error codes

**SDK 1.2.11+**

When an ad request or display fails, the SDK invokes `AdCallback.onAdFailed(placementId, errorCode, errorMessage)`.  
All `errorMessage` strings are **English** and safe to log or forward to mediation adapters.

Constants live in `com.bidscube.sdk.errors.AdErrorCode`.

---

## Error code reference

| Code | Constant | Summary | Typical cause |
|------|----------|---------|---------------|
| **-1** | `UNKNOWN` | Unknown error | Unclassified failure; check `errorMessage` |
| **204** | `NO_FILL` | No ad fill | SSP returned HTTP **204 No Content** (no demand for this request) |
| **1001** | `HTTP_ERROR` | Ad server HTTP error | Non-2xx HTTP status (except 204) |
| **1002** | `INVALID_RESPONSE` | Invalid ad server response | Response body could not be parsed |
| **1003** | `EMPTY_ADM` | Empty ad markup | HTTP 200 but ADM field is empty |
| **1004** | `NO_ACTIVITY_CONTEXT` | Activity context required | SDK initialized with `Application` context and no Activity was bound before show |
| **1005** | `NETWORK_ERROR` | Network error | Timeout, connection failure, I/O exception |
| **1006** | `DISPLAY_ERROR` | Ad display error | Unexpected failure while building or showing the ad UI |

Use `AdErrorCode.describe(errorCode)` for a short English label in logs.

---

## Example messages

| Code | Example `errorMessage` |
|------|------------------------|
| 204 | `No ad fill: ad server returned HTTP 204 (No Content)` |
| 1001 | `HTTP error: 500 — …` |
| 1002 | `Failed to parse ad server response` |
| 1004 | `Activity context is required to display ads. Pass an Activity when showing ads (for example from the MAX adapter show callback).` |
| 1005 | `Network error: Unable to resolve host …` |

---

## AppLovin MAX mediation

The Bidscube adapter forwards SDK failures to MAX **display failed** callbacks (`onInterstitialAdDisplayFailed`, `onRewardedAdDisplayFailed`, etc.) using the same numeric `errorCode` and English `errorMessage`.

Common MAX integration notes:

- **`204` / NO_FILL** — not a crash; Bidscube had no ad for the placement. Verify placement ID, app registration, and bundle ID on the Bidscube side.
- **`1004` / NO_ACTIVITY_CONTEXT** — should not occur on **1.2.7+** when using the official adapter (Activity is bound on show). Upgrade if you see this on an older SDK build.
- **`1006` / display error with `VideoAdPlayer` not found** — host app missing Google IMA; upgrade to **1.2.8+** for progressive MP4 VAST without IMA on the classpath.

---

## Standalone SDK

```java
BidscubeSDK.showInterstitialVideoAd("21492", new AdCallback() {
    @Override
    public void onAdFailed(String placementId, int errorCode, String errorMessage) {
        Log.e("Bidscube", "placement=" + placementId
                + " code=" + errorCode
                + " (" + AdErrorCode.describe(errorCode) + ")"
                + " msg=" + errorMessage);
    }
    // … other callbacks
});
```

When initializing with `Application` context, call `BidscubeSDK.setDisplayActivity(activity)` before show methods if you are not using the MAX adapter.

---

## Related

- [Main README](../README.md)
- [AppLovin MAX adapter README](../applovin-adapter/README.md)
- [CHANGELOG](../CHANGELOG.md)
