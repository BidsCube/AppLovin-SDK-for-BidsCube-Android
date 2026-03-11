package com.bidscube.sdk.models.natives;

import java.util.ArrayList;
import java.util.List;

public class NativeAd {

    public String ver;
    public List<NativeAsset> assets = new ArrayList<>();
    public NativeLink link;
    public List<String> imptrackers = new ArrayList<>();
    public List<EventTracker> eventtrackers = new ArrayList<>();
    public String jstracker;
    public String assetsurl;
    public String dcourl;
    public String privacy;
}
