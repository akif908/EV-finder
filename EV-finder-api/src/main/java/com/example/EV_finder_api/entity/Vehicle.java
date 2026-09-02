package com.example.EV_finder_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @Column(name = "vehicle_id", length = 36)
    private String id;

    /** A vehicle belongs to exactly one user. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(length = 60)
    private String manufacturer;

    @Column(length = 60)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "connector_type", length = 40)
    private String connectorType;

    @Column(name = "battery_capacity_kwh", precision = 5, scale = 2)
    private BigDecimal batteryCapacityKwh;

    @Column(name = "registration_no", nullable = false, unique = true, length = 30)
    private String registrationNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
