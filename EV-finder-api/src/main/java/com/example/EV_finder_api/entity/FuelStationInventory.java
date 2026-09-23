package com.example.EV_finder_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-fuel-type live info for one fuel station: queue length, remaining stock
 * (liters) and price (BDT/liter). Independently maintained per fuel type.
 * Quantities/price must never be negative (validated in the service layer).
 */
@Entity
@Table(name = "fuel_station_inventories",
        uniqueConstraints = @UniqueConstraint(name = "uq_fuel_station_type",
                columnNames = {"fuel_station_id", "fuel_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelStationInventory {

    @Id
    @Column(name = "inventory_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fuel_station_id", nullable = false)
    private FuelStation station;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    /** Vehicles currently waiting in queue for this fuel type. */
    @Column(name = "queue_count", nullable = false)
    @Builder.Default
    private Integer queueCount = 0;

    /** Remaining stock in liters (Bangladesh unit convention). 0 = Out of Stock. */
    @Column(name = "remaining_liters", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal remainingLiters = BigDecimal.ZERO;

    /** Current price in BDT per liter. */
    @Column(name = "price_per_liter", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerLiter;

    /** Last time queue/stock/price was updated by the operator. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
