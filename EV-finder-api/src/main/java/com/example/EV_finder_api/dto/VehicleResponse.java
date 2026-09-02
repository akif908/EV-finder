package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.Vehicle;
import com.example.EV_finder_api.entity.VehicleType;

import java.math.BigDecimal;

public record VehicleResponse(
        String id,
        String manufacturer,
        String model,
        VehicleType vehicleType,
        String connectorType,
        BigDecimal batteryCapacityKwh,
        String registrationNo
) {
    public static VehicleResponse from(Vehicle v) {
        return new VehicleResponse(v.getId(), v.getManufacturer(), v.getModel(),
                v.getVehicleType(), v.getConnectorType(), v.getBatteryCapacityKwh(),
                v.getRegistrationNo());
    }
}
