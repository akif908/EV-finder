package com.example.EV_finder_api.config;

import com.example.EV_finder_api.entity.*;
import com.example.EV_finder_api.repository.FuelStationRepository;
import com.example.EV_finder_api.repository.NewsRepository;
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
    private final NewsRepository newsRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository, StationRepository stationRepository,
                          StationReviewRepository reviewRepository,
                          FuelStationRepository fuelStationRepository,
                          NewsRepository newsRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.reviewRepository = reviewRepository;
        this.fuelStationRepository = fuelStationRepository;
        this.newsRepository = newsRepository;
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
        seedNews();
    }

    /**
     * Energy & fuel news feed (nation-wide, same for every user). Seeded with
     * publishedAt relative to startup so relative labels ("2 hours ago",
     * "Yesterday") demo correctly. Article text is local demo content, but
     * fuel-price articles embed {DIESEL}/{PETROL}/{OCTANE}/{LPG} tokens that
     * NewsServiceImpl renders from the fuel-station inventory on every read,
     * so operator price updates appear without editing the article. Demo text
     * is re-synced on startup (see seedNewsItem); publish state is left alone.
     */
    private void seedNews() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        seedNewsItem("CNG Supply Disruption Reported",
                "Dhaka's several areas may experience gas pressure issues today.",
                "Titas Gas Transmission and Distribution Company has reported a supply disruption affecting " +
                        "several areas of Dhaka. Households and CNG filling stations in Mirpur, Uttara and parts " +
                        "of Dhanmondi may experience low gas pressure throughout the day. Officials expect normal " +
                        "service to resume by evening as pipeline maintenance completes. CNG vehicle owners are " +
                        "advised to top up early and expect longer queues at operating stations.",
                NewsCategory.GAS_CNG, "The Daily Star",
                null, null,
                now.minusHours(2), true);

        seedNewsItem("Fuel Price Update",
                "Latest update on diesel, petrol, octane and LPG prices.",
                "Current fuel prices have been updated. Diesel is currently ৳{DIESEL}/L, Petrol ৳{PETROL}/L and " +
                        "Octane ৳{OCTANE}/L.\n\n" +
                        "Current fuel prices at a glance:\n" +
                        "- Diesel: ৳{DIESEL}/L\n" +
                        "- Petrol: ৳{PETROL}/L\n" +
                        "- Octane: ৳{OCTANE}/L\n" +
                        "- LPG (auto-gas): ৳{LPG}/L\n\n" +
                        "These rates come straight from EV Finder partner fuel stations. Whenever an operator " +
                        "updates a pump price in the Fuel Station management system, this article refreshes " +
                        "automatically — the values above always show the latest prices stored in the database. " +
                        "Energy analysts note the steady rates follow softening global crude prices, and transport " +
                        "operators welcomed the stability ahead of the harvest season.",
                NewsCategory.FUEL_PRICE, "Bangladesh Energy Regulatory Commission",
                null, null,
                now.minusHours(5), true);

        seedNewsItem("Fuel Price Adjustment Expected at Month-End Review",
                "Diesel now sells at ৳{DIESEL}/L at partner pumps; the month-end review may revise rates.",
                "The Bangladesh Energy Regulatory Commission is expected to conclude its month-end price review " +
                        "this week. At partner filling stations on EV Finder, diesel is currently ৳{DIESEL} per " +
                        "litre, petrol ৳{PETROL} and octane ৳{OCTANE}, while auto-LPG sells at ৳{LPG} per litre. " +
                        "Operators have been asked to keep pumps calibrated ahead of any adjustment, and transport " +
                        "owners' associations said they will review fares once the new rates are published. The " +
                        "prices in this article update automatically as operators revise them in the app.",
                NewsCategory.FUEL_PRICE, "Bangladesh Energy Regulatory Commission",
                null, null,
                now.minusHours(10), true);

        seedNewsItem("EV Charging Update",
                "New charging stations and EV infrastructure updates.",
                "Two new fast-charging hubs are opening this month — a 150 kW CCS2 station on Airport Road and a " +
                        "community 22 kW charging point in Mirpur. With these additions Dhaka's public charging " +
                        "network crosses forty locations. The utilities division confirmed grid connections have " +
                        "been completed and both hubs will operate 8am–11pm daily.",
                NewsCategory.EV_CHARGING, "EV Finder Desk",
                null, null,
                now.minusHours(26), true);

        seedNewsItem("Battery-Swap Network Expands for Three-Wheelers",
                "Easy bike operators get five new swap points along the Gazipur corridor.",
                "Battery-swap operator network has added five new swap points along the Gazipur–Dhaka corridor, " +
                        "cutting turnaround for easy-bike drivers to under three minutes. Each point carries eight " +
                        "charged lead-acid packs. Drivers report daily range anxiety dropping noticeably since the " +
                        "corridor went live last week.",
                NewsCategory.TRANSPORT_ENERGY, "Dhaka Tribune",
                null, null,
                now.minusDays(2), true);

        seedNewsItem("CNG Stations in Narayanganj to Close for Annual Maintenance",
                "A dozen CNG refuelling points will shut for two days next week for pipeline checks.",
                "Titas Gas has announced annual maintenance shutdowns for around a dozen CNG refuelling stations " +
                        "across Narayanganj next week. The two-day closure is part of a pipeline safety audit " +
                        "programme. CNG-run vehicle owners are advised to refuel in advance or use alternate " +
                        "stations along the Dhaka–Chattogram highway, where supply will remain normal. Station " +
                        "operators will post queue updates in the EV Finder app during the shutdown.",
                NewsCategory.GAS_CNG, "New Age Bangladesh",
                null, null,
                now.minusDays(2).minusHours(6), true);

        seedNewsItem("Government Announces Solar Rooftop Incentive",
                "New net-metering rebate targets factories and charging stations.",
                "The power division announced a net-metering rebate for commercial rooftops, including EV charging " +
                        "stations. Facilities installing at least 20 kW of solar can export surplus generation at a " +
                        "preferential rate for the next five years. Officials expect the incentive to speed up " +
                        "solar-powered charging hubs in industrial zones.",
                NewsCategory.GOVERNMENT, "UNB",
                null, null,
                now.minusDays(3), true);

        seedNewsItem("Night Tariff Discount for EV Charging Announced",
                "Off-peak charging from 11pm to 6am will cost 15% less at public stations.",
                "The power division has approved a night-tariff discount for public EV charging: charging between " +
                        "11pm and 6am will cost 15% less than daytime rates. The measure aims to spread grid load " +
                        "and make overnight charging cheaper for ride-share and delivery fleets. Charging point " +
                        "operators will update station pricing in the EV Finder app over the coming weeks, and EV " +
                        "owners will be able to compare day and night rates station by station.",
                NewsCategory.EV_CHARGING, "EV Finder Desk",
                null, null,
                now.minusDays(4), true);

        seedNewsItem("LPG Cylinder Price Steady for Winter Season",
                "Import costs absorbed to keep household cylinders unchanged.",
                "LPG marketing companies confirmed cylinder prices will hold through the winter season despite " +
                        "higher import costs. A 12 kg cylinder stays at the current retail rate, and auto-LPG at " +
                        "the pump currently sells for ৳{LPG} per litre at EV Finder partner stations (live value). " +
                        "Consumer groups cautiously welcomed the assurance.",
                NewsCategory.GAS_CNG, "New Age Bangladesh",
                null, null,
                now.minusDays(5), true);

        // one unpublished draft so the admin screen has something to manage
        seedNewsItem("Draft: EV Import Tax Revision Under Review",
                "NBR weighs duty cut on completely knocked-down EV kits.",
                "The National Board of Revenue is reviewing a proposal to reduce import duty on completely " +
                        "knocked-down (CKD) electric vehicle kits. A decision is expected after the next budget " +
                        "session; local assemblers have lobbied for the cut to narrow the price gap with " +
                        "combustion vehicles.",
                NewsCategory.GOVERNMENT, "EV Finder Desk",
                null, null,
                now.plusDays(1), false);
    }

    /**
     * Creates a demo article, or re-syncs its demo text if an older build
     * already seeded it (keeps price-token content and URL removals current).
     * Publish state and schedule are user data and are left untouched.
     */
    private void seedNewsItem(String title, String shortDescription, String content,
                              NewsCategory category, String source, String sourceUrl,
                              String imageUrl, java.time.LocalDateTime publishedAt, boolean isPublished) {
        News existing = newsRepository.findByTitle(title).orElse(null);
        if (existing == null) {
            newsRepository.save(News.builder()
                    .title(title)
                    .shortDescription(shortDescription)
                    .content(content)
                    .category(category)
                    .source(source)
                    .sourceUrl(sourceUrl)
                    .imageUrl(imageUrl)
                    .publishedAt(publishedAt)
                    .isPublished(isPublished)
                    .build());
            return;
        }
        existing.setShortDescription(shortDescription);
        existing.setContent(content);
        existing.setCategory(category);
        existing.setSource(source);
        existing.setSourceUrl(sourceUrl);
        existing.setImageUrl(imageUrl);
        newsRepository.save(existing);
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
