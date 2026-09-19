package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.FuelStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuelStationRepository extends JpaRepository<FuelStation, String> {

    List<FuelStation> findByOperatorIdOrderByCreatedAtDesc(String operatorId);
}
