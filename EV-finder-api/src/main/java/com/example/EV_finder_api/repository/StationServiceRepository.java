package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.ServiceStatus;
import com.example.EV_finder_api.entity.StationService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StationServiceRepository extends JpaRepository<StationService, String> {
    List<StationService> findByStationIdAndStatus(String stationId, ServiceStatus status);
    List<StationService> findByStationId(String stationId);
}
