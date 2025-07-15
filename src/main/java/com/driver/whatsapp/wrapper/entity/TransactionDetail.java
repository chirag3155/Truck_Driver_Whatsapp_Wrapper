package com.driver.whatsapp.wrapper.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Entity representing a transactional interaction record.
 * This table stores information about each driver/order/trip interaction and
 * acts as an audit trail or communication log between the system and end-user (driver).
 *
 * @author mohd.shadab
 */
@Entity
@Table(name = "transaction_detail")
@IdClass(TransactionDetailId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetail {

    /**
     * Primary key component - driver's phone number.
     */
    @Id
    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    /**
     * Primary key component - unique transaction identifier.
     */
    @Id
    @Column(name = "transaction_id", nullable = false, length = 20)
    private String transactionId;

    /**
     * Primary key component - unique identifier for the truck.
     */
    @Id
    @Column(name = "truck_number", nullable = false, length = 20)
    private String truckNumber;

    /**
     * Primary key component - order identifier.
     */
    @Id
    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    /**
     * Identifier for the conversation session or thread.
     */
    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    /**
     * Mode of communication used (e.g., whatsapp, sms, voice).
     */
    @Column(name = "communication_mode", nullable = false)
    private String communicationMode;

    /**
     * Optional status code representing the current state or response from driver.
     */
    @Column(name = "status_code", nullable = true)
    private String statusCode;

    /**
     * Optional state or regional info (could be driver state, trip state, etc.).
     */
    @Column(name = "state", nullable = true)
    private String state;

    /**
     * Optional flow name that triggered the transaction (e.g., REMINDER, LOADING_CONFIRMATION).
     */
    @Column(name = "flow_name", nullable = true)
    private String flowName;

    /**
     * Timestamp when the transaction was created (auto-set by DB).
     */
    @CreationTimestamp
    @Column(name = "transaction_timestamp", nullable = false)
    private LocalDateTime transactionTimestamp;

    /**
     * Timestamp automatically updated before the record is updated.
     */
    @Column(name = "updated_timestamp")
    private LocalDateTime updatedTimestamp;

    /**
     * Unique identifier from external system (for traceability).
     */
    @Column(name = "unique_id", nullable = false)
    private String uniqueId;

    /**
     * Trip identifier associated with this transaction.
     */
    @Column(name = "trip_id", nullable = false)
    private String tripId;

    /**
     * Driver's name.
     */
    @Column(name = "driver_name", nullable = false, length = 50)
    private String driverName;

    /**
     * Drop-off location of the order.
     */
    @Column(name = "drop_off_location", nullable = false, length = 250)
    private String dropOffLocation;

    /**
     * Pick-up location of the order.
     */
    @Column(name = "pick_up_location", nullable = false, length = 250)
    private String pickUpLocation;

    /**
     * Optional name or type of commodity being transported.
     */
    @Column(name = "commodity", nullable = true, length = 100)
    private String commodity;

    /**
     * Original date and time when the order was created.
     */
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    /**
     * Estimated time of arrival (ETA).
     */
    @Column(name = "eta_time", nullable = false)
    private LocalDateTime etaTime;

    /**
     * New ETA in case of changes or delays (optional).
     */
    @Column(name = "new_eta", nullable = true)
    private LocalDateTime newEta;

    /**
     * Number of Trips (optional).
     */
    @Column(name = "number_of_trips", nullable = true)
    private String numberOfTrips = "0";

    /**
     * Optional client or customer name associated with this trip.
     */
    @Column(name = "client_name", nullable = true)
    private String clientName;

    /**
     * Preferred language of the driver (e.g., en, hi, ar).
     */
    @Column(name = "driver_lang", nullable = false)
    private String driverLang;

    /**
     * Driver's timezone (e.g., UTC+5:30).
     */
    @Column(name = "driver_time_zone", nullable = false)
    private String driverTimeZone;

    /**
     * Optional deep link sent to the driver (for tracking, forms, etc.).
     */
    @Column(name = "deeplink", nullable = true)
    private String deeplink;

    /**
     * Optional flexible field for future use / extra metadata.
     */
    @Column(name = "opt_param_1", nullable = true)
    private String optParam1;

    /**
     * Hook method triggered before persisting (insert).
     * Used to ensure timestamps are set.
     */
    @PrePersist
    protected void onCreate() {
        transactionTimestamp = LocalDateTime.now();
    }

    /**
     * Hook method triggered before updating the entity.
     * Used to set last updated timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedTimestamp = LocalDateTime.now();
    }

}