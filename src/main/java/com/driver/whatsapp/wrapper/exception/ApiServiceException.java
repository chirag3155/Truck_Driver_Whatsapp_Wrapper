package com.driver.whatsapp.wrapper.exception;

/**
 * Custom exception for API service failures
 */
public class ApiServiceException extends RuntimeException {
    
    private final String serviceName;
    private final int statusCode;
    
    public ApiServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = 0;
    }
    
    public ApiServiceException(String serviceName, int statusCode, String message) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }
    
    public ApiServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = 0;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    @Override
    public String toString() {
        return String.format("ApiServiceException[service=%s, status=%d, message=%s]", 
                serviceName, statusCode, getMessage());
    }
} 