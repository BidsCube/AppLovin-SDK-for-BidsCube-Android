package com.bidscube.sdk.models;

import androidx.annotation.NonNull;

import com.bidscube.sdk.ads.AdType;
import com.bidscube.sdk.models.enums.AdPosition;
import com.bidscube.sdk.models.natives.NativeAd;

/**
 * Payload passed to {@link com.bidscube.sdk.interfaces.AdCallback#onAdRenderOverride(AdRenderContext)}
 * so that host apps can render the provided ADM or native model using their own UI.
 */
public final class AdRenderContext {
    private final String placementId;
    private final String adm;
    private final AdPosition position;
    private final AdType.Type renderType;
    private final NativeAd nativeAd; // may be null

    public AdRenderContext(
            @NonNull String placementId,
            String adm,
            AdPosition position,
            AdType.Type renderType
    ) {
        this(placementId, adm, position, renderType, null);
    }

    /**
     * New constructor that carries an optional parsed NativeAd model (may be null).
     */
    public AdRenderContext(
            @NonNull String placementId,
            String adm,
            AdPosition position,
            AdType.Type renderType,
            NativeAd nativeAd
    ) {
        this.placementId = placementId;
        this.adm = adm;
        this.position = position;
        this.renderType = renderType;
        this.nativeAd = nativeAd;
    }

    @NonNull
    public String getPlacementId() {
        return placementId;
    }

    public String getAdm() {
        return adm;
    }

    public AdPosition getPosition() {
        return position;
    }

    public AdType.Type getRenderType() {
        return renderType;
    }

    /**
     * When a native ad was parsed, this returns the typed model; otherwise null.
     */
    public NativeAd getNativeAd() {
        return nativeAd;
    }
}
