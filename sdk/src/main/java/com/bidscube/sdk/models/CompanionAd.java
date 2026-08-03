package com.bidscube.sdk.models;

import java.util.Collections;
import java.util.List;

/**
 * Parsed VAST {@code Companion} selected for post-video display. Tracking and click URLs are
 * scoped to the chosen {@code Companion} element only.
 */
public final class CompanionAd {

    public enum ResourceType {
        HTML,
        IFRAME,
        STATIC
    }

    private final ResourceType resourceType;
    private final String resource;
    private final int width;
    private final int height;
    private final String clickThroughUrl;
    private final List<String> clickTrackingUrls;
    private final List<String> creativeViewTrackingUrls;

    public CompanionAd(
            ResourceType resourceType,
            String resource,
            int width,
            int height,
            String clickThroughUrl,
            List<String> clickTrackingUrls,
            List<String> creativeViewTrackingUrls) {
        this.resourceType = resourceType;
        this.resource = resource;
        this.width = width;
        this.height = height;
        this.clickThroughUrl = clickThroughUrl;
        this.clickTrackingUrls = clickTrackingUrls == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(clickTrackingUrls);
        this.creativeViewTrackingUrls = creativeViewTrackingUrls == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(creativeViewTrackingUrls);
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResource() {
        return resource;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getClickThroughUrl() {
        return clickThroughUrl;
    }

    public List<String> getClickTrackingUrls() {
        return clickTrackingUrls;
    }

    public List<String> getCreativeViewTrackingUrls() {
        return creativeViewTrackingUrls;
    }

    public boolean isInteractive() {
        return resourceType == ResourceType.HTML || resourceType == ResourceType.IFRAME;
    }

    public boolean isStaticImage() {
        return resourceType == ResourceType.STATIC;
    }
}
