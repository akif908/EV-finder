package com.example.EV_finder_api.config;

import com.example.EV_finder_api.entity.*;
import com.example.EV_finder_api.repository.FuelStationRepository;
import com.example.EV_finder_api.repository.StationRepository;
import com.example.EV_finder_api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeds demo accounts, EV stations and fuel stations so the app has something
 * to show. Runs only while the stations table is empty. Remove for production.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final FuelStationRepository fuelStationRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, StationRepository stationRepository,
                          FuelStationRepository fuelStationRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.fuelStationRepository = fuelStationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User operator = getOrCreateUser("operator@ev.com", "operator123", "Volt Operator", Role.OPERATOR);
        getOrCreateUser("admin@ev.com", "admin123", "Platform Admin", Role.ADMIN);

        // The two modules are seeded independently, so an existing database that
        // already has EV stations still receives the fuel stations.
        if (stationRepository.count() == 0) {
            seedStation(operator, "GreenPulse Hub Banani", "Fast charging hub with lounge",
                    "Road 11, Banani, Dhaka", 23.7925, 90.4078, "08:00", "23:00",
                    new String[][]{{"CHARGING", "CCS2", "150", "45.00"}, {"CHARGING", "Type2", "60", "35.00"}});

            seedStation(operator, "Dhanmondi Swap Point", "Battery swap for bikes & three-wheelers",
                    "Mirpur Road, Dhanmondi, Dhaka", 23.7461, 90.3742, "09:00", "21:00",
                    new String[][]{{"BATTERY_SWAP", null, null, "80.00"}, {"CHARGING", "Type2", "22", "30.00"}});

            seedStation(operator, "Uttara UltraCharge", "Ultra-fast corridor station",
                    "Sector 7, Uttara, Dhaka", 23.8759, 90.3795, "00:00", "23:59",
                    new String[][]{{"CHARGING", "CCS2", "350", "55.00"}});
        }

        if (fuelStationRepository.count() == 0) {
            seedFuelStations(operator);
        }

        System.out.println("[DemoDataSeeder] Ready — demo accounts "
                + "(operator@ev.com/operator123, admin@ev.com/admin123), "
                + stationRepository.count() + " EV stations, "
                + fuelStationRepository.count() + " fuel stations");
    }

    /**
     * Separate fuel-station module: LPG/Diesel/Octane/Petrol with queue length,
     * remaining stock (litres) and BDT price per litre.
     */
    private void seedFuelStations(User operator) {
        Set<String> existing = fuelStationRepository.findAll().stream()
                .map(FuelStation::getName).collect(Collectors.toSet());

        seedFuelStation(existing, operator, "Padma Filling Station", "Full-line filling station",
                "Mirpur 10, Dhaka", 23.8103, 90.3654, true,
                new String[][]{
                        {"LPG", "4", "850", "70"},
                        {"DIESEL", "7", "1250", "105"},
                        {"OCTANE", "3", "720", "125"},
                        {"PETROL", "5", "950", "121"}});

        seedFuelStation(existing, operator, "Banani Service & Fuel", "Fuel and service centre",
                "Road 11, Banani, Dhaka", 23.7939, 90.4063, true,
                new String[][]{
                        {"DIESEL", "2", "600", "105"},
                        {"OCTANE", "2", "300", "125"},
                        {"PETROL", "1", "800", "121"}});

        seedFuelStation(existing, operator, "Uttara Auto Fuel Point", "Quick top-up point",
                "Sector 7, Uttara, Dhaka", 23.8763, 90.3799, true,
                new String[][]{
                        {"LPG", "1", "400", "70"},
                        {"OCTANE", "6", "150", "125"},
                        {"PETROL", "3", "0", "121"}});

        seedFuelStation(existing, operator, "Jatrabari Fuel Depot", "Depot — currently closed",
                "Jatrabari, Dhaka", 23.7233, 90.4195, false,
                new String[][]{
                        {"DIESEL", "0", "2000", "104"},
                        {"PETROL", "0", "1500", "120"}});
    }

    private void seedFuelStation(Set<String> existing, User operator, String name, String desc,
                                 String address, double lat, double lng, boolean isOpen, String[][] fuels) {
        if (existing.contains(name)) return;
        existing.add(name);

        FuelStation station = FuelStation.builder()
                .operator(operator)
                .name(name).description(desc).address(address)
                .latitude(BigDecimal.valueOf(lat)).longitude(BigDecimal.valueOf(lng))
                .isOpen(isOpen)
                .build();

        for (String[] f : fuels) {
            station.getInventories().add(FuelStationInventory.builder()
                    .station(station)
                    .fuelType(FuelType.valueOf(f[0]))
                    .queueCount(Integer.parseInt(f[1]))
                    .remainingLiters(new BigDecimal(f[2]))
                    .pricePerLiter(new BigDecimal(f[3]))
                    .build());
        }
        fuelStationRepository.save(station);
    }

    private User getOrCreateUser(String email, String password, String name, Role role) {        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .name(name).email(email)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(role).build()));
    }

    private void seedStation(User operator, String name, String desc, String address,
                             double lat, double lng, String open, String close,
                             String[][] services) {
        Station station = Station.builder()
                .operator(operator)
                .name(name).description(desc).address(address)
                .latitude(BigDecimal.valueOf(lat)).longitude(BigDecimal.valueOf(lng))
                .openingTime(LocalTime.parse(open)).closingTime(LocalTime.parse(close))
                .status(StationStatus.ACTIVE)
                .build();

        for (String[] sv : services) {
            station.getServices().add(StationService.builder()
                    .station(station)
                    .serviceType(ServiceType.valueOf(sv[0]))
                    .connectorType(sv[1])
                    .powerKw(sv[2] == null ? null : new BigDecimal(sv[2]))
                    .pricePerUnit(new BigDecimal(sv[3]))
                    .availableSlots(sv[0].equals("BATTERY_SWAP") ? 4 : 2)
                    .status(ServiceStatus.ACTIVE)
                    .build());
        }
        stationRepository.save(station);
    }
}
