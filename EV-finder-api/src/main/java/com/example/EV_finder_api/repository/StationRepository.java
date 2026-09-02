package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StationRepository extends JpaRepository<Station, String> {
    List<Station> findByStatusOrderByCreatedAtDesc(com.example.EV_finder_api.entity.StationStatus status);
    List<Station> findByOperatorIdOrderByCreatedAtDesc(String operatorId);
    boolean existsByOperatorId(String operatorId);
}
