package com.bidscube.sdk.utils;

import com.bidscube.sdk.models.CompanionAd;
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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
        CompanionAd selected = selectPostVideoCompanion(vastXml);
        System.out.println("Selected companion: " + (selected != null ? selected.getResourceType() : "NONE"));
        System.out.println("Companion preview URL: " + valueOrMissing(getCompanionImageUrl(vastXml)));
        System.out.println("Companion click URL: " + valueOrMissing(getCompanionClickThroughUrl(vastXml)));
        System.out.println("Skip offset ms: " + getSkipOffsetMs(vastXml));
        System.out.println("=== End Analysis ===");
    }

    /**
     * Selects the best post-video Companion. HTML/IFrame resources take priority over Static.
     * Tracking and click URLs are scoped to the selected {@code Companion} element only.
     */
    public static CompanionAd selectPostVideoCompanion(String vastXml) {
        Document doc = parseDocument(vastXml);
        if (doc == null) {
            return null;
        }
        NodeList companionList = doc.getElementsByTagName("Companion");
        if (companionList == null || companionList.getLength() == 0) {
            return null;
        }

        CompanionAd best = null;
        int bestPriority = -1;
        for (int i = 0; i < companionList.getLength(); i++) {
            Node node = companionList.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            CompanionAd candidate = parseCompanionElement((Element) node);
            if (candidate == null) {
                continue;
            }
            int priority = companionResourcePriority(candidate.getResourceType());
            if (priority > bestPriority) {
                best = candidate;
                bestPriority = priority;
            }
        }
        return best;
    }

    public static String getCompanionImageUrl(String vastXml) {
        CompanionAd companion = selectPostVideoCompanion(vastXml);
        if (companion != null && companion.getResourceType() == CompanionAd.ResourceType.STATIC) {
            return companion.getResource();
        }
        return null;
    }

    /** True when the selected companion is a static image resource. */
    public static boolean hasCompanionPreview(String vastXml) {
        CompanionAd companion = selectPostVideoCompanion(vastXml);
        return companion != null && companion.isStaticImage();
    }

    /** True when the selected companion is HTML or IFrame. */
    public static boolean hasHtmlCompanion(String vastXml) {
        CompanionAd companion = selectPostVideoCompanion(vastXml);
        return companion != null && companion.isInteractive();
    }

    /** Inline HTML snippet from the selected companion {@code HTMLResource}. */
    public static String getCompanionHtmlContent(String vastXml) {
        CompanionAd companion = selectPostVideoCompanion(vastXml);
        if (companion != null && companion.getResourceType() == CompanionAd.ResourceType.HTML) {
            return companion.getResource();
        }
        return null;
    }

    /** Remote document URL from the selected companion {@code IFrameResource}. */
    public static String getCompanionIFrameUrl(String vastXml) {
        CompanionAd companion = selectPostVideoCompanion(vastXml);
        if (companion != null && companion.getResourceType() == CompanionAd.ResourceType.IFRAME) {
            return companion.getResource();
        }
        return null;
    }

    public static String getCompanionClickThroughUrl(String vastXml) {
        CompanionAd companion = selectPostVideoCompanion(vastXml);
        return companion != null ? companion.getClickThroughUrl() : null;
    }

    private static CompanionAd parseCompanionElement(Element companion) {
        String html = firstChildText(companion, "HTMLResource");
        String iframe = firstChildText(companion, "IFrameResource");
        String staticUrl = firstStaticResourceUrl(companion);

        CompanionAd.ResourceType resourceType;
        String resource;
        if (!isBlank(html)) {
            resourceType = CompanionAd.ResourceType.HTML;
            resource = html;
        } else if (!isBlank(iframe)) {
            resourceType = CompanionAd.ResourceType.IFRAME;
            resource = iframe;
        } else if (!isBlank(staticUrl)) {
            resourceType = CompanionAd.ResourceType.STATIC;
            resource = staticUrl;
        } else {
            return null;
        }

        int width = parseDimensionAttribute(companion.getAttribute("width"));
        int height = parseDimensionAttribute(companion.getAttribute("height"));
        String clickThrough = firstChildText(companion, "CompanionClickThrough");
        List<String> clickTracking = childTextList(companion, "CompanionClickTracking");
        List<String> creativeView = companionTrackingUrls(companion, "creativeView");

        return new CompanionAd(resourceType, resource, width, height, clickThrough, clickTracking, creativeView);
    }

    private static int companionResourcePriority(CompanionAd.ResourceType type) {
        if (type == CompanionAd.ResourceType.HTML) {
            return 3;
        }
        if (type == CompanionAd.ResourceType.IFRAME) {
            return 2;
        }
        if (type == CompanionAd.ResourceType.STATIC) {
            return 1;
        }
        return 0;
    }

    private static String firstChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getParentNode() != parent) {
                continue;
            }
            String text = normalizeText(node);
            if (!isBlank(text)) {
                return text;
            }
        }
        return null;
    }

    private static String firstStaticResourceUrl(Element companion) {
        NodeList nodes = companion.getElementsByTagName("StaticResource");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getParentNode() != companion) {
                continue;
            }
            String text = normalizeText(node);
            if (!isBlank(text)) {
                return text;
            }
        }
        return null;
    }

    private static List<String> childTextList(Element parent, String tagName) {
        List<String> values = new ArrayList<>();
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getParentNode() != parent) {
                continue;
            }
            String text = normalizeText(node);
            if (!isBlank(text)) {
                values.add(text);
            }
        }
        return values;
    }

    private static List<String> companionTrackingUrls(Element companion, String eventName) {
        List<String> urls = new ArrayList<>();
        NodeList trackingEvents = companion.getElementsByTagName("TrackingEvents");
        for (int i = 0; i < trackingEvents.getLength(); i++) {
            Node eventsNode = trackingEvents.item(i);
            if (eventsNode.getParentNode() != companion) {
                continue;
            }
            if (!(eventsNode instanceof Element)) {
                continue;
            }
            NodeList trackingNodes = ((Element) eventsNode).getElementsByTagName("Tracking");
            for (int j = 0; j < trackingNodes.getLength(); j++) {
                Node trackingNode = trackingNodes.item(j);
                if (!(trackingNode instanceof Element)) {
                    continue;
                }
                Element trackingElement = (Element) trackingNode;
                String event = trackingElement.getAttribute("event");
                if (!eventName.equalsIgnoreCase(event)) {
                    continue;
                }
                String url = normalizeText(trackingElement);
                if (!isBlank(url)) {
                    urls.add(url);
                }
            }
        }
        return urls;
    }

    private static int parseDimensionAttribute(String raw) {
        if (isBlank(raw)) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static Document parseDocument(String vastXml) {
        if (isBlank(vastXml)) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            trySetFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
            trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            trySetAttribute(factory, "http://javax.xml.XMLConstants/property/accessExternalDTD", "");
            trySetAttribute(factory, "http://javax.xml.XMLConstants/property/accessExternalSchema", "");
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(vastXml)));
            doc.getDocumentElement().normalize();
            return doc;
        } catch (Exception e) {
            System.err.println("Error parsing VAST XML: " + e.getMessage());
            return null;
        }
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException ignored) {
        }
    }

    private static void trySetAttribute(DocumentBuilderFactory factory, String name, Object value) {
        try {
            factory.setAttribute(name, value);
        } catch (IllegalArgumentException ignored) {
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
