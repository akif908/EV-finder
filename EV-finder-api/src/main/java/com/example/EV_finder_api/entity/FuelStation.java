package com.example.EV_finder_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A fuel (LPG/Diesel/Octane/Petrol) filling station.
 * Completely separate from the EV {@link Station} — no booking concept exists here;
 * users only see live queue/stock/price info maintained by the operator.
 */
@Entity
@Table(name = "fuel_stations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelStation {

    @Id
    @Column(name = "fuel_station_id", length = 36)
    private String id;

    /** Ownership: an operator may only manage their own fuel stations. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 7)
    private BigDecimal longitude;

    /** Closed stations stay visible to users but show a "Closed" status. */
    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private Boolean isOpen = true;

    @OneToMany(mappedBy = "station", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FuelStationInventory> inventories = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
