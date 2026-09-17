package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.ServiceResponse;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.StationServiceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Single service lookup — used by the app's booking flow for the cost estimator. */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final StationServiceRepository stationServiceRepository;

    public ServiceController(StationServiceRepository stationServiceRepository) {
        this.stationServiceRepository = stationServiceRepository;
    }

    @GetMapping("/{serviceId}")
    public ServiceResponse get(@PathVariable String serviceId) {
        return stationServiceRepository.findById(serviceId)
                .map(ServiceResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
    }
}
