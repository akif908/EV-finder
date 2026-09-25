package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.VehicleRequest;
import com.example.EV_finder_api.dto.VehicleResponse;
import com.example.EV_finder_api.entity.User;
import com.example.EV_finder_api.entity.Vehicle;
import com.example.EV_finder_api.exception.DuplicateResourceException;
import com.example.EV_finder_api.exception.ForbiddenException;
import com.example.EV_finder_api.exception.ResourceInUseException;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.BookingRepository;
import com.example.EV_finder_api.repository.VehicleRepository;
import com.example.EV_finder_api.security.CurrentUserProvider;
import com.example.EV_finder_api.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserProvider currentUserProvider;

    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              BookingRepository bookingRepository,
                              CurrentUserProvider currentUserProvider) {
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public VehicleResponse create(VehicleRequest request) {
        if (vehicleRepository.existsByRegistrationNoIgnoreCase(request.registrationNo())) {
            throw new DuplicateResourceException("Registration number already exists: " + request.registrationNo());
        }
        User owner = currentUserProvider.getCurrentUser();
        Vehicle vehicle = Vehicle.builder()
                .owner(owner)
                .manufacturer(request.manufacturer())
                .model(request.model())
                .vehicleType(request.vehicleType())
                .connectorType(request.connectorType())
                .batteryCapacityKwh(request.batteryCapacityKwh())
                .registrationNo(request.registrationNo())
                .build();
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles() {
        User owner = currentUserProvider.getCurrentUser();
        return vehicleRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream().map(VehicleResponse::from).toList();
    }

    @Override
    public VehicleResponse update(String vehicleId, VehicleRequest request) {
        Vehicle vehicle = getOwnedVehicle(vehicleId);
        if (!vehicle.getRegistrationNo().equalsIgnoreCase(request.registrationNo())
                && vehicleRepository.existsByRegistrationNoIgnoreCase(request.registrationNo())) {
            throw new DuplicateResourceException("Registration number already exists: " + request.registrationNo());
        }
        vehicle.setManufacturer(request.manufacturer());
        vehicle.setModel(request.model());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setConnectorType(request.connectorType());
        vehicle.setBatteryCapacityKwh(request.batteryCapacityKwh());
        vehicle.setRegistrationNo(request.registrationNo());
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Override
    public void delete(String vehicleId) {
        Vehicle vehicle = getOwnedVehicle(vehicleId);
        // Bookings reference the vehicle, and the FK is ON DELETE RESTRICT, so an
        // unguarded delete would surface as a 500. Refuse it with a clear 409 and
        // keep the booking history intact.
        if (bookingRepository.existsByVehicleId(vehicleId)) {
            throw new ResourceInUseException(
                    "This vehicle has booking history and cannot be deleted: " + vehicle.getRegistrationNo());
        }
        vehicleRepository.delete(vehicle);
    }

    /** Central ownership rule: only the owner (or an ADMIN) may touch a vehicle. */
    private Vehicle getOwnedVehicle(String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        User current = currentUserProvider.getCurrentUser();
        boolean isAdmin = current.getRole() == com.example.EV_finder_api.entity.Role.ADMIN;
        if (!vehicle.getOwner().getId().equals(current.getId()) && !isAdmin) {
            throw new ForbiddenException("You can only manage your own vehicles");
        }
        return vehicle;
    }
}
