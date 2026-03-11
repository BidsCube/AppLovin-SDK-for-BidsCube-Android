package com.bidscube.sdk.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;

import java.io.*;

public class VastParser {

    /**
     * Get ClickThrough URL from VAST XML
     * @param vastXml The VAST XML string to parse
     * @return ClickThrough URL if found, null otherwise
     */
    public static String getClickThroughUrl(String vastXml) {
        if (vastXml == null || vastXml.trim().isEmpty()) {
            System.err.println("VAST XML is null or empty");
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new ByteArrayInputStream(vastXml.getBytes("UTF-8"));
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList videoClicksNodes = doc.getElementsByTagName("VideoClicks");
            if (videoClicksNodes.getLength() > 0) {
                Element videoClicks = (Element) videoClicksNodes.item(0);
                NodeList clickThroughInVideoClicks = videoClicks.getElementsByTagName("ClickThrough");
                if (clickThroughInVideoClicks.getLength() > 0) {
                    String url = clickThroughInVideoClicks.item(0).getTextContent();
                    if (url != null && !url.trim().isEmpty()) {
                        System.out.println("Found ClickThrough URL in VideoClicks: " + url.trim());
                        return url.trim();
                    }
                }
            }

            NodeList clickThroughNodes = doc.getElementsByTagName("ClickThrough");
            if (clickThroughNodes.getLength() > 0) {
                String url = clickThroughNodes.item(0).getTextContent();
                if (url != null && !url.trim().isEmpty()) {
                    System.out.println("Found ClickThrough URL: " + url.trim());
                    return url.trim();
                }
            }

            NodeList clickTrackingNodes = doc.getElementsByTagName("ClickTracking");
            if (clickTrackingNodes.getLength() > 0) {
                String url = clickTrackingNodes.item(0).getTextContent();
                if (url != null && !url.trim().isEmpty()) {
                    System.out.println("Found ClickTracking URL: " + url.trim());
                    return url.trim();
                }
            }

            System.out.println("No ClickThrough tag found. Available tags:");
            printAvailableTags(doc);

        } catch (Exception e) {
            System.err.println("Error parsing VAST XML: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.err.println("No ClickThrough tag found");
        return null;
    }

    /**
     * Get Video ClickThrough URL from VAST XML (specifically for video ads)
     * @param vastXml The VAST XML string to parse
     * @return Video ClickThrough URL if found, null otherwise
     */
    public static String getVideoClickThroughUrl(String vastXml) {
        if (vastXml == null || vastXml.trim().isEmpty()) {
            System.err.println("VAST XML is null or empty");
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new ByteArrayInputStream(vastXml.getBytes("UTF-8"));
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList videoClicksNodes = doc.getElementsByTagName("VideoClicks");
            if (videoClicksNodes.getLength() > 0) {
                Element videoClicks = (Element) videoClicksNodes.item(0);
                NodeList clickThroughNodes = videoClicks.getElementsByTagName("ClickThrough");
                if (clickThroughNodes.getLength() > 0) {
                    String url = clickThroughNodes.item(0).getTextContent();
                    if (url != null && !url.trim().isEmpty()) {
                        System.out.println("Found Video ClickThrough URL: " + url.trim());
                        return url.trim();
                    }
                }
            }

            NodeList clickThroughNodes = doc.getElementsByTagName("ClickThrough");
            if (clickThroughNodes.getLength() > 0) {
                String url = clickThroughNodes.item(0).getTextContent();
                if (url != null && !url.trim().isEmpty()) {
                    System.out.println("Found general ClickThrough URL: " + url.trim());
                    return url.trim();
                }
            }

            System.err.println("No Video ClickThrough URL found");
            return null;

        } catch (Exception e) {
            System.err.println("Error parsing VAST XML for video click-through: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validate VAST XML structure
     * @param vastXml The VAST XML string to validate
     * @return true if valid, false otherwise
     */
    public static boolean validateVastStructure(String vastXml) {
        if (vastXml == null || vastXml.trim().isEmpty()) {
            return false;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new ByteArrayInputStream(vastXml.getBytes("UTF-8"));
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            if (!"VAST".equals(doc.getDocumentElement().getTagName())) {
                System.err.println("Root element is not VAST");
                return false;
            }

            NodeList adNodes = doc.getElementsByTagName("Ad");
            if (adNodes.getLength() == 0) {
                System.err.println("No Ad element found");
                return false;
            }

            NodeList creativeNodes = doc.getElementsByTagName("Creative");
            if (creativeNodes.getLength() == 0) {
                System.err.println("No Creative element found");
                return false;
            }

            NodeList mediaFileNodes = doc.getElementsByTagName("MediaFile");
            if (mediaFileNodes.getLength() == 0) {
                System.err.println("No MediaFile element found");
                return false;
            }

            System.out.println("VAST structure validation passed");
            return true;

        } catch (Exception e) {
            System.err.println("Error validating VAST structure: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get MediaFile URL from VAST XML
     * @param vastXml The VAST XML string to parse
     * @return MediaFile URL if found, null otherwise
     */
    public static String getMediaFileUrl(String vastXml) {
        if (vastXml == null || vastXml.trim().isEmpty()) {
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new ByteArrayInputStream(vastXml.getBytes("UTF-8"));
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList mediaFileNodes = doc.getElementsByTagName("MediaFile");
            if (mediaFileNodes.getLength() > 0) {
                String url = mediaFileNodes.item(0).getTextContent();
                if (url != null && !url.trim().isEmpty()) {
                    System.out.println("Found MediaFile URL: " + url.trim());
                    return url.trim();
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting MediaFile URL: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Print all available tags in the VAST XML for debugging
     * @param doc The parsed XML document
     */
    private static void printAvailableTags(Document doc) {
        try {
            NodeList allElements = doc.getElementsByTagName("*");
            System.out.println("Available tags in VAST:");
            for (int i = 0; i < allElements.getLength(); i++) {
                Element element = (Element) allElements.item(i);
                String tagName = element.getTagName();
                String content = element.getTextContent();
                if (content != null && !content.trim().isEmpty() && content.length() < 100) {
                    System.out.println("  " + tagName + ": " + content.trim());
                } else {
                    System.out.println("  " + tagName + ": [content too long or empty]");
                }
            }
        } catch (Exception e) {
            System.err.println("Error printing available tags: " + e.getMessage());
        }
    }

    /**
     * Parse and display VAST structure information
     * @param vastXml The VAST XML string to analyze
     */
    public static void analyzeVast(String vastXml) {
        System.out.println("=== VAST Analysis ===");
        
        if (!validateVastStructure(vastXml)) {
            System.err.println("VAST structure validation failed");
            return;
        }

        String clickThroughUrl = getClickThroughUrl(vastXml);
        String videoClickThroughUrl = getVideoClickThroughUrl(vastXml);
        String mediaFileUrl = getMediaFileUrl(vastXml);

        System.out.println("General ClickThrough URL: " + (clickThroughUrl != null ? clickThroughUrl : "NOT FOUND"));
        System.out.println("Video ClickThrough URL: " + (videoClickThroughUrl != null ? videoClickThroughUrl : "NOT FOUND"));
        System.out.println("MediaFile URL: " + (mediaFileUrl != null ? mediaFileUrl : "NOT FOUND"));

        if (videoClickThroughUrl != null) {
            System.out.println("✅ RECOMMENDED: Use Video ClickThrough URL for video ads");
        } else if (clickThroughUrl != null) {
            System.out.println("⚠️  FALLBACK: Use general ClickThrough URL");
        } else {
            System.out.println("❌ ERROR: No click-through URL found");
        }
        
        System.out.println("=== End Analysis ===");
    }

    public static String getCompanionImageUrl(String vastXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(vastXml));
            Document doc = builder.parse(is);

            NodeList companionList = doc.getElementsByTagName("Companion");
            if (companionList != null && companionList.getLength() > 0) {
                Element companion = (Element) companionList.item(0);
                NodeList staticResources = companion.getElementsByTagName("StaticResource");
                if (staticResources != null && staticResources.getLength() > 0) {
                    return staticResources.item(0).getTextContent().trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
