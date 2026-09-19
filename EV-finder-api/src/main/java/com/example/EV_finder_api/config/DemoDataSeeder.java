package com.example.EV_finder_api.config;

import com.example.EV_finder_api.entity.*;
import com.example.EV_finder_api.repository.FuelStationRepository;
import com.example.EV_finder_api.repository.StationRepository;
import com.example.EV_finder_api.repository.StationReviewRepository;
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
    private final StationReviewRepository reviewRepository;
    private final FuelStationRepository fuelStationRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, StationRepository stationRepository,
                          StationReviewRepository reviewRepository,
                          FuelStationRepository fuelStationRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.reviewRepository = reviewRepository;
        this.fuelStationRepository = fuelStationRepository;
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

        seedReviews();
        seedFuelStations(operator);
    }

    /** Separate fuel-station module: LPG/Diesel/Octane/Petrol with queue, stock (liters), BDT price. */
    private void seedFuelStations(User operator) {
        java.util.Set<String> existingFuelNames = fuelStationRepository.findAll().stream()
                .map(FuelStation::getName).collect(java.util.stream.Collectors.toSet());

        seedFuelStation(existingFuelNames, operator, "Padma Filling Station", "Full-line filling station",
                "Mirpur 10, Dhaka", 23.8103, 90.3654, true,
                new String[][]{
                        {"LPG", "4", "850", "70"},
                        {"DIESEL", "7", "1250", "105"},
                        {"OCTANE", "3", "720", "125"},
                        {"PETROL", "5", "950", "121"}});

        seedFuelStation(existingFuelNames, operator, "Banani Service & Fuel", "Fuel and service centre",
                "Road 11, Banani, Dhaka", 23.7939, 90.4063, true,
                new String[][]{
                        {"DIESEL", "2", "600", "105"},
                        {"OCTANE", "2", "300", "125"},
                        {"PETROL", "1", "800", "121"}});

        seedFuelStation(existingFuelNames, operator, "Uttara Auto Fuel Point", "Quick top-up point",
                "Sector 7, Uttara, Dhaka", 23.8763, 90.3799, true,
                new String[][]{
                        {"LPG", "1", "400", "70"},
                        {"OCTANE", "6", "150", "125"},
                        {"PETROL", "3", "0", "121"}});

        seedFuelStation(existingFuelNames, operator, "Jatrabari Fuel Depot", "Depot - currently closed",
                "Jatrabari, Dhaka", 23.7233, 90.4195, false,
                new String[][]{
                        {"DIESEL", "0", "2000", "104"},
                        {"PETROL", "0", "1500", "120"}});
    }

    private void seedFuelStation(java.util.Set<String> existingNames, User operator, String name, String desc,
                                 String address, double lat, double lng, boolean isOpen, String[][] fuels) {
        if (existingNames.contains(name)) return;
        existingNames.add(name);

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

    /** Demo reviewer accounts + one review per reviewer per demo station (skipped if already present). */
    private void seedReviews() {
        User ayesha = getOrCreateUser("user@ev.com", "user123", "Ayesha Rahman", Role.USER);
        User rakib = getOrCreateUser("rakib@ev.com", "user123", "Rakib Islam", Role.USER);
        User nusrat = getOrCreateUser("nusrat@ev.com", "user123", "Nusrat Jahan", Role.USER);

        java.util.Map<String, Station> byName = stationRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Station::getName, s -> s));

        seedReview(byName, "GreenPulse Hub Banani", ayesha, 5, "Fast chargers, clean lounge. Highly recommended.");
        seedReview(byName, "GreenPulse Hub Banani", rakib, 4, "Good location, slight wait on weekends.");
        seedReview(byName, "GreenPulse Hub Banani", nusrat, 4, "Reliable CCS2, helpful staff.");
        seedReview(byName, "Dhanmondi Swap Point", ayesha, 4, "Swap takes under two minutes.");
        seedReview(byName, "Dhanmondi Swap Point", rakib, 5, "Perfect for my three-wheeler route.");
        seedReview(byName, "Dhanmondi Swap Point", nusrat, 3, "Gets busy in the evening.");
        seedReview(byName, "Uttara UltraCharge", ayesha, 5, "350 kW is no joke - 10 to 80 in minutes.");
        seedReview(byName, "Uttara UltraCharge", rakib, 5, "Best corridor stop on the route.");
        seedReview(byName, "Uttara UltraCharge", nusrat, 5, "Premium pricing but worth it.");
        seedReview(byName, "Mirpur Community Chargers", ayesha, 4, "Affordable 22 kW top-up.");
        seedReview(byName, "Mirpur Community Chargers", rakib, 3, "Only two guns, plan ahead.");
        seedReview(byName, "Mirpur Community Chargers", nusrat, 4, "Quiet area, easy parking.");
        seedReview(byName, "Old Dhaka Rapid Charge", ayesha, 2, "Queue was long and one gun was down.");
        seedReview(byName, "Old Dhaka Rapid Charge", rakib, 3, "Okay for a quick top-up.");
        seedReview(byName, "Old Dhaka Rapid Charge", nusrat, 2, "Needs maintenance.");
    }

    private void seedReview(java.util.Map<String, Station> stationsByName, String stationName,
                            User user, int rating, String comment) {
        Station station = stationsByName.get(stationName);
        if (station == null) return;
        if (reviewRepository.findByStationIdAndUserId(station.getId(), user.getId()).isPresent()) return;
        reviewRepository.save(StationReview.builder()
                .station(station).user(user).rating(rating).comment(comment)
                .build());
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
