package com.bidscube.sdk.models.natives;

import java.util.ArrayList;
import java.util.List;

public class NativeLink {
    public String url;
    public String fallback;
    public final List<String> clicktrackers = new ArrayList<>();
}