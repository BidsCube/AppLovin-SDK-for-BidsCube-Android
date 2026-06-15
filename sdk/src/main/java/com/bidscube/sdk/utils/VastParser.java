package com.bidscube.sdk.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public final class VastParser {

    private VastParser() {
    }

    public static String getClickThroughUrl(String vastXml) {
        Document doc = parseDocument(vastXml);
        if (doc == null) {
            return null;
        }
        String fromVideoClicks = getNestedTagText(doc, "VideoClicks", "ClickThrough");
        return !isBlank(fromVideoClicks) ? fromVideoClicks : getFirstTagText(doc, "ClickThrough");
    }

    public static String getVideoClickThroughUrl(String vastXml) {
        return getClickThroughUrl(vastXml);
    }

    public static boolean validateVastStructure(String vastXml) {
        Document doc = parseDocument(vastXml);
        if (doc == null || doc.getDocumentElement() == null) {
            return false;
        }
        if (!"VAST".equalsIgnoreCase(doc.getDocumentElement().getTagName())) {
            return false;
        }
        return !isBlank(getMediaFileUrl(vastXml));
    }

    public static String getMediaFileUrl(String vastXml) {
        Document doc = parseDocument(vastXml);
        if (doc == null) {
            return null;
        }
        NodeList mediaFileNodes = doc.getElementsByTagName("MediaFile");
        for (int i = 0; i < mediaFileNodes.getLength(); i++) {
            String url = normalizeText(mediaFileNodes.item(i));
            if (!isBlank(url)) {
                return url;
            }
        }
        return null;
    }

    public static List<String> getTrackingUrls(String vastXml, String eventName) {
        Document doc = parseDocument(vastXml);
        if (doc == null || isBlank(eventName)) {
            return Collections.emptyList();
        }

        List<String> urls = new ArrayList<>();
        NodeList trackingNodes = doc.getElementsByTagName("Tracking");
        for (int i = 0; i < trackingNodes.getLength(); i++) {
            Node node = trackingNodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;
            String event = element.getAttribute("event");
            if (!eventName.equalsIgnoreCase(event)) {
                continue;
            }
            String url = normalizeText(element);
            if (!isBlank(url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    public static List<String> getImpressionUrls(String vastXml) {
        return getTagTextList(parseDocument(vastXml), "Impression");
    }

    public static List<String> getClickTrackingUrls(String vastXml) {
        Document doc = parseDocument(vastXml);
        if (doc == null) {
            return Collections.emptyList();
        }
        List<String> urls = getTagTextList(doc, "ClickTracking");
        if (!urls.isEmpty()) {
            return urls;
        }
        String clickThrough = getClickThroughUrl(vastXml);
        if (isBlank(clickThrough)) {
            return Collections.emptyList();
        }
        List<String> fallback = new ArrayList<>();
        fallback.add(clickThrough);
        return fallback;
    }

    public static List<String> getErrorUrls(String vastXml) {
        return getTagTextList(parseDocument(vastXml), "Error");
    }

    public static long getDurationMs(String vastXml) {
        Document doc = parseDocument(vastXml);
        if (doc == null) {
            return -1L;
        }
        return parseTimeToMs(getFirstTagText(doc, "Duration"));
    }

    public static long getSkipOffsetMs(String vastXml) {
        Document doc = parseDocument(vastXml);
        if (doc == null) {
            return -1L;
        }
        NodeList linearNodes = doc.getElementsByTagName("Linear");
        if (linearNodes == null || linearNodes.getLength() == 0) {
            return -1L;
        }
        Node first = linearNodes.item(0);
        if (!(first instanceof Element)) {
            return -1L;
        }
        String skipOffset = ((Element) first).getAttribute("skipoffset");
        if (isBlank(skipOffset)) {
            return -1L;
        }
        skipOffset = skipOffset.trim();
        if (skipOffset.endsWith("%")) {
            try {
                double percent = Double.parseDouble(skipOffset.substring(0, skipOffset.length() - 1));
                long durationMs = getDurationMs(vastXml);
                if (durationMs <= 0L) {
                    return -1L;
                }
                return Math.round(durationMs * (percent / 100d));
            } catch (Exception ignored) {
                return -1L;
            }
        }
        return parseTimeToMs(skipOffset);
    }

    public static void analyzeVast(String vastXml) {
        System.out.println("=== VAST Analysis ===");
        if (!validateVastStructure(vastXml)) {
            System.err.println("VAST structure validation failed");
            return;
        }

        System.out.println("General ClickThrough URL: " + valueOrMissing(getClickThroughUrl(vastXml)));
        System.out.println("Video ClickThrough URL: " + valueOrMissing(getVideoClickThroughUrl(vastXml)));
        System.out.println("MediaFile URL: " + valueOrMissing(getMediaFileUrl(vastXml)));
        System.out.println("Impression trackers: " + getImpressionUrls(vastXml).size());
        System.out.println("Start trackers: " + getTrackingUrls(vastXml, "start").size());
        System.out.println("Complete trackers: " + getTrackingUrls(vastXml, "complete").size());
        System.out.println("Companion preview URL: " + valueOrMissing(getCompanionImageUrl(vastXml)));
        System.out.println("Companion click URL: " + valueOrMissing(getCompanionClickThroughUrl(vastXml)));
        System.out.println("Skip offset ms: " + getSkipOffsetMs(vastXml));
        System.out.println("=== End Analysis ===");
    }

    public static String getCompanionImageUrl(String vastXml) {
        Document doc = parseDocument(vastXml);
        if (doc == null) {
            return null;
        }

        NodeList companionList = doc.getElementsByTagName("Companion");
        if (companionList != null && companionList.getLength() > 0) {
            Element companion = (Element) companionList.item(0);
            NodeList staticResources = companion.getElementsByTagName("StaticResource");
            if (staticResources != null && staticResources.getLength() > 0) {
                return normalizeText(staticResources.item(0));
            }
        }
        return null;
    }

    public static String getCompanionClickThroughUrl(String vastXml) {
        Document doc = parseDocument(vastXml);
        if (doc == null) {
            return null;
        }
        NodeList companionList = doc.getElementsByTagName("Companion");
        if (companionList != null && companionList.getLength() > 0) {
            Element companion = (Element) companionList.item(0);
            NodeList clickThroughNodes = companion.getElementsByTagName("CompanionClickThrough");
            if (clickThroughNodes != null && clickThroughNodes.getLength() > 0) {
                return normalizeText(clickThroughNodes.item(0));
            }
        }
        return null;
    }

    private static Document parseDocument(String vastXml) {
        if (isBlank(vastXml)) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(vastXml)));
            doc.getDocumentElement().normalize();
            return doc;
        } catch (Exception e) {
            System.err.println("Error parsing VAST XML: " + e.getMessage());
            return null;
        }
    }

    private static String getNestedTagText(Document doc, String parentTag, String childTag) {
        NodeList parents = doc.getElementsByTagName(parentTag);
        for (int i = 0; i < parents.getLength(); i++) {
            Node parent = parents.item(i);
            if (!(parent instanceof Element)) {
                continue;
            }
            NodeList children = ((Element) parent).getElementsByTagName(childTag);
            for (int j = 0; j < children.getLength(); j++) {
                String text = normalizeText(children.item(j));
                if (!isBlank(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private static String getFirstTagText(Document doc, String tagName) {
        List<String> texts = getTagTextList(doc, tagName);
        return texts.isEmpty() ? null : texts.get(0);
    }

    private static List<String> getTagTextList(Document doc, String tagName) {
        if (doc == null || isBlank(tagName)) {
            return Collections.emptyList();
        }
        List<String> urls = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            String text = normalizeText(nodes.item(i));
            if (!isBlank(text)) {
                urls.add(text);
            }
        }
        return urls;
    }

    private static String normalizeText(Node node) {
        if (node == null) {
            return null;
        }
        String text = node.getTextContent();
        return isBlank(text) ? null : text.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String valueOrMissing(String value) {
        return isBlank(value) ? "NOT FOUND" : value;
    }

    private static long parseTimeToMs(String raw) {
        if (isBlank(raw)) {
            return -1L;
        }
        try {
            String[] parts = raw.trim().split(":");
            if (parts.length != 3) {
                return -1L;
            }
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            double seconds = Double.parseDouble(parts[2]);
            double totalSeconds = (hours * 3600d) + (minutes * 60d) + seconds;
            return Math.round(totalSeconds * 1000d);
        } catch (Exception ignored) {
            return -1L;
        }
    }
}
