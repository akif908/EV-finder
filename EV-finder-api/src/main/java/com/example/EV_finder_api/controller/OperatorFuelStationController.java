package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.FuelInventoryUpdateRequest;
import com.example.EV_finder_api.dto.FuelStationRequest;
import com.example.EV_finder_api.dto.FuelStationResponse;
import com.example.EV_finder_api.entity.FuelType;
import com.example.EV_finder_api.service.FuelStationManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Operator fuel station management. Role enforced here AND ownership in the
 * service layer (same pattern as EV {@link OperatorStationController}).
 * Fuel stations have no booking endpoints by design.
 */
@RestController
@RequestMapping("/api/operator/fuel-stations")
@PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
public class OperatorFuelStationController {

    private final FuelStationManagementService fuelStationManagementService;

    public OperatorFuelStationController(FuelStationManagementService fuelStationManagementService) {
        this.fuelStationManagementService = fuelStationManagementService;
    }

    @PostMapping
    public ResponseEntity<FuelStationResponse> create(@Valid @RequestBody FuelStationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fuelStationManagementService.create(request));
    }

    @GetMapping("/my")
    public List<FuelStationResponse> myStations() {
        return fuelStationManagementService.myStations();
    }

    @PutMapping("/{id}")
    public FuelStationResponse update(@PathVariable String id, @Valid @RequestBody FuelStationRequest request) {
        return fuelStationManagementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        fuelStationManagementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/open")
    public FuelStationResponse setOpen(@PathVariable String id, @RequestParam boolean isOpen) {
        return fuelStationManagementService.setOpen(id, isOpen);
    }

    /** Add a fuel type or update its queue/stock/price (per fuel type, upsert). */
    @PutMapping("/{id}/fuel/{fuelType}")
    public FuelStationResponse updateFuel(@PathVariable String id,
                                          @PathVariable FuelType fuelType,
                                          @Valid @RequestBody FuelInventoryUpdateRequest request) {
        return fuelStationManagementService.updateFuel(id, fuelType, request);
    }

    @DeleteMapping("/{id}/fuel/{fuelType}")
    public ResponseEntity<Void> removeFuel(@PathVariable String id, @PathVariable FuelType fuelType) {
        fuelStationManagementService.removeFuel(id, fuelType);
        return ResponseEntity.noContent().build();
    }
}
