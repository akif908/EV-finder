package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.ServiceResponse;
import com.example.EV_finder_api.dto.StationRequest;
import com.example.EV_finder_api.dto.StationResponse;
import com.example.EV_finder_api.dto.StationServiceRequest;
import com.example.EV_finder_api.entity.StationStatus;
import com.example.EV_finder_api.service.StationManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Operator station/service management. Role enforced here AND ownership in the service layer. */
@RestController
@RequestMapping("/api/operator/stations")
@PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
public class OperatorStationController {

    private final StationManagementService stationManagementService;

    public OperatorStationController(StationManagementService stationManagementService) {
        this.stationManagementService = stationManagementService;
    }

    @PostMapping
    public ResponseEntity<StationResponse> create(@Valid @RequestBody StationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stationManagementService.create(request));
    }

    @GetMapping("/my")
    public List<StationResponse> myStations() {
        return stationManagementService.myStations();
    }

    @PutMapping("/{id}")
    public StationResponse update(@PathVariable String id, @Valid @RequestBody StationRequest request) {
        return stationManagementService.update(id, request);
    }

    @PutMapping("/{id}/status")
    public StationResponse updateStatus(@PathVariable String id, @RequestParam StationStatus status) {
        return stationManagementService.updateStatus(id, status);
    }

    @PostMapping("/{id}/services")
    public ResponseEntity<ServiceResponse> addService(@PathVariable String id,
                                                      @Valid @RequestBody StationServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stationManagementService.addService(id, request));
    }

    @PutMapping("/{id}/services/{serviceId}")
    public ServiceResponse updateService(@PathVariable String id,
                                         @PathVariable String serviceId,
                                         @Valid @RequestBody StationServiceRequest request) {
        return stationManagementService.updateService(id, serviceId, request);
    }

    @DeleteMapping("/{id}/services/{serviceId}")
    public ResponseEntity<Void> removeService(@PathVariable String id, @PathVariable String serviceId) {
        stationManagementService.removeService(id, serviceId);
        return ResponseEntity.noContent().build();
    }
}
