package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.Vehicle;
import com.example.EV_finder_api.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VehicleRequest(
        @Size(max = 60) String manufacturer,
        @Size(max = 60) String model,
        @NotNull VehicleType vehicleType,
        @Size(max = 40) String connectorType,
        BigDecimal batteryCapacityKwh,
        @NotBlank @Size(max = 30) String registrationNo
) {
    public static VehicleRequest fromEntity(Vehicle v) {
        return new VehicleRequest(v.getManufacturer(), v.getModel(), v.getVehicleType(),
                v.getConnectorType(), v.getBatteryCapacityKwh(), v.getRegistrationNo());
    }
}
