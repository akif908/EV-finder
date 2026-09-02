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

/**
 * Seeds demo accounts and stations so the app has something to show.
 * Runs only while the stations table is empty. Remove for production.
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
        if (stationRepository.count() > 0) return;

        User operator = getOrCreateUser("operator@ev.com", "operator123", "Volt Operator", Role.OPERATOR);
        getOrCreateUser("admin@ev.com", "admin123", "Platform Admin", Role.ADMIN);

        seedStation(operator, "GreenPulse Hub Banani", "Fast charging hub with lounge",
                "Road 11, Banani, Dhaka", 23.7925, 90.4078, "08:00", "23:00",
                new String[][]{{"CHARGING", "CCS2", "150", "45.00"}, {"CHARGING", "Type2", "60", "35.00"}});

        seedStation(operator, "Dhanmondi Swap Point", "Battery swap for bikes & three-wheelers",
                "Mirpur Road, Dhanmondi, Dhaka", 23.7461, 90.3742, "09:00", "21:00",
                new String[][]{{"BATTERY_SWAP", null, null, "80.00"}, {"CHARGING", "Type2", "22", "30.00"}});

        seedStation(operator, "Uttara UltraCharge", "Ultra-fast corridor station",
                "Sector 7, Uttara, Dhaka", 23.8759, 90.3795, "00:00", "23:59",
                new String[][]{{"CHARGING", "CCS2", "350", "55.00"}});

        System.out.println("[DemoDataSeeder] Seeded demo accounts "
                + "(operator@ev.com/operator123, admin@ev.com/admin123) and 3 stations");
    }

    private User getOrCreateUser(String email, String password, String name, Role role) {
        return userRepository.findByEmail(email).orElseGet(() ->
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
