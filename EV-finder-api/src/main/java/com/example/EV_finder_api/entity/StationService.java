package com.example.EV_finder_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "station_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationService {

    @Id
    @Column(name = "service_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Column(name = "connector_type", length = 40)
    private String connectorType;

    @Column(name = "power_kw", precision = 6, scale = 2)
    private BigDecimal powerKw;

    @Column(name = "price_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUnit;

    /** Concurrent capacity — availability is computed from this + active bookings (Option B). */
    @Column(name = "available_slots", nullable = false)
    @Builder.Default
    private Integer availableSlots = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
