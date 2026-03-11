package com.bidscube.sdk.models.enums;

/**
 * Enum for defining ad positions in windowed mode
 */
public enum AdPosition {
    UNKNOWN(0, "Unknown"),
    ABOVE_THE_FOLD(1, "Above the fold"),
    MAYBE_DEPENDING_ON_SCREEN_SIZE(2, "Maybe depending on screen size"),
    BELOW_THE_FOLD(3, "Below the fold"),
    HEADER(4, "Header"),
    FOOTER(5, "Footer"),
    SIDEBAR(6, "Sidebar"),
    FULL_SCREEN(7, "Full screen");
    
    private final int responseValue;
    private final String displayName;
    
    AdPosition(int responseValue, String displayName) {
        this.responseValue = responseValue;
        this.displayName = displayName;
    }
    
    public int getResponseValue() {
        return responseValue;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Convert integer response value to AdPosition enum
     * @param responseValue Integer value from response
     * @return AdPosition enum or UNKNOWN if value doesn't match
     */
    public static AdPosition fromResponseValue(int responseValue) {
        for (AdPosition pos : values()) {
            if (pos.responseValue == responseValue) {
                return pos;
            }
        }
        return UNKNOWN;
    }
    
    /**
     * Convert string to AdPosition enum
     * @param position String representation of position
     * @return AdPosition enum or UNKNOWN if string doesn't match
     */
    public static AdPosition fromString(String position) {
        if (position == null) return UNKNOWN;
        
        for (AdPosition pos : values()) {
            if (pos.displayName.equalsIgnoreCase(position) || 
                pos.name().equalsIgnoreCase(position)) {
                return pos;
            }
        }
        return UNKNOWN;
    }
}

