package com.bidscube.sdk.models.natives;

import java.util.List;

/**
 * Represents an event tracker for OpenRTB Native Ads specification (version 1.2).
 * This object contains event type, tracking method, and URLs to fire on that event.
 */
public class EventTracker {

    /**
     * Event type:
     * 1 = Impression
     * 2 = Viewable MRC50
     * 3 = Viewable MRC100
     * 4 = Viewable Video50
     */
    private int event;

    /**
     * Method:
     * 1 = Image (img)
     * 2 = JavaScript (js)
     */
    private int method;

    /**
     * List of tracking URLs that will be triggered when the event occurs.
     */
    private List<String> url;

    public EventTracker() {}

    public EventTracker(int event, int method, List<String> url) {
        this.event = event;
        this.method = method;
        this.url = url;
    }

    public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public List<String> getUrl() {
        return url;
    }

    public void setUrl(List<String> url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "EventTracker{" +
                "event=" + event +
                ", method=" + method +
                ", url=" + url +
                '}';
    }
}

