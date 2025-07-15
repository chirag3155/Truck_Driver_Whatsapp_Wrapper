package com.driver.whatsapp.wrapper.model;

/**
 * Enum representing different communication flow types
 */
public enum FlowType {
    LOADING_CONFIRMATION("LOADING_CONFIRMATION"),
    DOC_REMINDER("DOC_REMINDER"), 
    OTR("OTR"),
    REMINDER("REMINDER"),
    STATUS_FOLLOW_UP("STATUS_FOLLOW_UP");
    
    private final String value;
    
    FlowType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get FlowType from string value
     */
    public static FlowType fromValue(String value) {
        for (FlowType flowType : FlowType.values()) {
            if (flowType.getValue().equalsIgnoreCase(value)) {
                return flowType;
            }
        }
        throw new IllegalArgumentException("Unknown flow type: " + value);
    }
    
    /**
     * Check if flow type is valid
     */
    public static boolean isValid(String value) {
        try {
            fromValue(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 