package com.driver.whatsapp.wrapper.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Component
@Slf4j
public class DateTimeFormatUtil {
    
    @Value("${app.date.format.order-date:d'th' MMMM, h:mm a}")
    private String orderDateFormat;
    
    @Value("${app.date.format.eta-time:h:mm a}")
    private String etaTimeFormat;
    
    @Value("${app.date.format.default:MMMM d'th' ha}")
    private String defaultDateFormat;
    
    /**
     * Format date/time using configurable format
     */
    public String formatDateTime(LocalDateTime dateTime, String format) {
        try {
            if (dateTime == null) {
                return "scheduled time";
            }
            
            // Handle ordinal suffix for day
            String finalFormat = format;
            if (format.contains("${ordinal}")) {
                int day = dateTime.getDayOfMonth();
                String ordinalSuffix = getDayNumberSuffix(day);
                finalFormat = format.replace("${ordinal}", ordinalSuffix);
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(finalFormat, Locale.ENGLISH);
            String formattedDateTime = dateTime.format(formatter);
            
            // Ensure AM/PM is uppercase
            formattedDateTime = formattedDateTime.replace(" am", " AM").replace(" pm", " PM");
            
            return formattedDateTime;
            
        } catch (Exception e) {
            log.warn("Error formatting datetime '{}' with format '{}': {}", dateTime, format, e.getMessage());
            return dateTime != null ? dateTime.toString() : "scheduled time";
        }
    }
    
    /**
     * Format date/time from string using configurable format
     */
    public String formatDateTime(String dateTimeStr, String format) {
        try {
            if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
                return "scheduled time";
            }
            
            log.debug("🔍 Parsing datetime string: '{}'", dateTimeStr);
            
            // Parse the datetime string with multiple format support
            LocalDateTime dateTime = parseDateTime(dateTimeStr);
            
            log.debug("✅ Parsed datetime: {}", dateTime);
            
            return formatDateTime(dateTime, format);
            
        } catch (Exception e) {
            log.warn("Error parsing/formatting datetime string '{}' with format '{}': {}", dateTimeStr, format, e.getMessage());
            return dateTimeStr; // Return original if parsing fails
        }
    }
    
    /**
     * Parse datetime string with multiple format support
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        // Remove any trailing 'Z' if present
        String cleanDateTimeStr = dateTimeStr.endsWith("Z") ? dateTimeStr.substring(0, dateTimeStr.length() - 1) : dateTimeStr;
        
        try {
            // Try parsing with milliseconds first: "2025-07-07T07:59:06.446"
            if (cleanDateTimeStr.contains(".")) {
                // Remove milliseconds part
                int dotIndex = cleanDateTimeStr.indexOf('.');
                String withoutMillis = cleanDateTimeStr.substring(0, dotIndex);
                return LocalDateTime.parse(withoutMillis);
            }
            
            // Try parsing standard format: "2025-07-05T14:30:00"
            return LocalDateTime.parse(cleanDateTimeStr);
            
        } catch (DateTimeParseException e) {
            // Try parsing with just date part if time is missing
            if (cleanDateTimeStr.contains("T")) {
                String datePart = cleanDateTimeStr.substring(0, cleanDateTimeStr.indexOf("T"));
                return LocalDateTime.parse(datePart + "T00:00:00");
            }
            
            // If all else fails, throw the exception
            throw e;
        }
    }
    
    /**
     * Format order date using configurable format
     */
    public String formatOrderDate(LocalDateTime dateTime) {
        return formatDateTime(dateTime, orderDateFormat);
    }
    
    /**
     * Format order date from string using configurable format
     */
    public String formatOrderDate(String dateTimeStr) {
        return formatDateTime(dateTimeStr, orderDateFormat);
    }
    
    /**
     * Format ETA time using configurable format
     */
    public String formatEtaTime(LocalDateTime dateTime) {
        return formatDateTime(dateTime, etaTimeFormat);
    }
    
    /**
     * Format ETA time from string using configurable format
     */
    public String formatEtaTime(String dateTimeStr) {
        return formatDateTime(dateTimeStr, etaTimeFormat);
    }
    
    /**
     * Format date/time using default format
     */
    public String formatDefault(LocalDateTime dateTime) {
        return formatDateTime(dateTime, defaultDateFormat);
    }
    
    /**
     * Format date/time from string using default format
     */
    public String formatDefault(String dateTimeStr) {
        return formatDateTime(dateTimeStr, defaultDateFormat);
    }
    
    /**
     * Get day number suffix (st, nd, rd, th)
     */
    private String getDayNumberSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
} 