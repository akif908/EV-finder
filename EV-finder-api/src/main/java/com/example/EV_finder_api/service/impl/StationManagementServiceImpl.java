package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.ServiceResponse;
import com.example.EV_finder_api.dto.StationRequest;
import com.example.EV_finder_api.dto.StationResponse;
import com.example.EV_finder_api.dto.StationServiceRequest;
import com.example.EV_finder_api.entity.*;
import com.example.EV_finder_api.exception.ForbiddenException;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.StationRepository;
import com.example.EV_finder_api.repository.StationServiceRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.StationManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StationManagementServiceImpl implements StationManagementService {

    private final StationRepository stationRepository;
    private final StationServiceRepository stationServiceRepository;
    private final CurrentUserProvider currentUserProvider;

    public StationManagementServiceImpl(StationRepository stationRepository,
                                        StationServiceRepository stationServiceRepository,
                                        CurrentUserProvider currentUserProvider) {
        this.stationRepository = stationRepository;
        this.stationServiceRepository = stationServiceRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public StationResponse create(StationRequest request) {
        User operator = currentUserProvider.getCurrentUser();
        Station station = Station.builder()
                .operator(operator)
                .name(request.name())
                .description(request.description())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .openingTime(request.openingTime())
                .closingTime(request.closingTime())
                .fuelLevel(request.fuelLevel() == null ? 100 : request.fuelLevel())
                .status(StationStatus.ACTIVE)
                .build();
        return StationResponse.from(stationRepository.save(station));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationResponse> myStations() {
        User operator = currentUserProvider.getCurrentUser();
        return stationRepository.findByOperatorIdOrderByCreatedAtDesc(operator.getId())
                .stream().map(StationResponse::from).toList();
    }

    @Override
    public StationResponse update(String stationId, StationRequest request) {
        Station station = getOwnedStation(stationId);
        station.setName(request.name());
        station.setDescription(request.description());
        station.setAddress(request.address());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        station.setOpeningTime(request.openingTime());
        station.setClosingTime(request.closingTime());
        if (request.fuelLevel() != null) {
            station.setFuelLevel(request.fuelLevel());
        }
        return StationResponse.from(stationRepository.save(station));
    }

    @Override
    public StationResponse updateStatus(String stationId, StationStatus status) {
        Station station = getOwnedStation(stationId);
        station.setStatus(status);
        return StationResponse.from(stationRepository.save(station));
    }

    @Override
    public ServiceResponse addService(String stationId, StationServiceRequest request) {
        Station station = getOwnedStation(stationId);
        StationService service = StationService.builder()
                .station(station)
                .serviceType(request.serviceType())
                .connectorType(request.connectorType())
                .powerKw(request.powerKw())
                .pricePerUnit(request.pricePerUnit())
                .availableSlots(request.availableSlots())
                .status(ServiceStatus.ACTIVE)
                .build();
        return ServiceResponse.from(stationServiceRepository.save(service));
    }

    @Override
    public ServiceResponse updateService(String stationId, String serviceId, StationServiceRequest request) {
        StationService service = getOwnedService(stationId, serviceId);
        service.setServiceType(request.serviceType());
        service.setConnectorType(request.connectorType());
        service.setPowerKw(request.powerKw());
        service.setPricePerUnit(request.pricePerUnit());
        service.setAvailableSlots(request.availableSlots());
        return ServiceResponse.from(stationServiceRepository.save(service));
    }

    @Override
    public void removeService(String stationId, String serviceId) {
        stationServiceRepository.delete(getOwnedService(stationId, serviceId));
    }

    // ---- own-station authorization rule: operator may only touch their own stations ----

    private Station getOwnedStation(String stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + stationId));
        User current = currentUserProvider.getCurrentUser();
        boolean isAdmin = current.getRole() == Role.ADMIN;
        if (!station.getOperator().getId().equals(current.getId()) && !isAdmin) {
            throw new ForbiddenException("You can only manage your own stations");
        }
        return station;
    }

    private StationService getOwnedService(String stationId, String serviceId) {
        getOwnedStation(stationId); // enforces ownership first
        StationService service = stationServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
        if (!service.getStation().getId().equals(stationId)) {
            throw new ResourceNotFoundException("Service not found on this station: " + serviceId);
        }
        return service;
    }
}
