package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.StationRequest;
import com.example.EV_finder_api.dto.StationResponse;
import com.example.EV_finder_api.dto.StationServiceRequest;
import com.example.EV_finder_api.dto.ServiceResponse;
import com.example.EV_finder_api.entity.StationStatus;

import java.util.List;

/** Operator-side management. Every method enforces the own-station rule. */
public interface StationManagementService {

    StationResponse create(StationRequest request);

    List<StationResponse> myStations();

    StationResponse update(String stationId, StationRequest request);

    StationResponse updateStatus(String stationId, StationStatus status);

    ServiceResponse addService(String stationId, StationServiceRequest request);

    ServiceResponse updateService(String stationId, String serviceId, StationServiceRequest request);

    void removeService(String stationId, String serviceId);
}
