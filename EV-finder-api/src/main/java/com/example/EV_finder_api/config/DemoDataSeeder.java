package com.example.EV_finder_api.config;

import com.example.EV_finder_api.entity.*;
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
 * Seeds demo accounts and stations so the app has something to show.
 * On every startup it ensures the demo stations below exist (by name) and
 * backfills fuel levels; user/operator-created stations are never touched.
 * Remove for production.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, StationRepository stationRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // stations created before fuel_level existed: give them the default full reserve
        stationRepository.findAll().forEach(s -> {
            if (s.getFuelLevel() == null) {
                s.setFuelLevel(100);
                stationRepository.save(s);
            }
        });

        User operator = getOrCreateUser("operator@ev.com", "operator123", "Volt Operator", Role.OPERATOR);
        getOrCreateUser("admin@ev.com", "admin123", "Platform Admin", Role.ADMIN);

        // ensure the demo stations exist — adds missing ones, never touches other stations
        Set<String> existingNames = stationRepository.findAll().stream()
                .map(Station::getName).collect(Collectors.toSet());
        long before = stationRepository.count();

        seedStation(existingNames, operator, "GreenPulse Hub Banani", "Fast charging hub with lounge",
                "Road 11, Banani, Dhaka", 23.7925, 90.4078, "08:00", "23:00", 92,
                new String[][]{{"CHARGING", "CCS2", "150", "45.00"}, {"CHARGING", "Type2", "60", "35.00"}});

        seedStation(existingNames, operator, "Dhanmondi Swap Point", "Battery swap for bikes & three-wheelers",
                "Mirpur Road, Dhanmondi, Dhaka", 23.7461, 90.3742, "09:00", "21:00", 67,
                new String[][]{{"BATTERY_SWAP", null, null, "80.00"}, {"CHARGING", "Type2", "22", "30.00"}});

        seedStation(existingNames, operator, "Uttara UltraCharge", "Ultra-fast corridor station",
                "Sector 7, Uttara, Dhaka", 23.8759, 90.3795, "00:00", "23:59", 100,
                new String[][]{{"CHARGING", "CCS2", "350", "55.00"}});

        seedStation(existingNames, operator, "Mirpur Community Chargers", "Neighborhood 22 kW top-up spot",
                "Ring Road, Mirpur, Dhaka", 23.8046, 90.3665, "07:00", "22:00", 38,
                new String[][]{{"CHARGING", "Type2", "22", "25.00"}, {"CHARGING", "Type2", "22", "25.00"}});

        seedStation(existingNames, operator, "Old Dhaka Rapid Charge", "Quick top-up near Gulistan",
                "Gulistan, Dhaka", 23.7255, 90.4126, "06:00", "23:00", 12,
                new String[][]{{"CHARGING", "CCS2", "150", "48.00"}});

        System.out.println("[DemoDataSeeder] Demo accounts ensured (operator@ev.com/operator123, admin@ev.com/admin123); "
                + (stationRepository.count() - before) + " demo station(s) added, " + stationRepository.count() + " total");
    }

    private User getOrCreateUser(String email, String password, String name, Role role) {
        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .name(name).email(email)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(role).build()));
    }

    private void seedStation(Set<String> existingNames, User operator, String name, String desc, String address,
                             double lat, double lng, String open, String close, int fuelLevel,
                             String[][] services) {
        if (existingNames.contains(name)) return; // demo station already present — leave it as the operator set it
        existingNames.add(name);

        Station station = Station.builder()
                .operator(operator)
                .name(name).description(desc).address(address)
                .latitude(BigDecimal.valueOf(lat)).longitude(BigDecimal.valueOf(lng))
                .openingTime(LocalTime.parse(open)).closingTime(LocalTime.parse(close))
                .fuelLevel(fuelLevel)
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
