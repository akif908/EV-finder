package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.FuelInventoryUpdateRequest;
import com.example.EV_finder_api.dto.FuelStationRequest;
import com.example.EV_finder_api.dto.FuelStationResponse;
import com.example.EV_finder_api.entity.FuelStation;
import com.example.EV_finder_api.entity.FuelStationInventory;
import com.example.EV_finder_api.entity.FuelType;
import com.example.EV_finder_api.entity.Role;
import com.example.EV_finder_api.entity.User;
import com.example.EV_finder_api.exception.ForbiddenException;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.FuelStationInventoryRepository;
import com.example.EV_finder_api.repository.FuelStationRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.FuelStationManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FuelStationManagementServiceImpl implements FuelStationManagementService {

    private final FuelStationRepository fuelStationRepository;
    private final FuelStationInventoryRepository inventoryRepository;
    private final CurrentUserProvider currentUserProvider;

    public FuelStationManagementServiceImpl(FuelStationRepository fuelStationRepository,
                                            FuelStationInventoryRepository inventoryRepository,
                                            CurrentUserProvider currentUserProvider) {
        this.fuelStationRepository = fuelStationRepository;
        this.inventoryRepository = inventoryRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public FuelStationResponse create(FuelStationRequest request) {
        User operator = currentUserProvider.getCurrentUser();
        FuelStation station = FuelStation.builder()
                .operator(operator)
                .name(request.name())
                .description(request.description())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .isOpen(request.isOpen())
                .build();
        return FuelStationResponse.from(fuelStationRepository.save(station));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuelStationResponse> myStations() {
        User operator = currentUserProvider.getCurrentUser();
        return fuelStationRepository.findByOperatorIdOrderByCreatedAtDesc(operator.getId())
                .stream().map(FuelStationResponse::from).toList();
    }

    @Override
    public FuelStationResponse update(String fuelStationId, FuelStationRequest request) {
        FuelStation station = getOwnedStation(fuelStationId);
        station.setName(request.name());
        station.setDescription(request.description());
        station.setAddress(request.address());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        station.setIsOpen(request.isOpen());
        return FuelStationResponse.from(fuelStationRepository.save(station));
    }

    @Override
    public void delete(String fuelStationId) {
        fuelStationRepository.delete(getOwnedStation(fuelStationId));
    }

    @Override
    public FuelStationResponse setOpen(String fuelStationId, boolean isOpen) {
        FuelStation station = getOwnedStation(fuelStationId);
        station.setIsOpen(isOpen);
        return FuelStationResponse.from(fuelStationRepository.save(station));
    }

    @Override
    public FuelStationResponse updateFuel(String fuelStationId, FuelType fuelType,
                                          FuelInventoryUpdateRequest request) {
        FuelStation station = getOwnedStation(fuelStationId);
        FuelStationInventory inventory = inventoryRepository
                .findByStationIdAndFuelType(fuelStationId, fuelType)
                .orElseGet(() -> FuelStationInventory.builder()
                        .station(station)
                        .fuelType(fuelType)
                        .build());
        // request-level @Min/@DecimalMin already guard negatives; clamp defensively too
        inventory.setQueueCount(Math.max(0, request.queueCount()));
        inventory.setRemainingLiters(request.remainingLiters().max(java.math.BigDecimal.ZERO));
        inventory.setPricePerLiter(request.pricePerLiter().max(java.math.BigDecimal.ZERO));
        inventoryRepository.save(inventory);
        if (!station.getInventories().contains(inventory)) {
            station.getInventories().add(inventory);
        }
        return FuelStationResponse.from(fuelStationRepository.save(station));
    }

    @Override
    public void removeFuel(String fuelStationId, FuelType fuelType) {
        getOwnedStation(fuelStationId);
        FuelStationInventory inventory = inventoryRepository
                .findByStationIdAndFuelType(fuelStationId, fuelType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fuel type " + fuelType + " is not offered by this station"));
        inventoryRepository.delete(inventory);
    }

    // ---- own-station authorization rule: operator may only touch their own fuel stations ----

    private FuelStation getOwnedStation(String fuelStationId) {
        FuelStation station = fuelStationRepository.findById(fuelStationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel station not found: " + fuelStationId));
        User current = currentUserProvider.getCurrentUser();
        boolean isAdmin = current.getRole() == Role.ADMIN;
        if (!station.getOperator().getId().equals(current.getId()) && !isAdmin) {
            throw new ForbiddenException("You can only manage your own fuel stations");
        }
        return station;
    }
}
