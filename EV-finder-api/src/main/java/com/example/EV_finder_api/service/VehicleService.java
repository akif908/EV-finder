package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.VehicleRequest;
import com.example.EV_finder_api.dto.VehicleResponse;

import java.util.List;

/** Vehicle CRUD, strictly scoped to the authenticated user's own vehicles. */
public interface VehicleService {

    VehicleResponse create(VehicleRequest request);

    List<VehicleResponse> getMyVehicles();

    VehicleResponse update(String vehicleId, VehicleRequest request);

    void delete(String vehicleId);
}
