package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    List<Vehicle> findByOwnerIdOrderByCreatedAtDesc(String userId);
    boolean existsByRegistrationNoIgnoreCase(String registrationNo);
}
