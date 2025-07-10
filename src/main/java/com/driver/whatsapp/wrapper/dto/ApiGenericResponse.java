package com.driver.whatsapp.wrapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standard structure for API success responses.
 * <p>
 * This class is used to return consistent success information for all REST endpoints.
 * It includes a timestamp (set automatically), HTTP status code, message, request path, and the response data.
 * <p>
 * Example JSON:
 * <pre>
 * {
 *   "timestamp": "2025-07-04T22:30:00",
 *   "status": 200,
 *   "message": "Success",
 *   "path": "/api/resource/123",
 *   "data": { ... }
 * }
 * </pre>
 * <p>
 * Lombok annotations are used to generate boilerplate code:
 * <ul>
 *   <li>{@code @Data} - Getters, setters, toString, equals, and hashCode</li>
 *   <li>{@code @Builder} - Builder pattern for easy instantiation</li>
 *   <li>{@code @NoArgsConstructor} - No-argument constructor</li>
 *   <li>{@code @AllArgsConstructor} - All-argument constructor</li>
 * </ul>
 *
 * @author moh.shadab
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiGenericResponse<T> {

    /** The HTTP status code (e.g., 200, 201). */
    private int status;

    /** A human-readable message describing the result. */
    private String message;

    /** The request path where the response is generated. */
    private String path;

    /** The timestamp when the response was created (ISO-8601 format). */
    @Builder.Default
    private String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    /** The actual response data (can be null for empty responses). */
    private T data;
}