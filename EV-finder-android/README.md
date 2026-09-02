# EV-finder-android — Kotlin Client

The ONE final client application for the **EV Charging & Battery-Swap Finder and Booking System**
(AOOP course project). Native Android, Kotlin, Jetpack Compose, MVVM.

Sibling of `../EV-finder-api` (Spring Boot backend) — the two communicate only via
REST (Retrofit) and WebSocket (OkHttp). The backend is the source of truth for
availability and authorization.

## Open & run

1. Open **this folder** (`EV-finder-android`) in **Android Studio** (Jellyfish+ recommended).
2. Let Gradle sync — Studio generates `local.properties` and downloads Gradle 8.7 + dependencies.
3. Start the backend first (`../EV-finder-api`, `mvnw spring-boot:run`).
4. Run the `app` configuration on an emulator or device.

The emulator reaches the host backend via `10.0.2.2:8080` (set in `core/network/ApiClient.kt`).
A physical device needs your PC's LAN IP instead (same file, two constants).

## Structure

```
app/src/main/java/com/example/evfinder/
├── MainActivity.kt          # bottom navigation: Home | Map | Bookings | Vehicles | Profile
├── ui/theme/                # EVFinderTheme: dark + green design system (matches UI reference)
├── core/
│   ├── network/             # ApiClient (Retrofit/OkHttp), ApiService (REST endpoints)
│   ├── model/               # DTOs mirroring backend responses
│   └── storage/             # TokenStore (JWT + role)
└── feature/                 # one package per feature (context §27)
    ├── auth/   home/  station/  map/  booking/
    ├── vehicle/  profile/
    └── operator/  admin/    # role-based screens; navigation switches on the JWT role
```

## Build order (matches project context §29)

Auth screens (login/register against `/api/auth`) → Home/station list → station details →
booking flow → simulated payment → bookings history → vehicles → map (osmdroid + OSM) →
operator → admin → WebSocket live availability.

## Notes

- Map (Phase 7): add `org.osmdroid:osmdroid-android:6.1.18` — free, no API key.
- No AI/ML features. Payment is simulated. One client for all three roles.
- UI reference exports live in `../docs/ev finder ui.zip` (desktop mockups — adapt to mobile,
  drop AI badges/OAuth buttons per the design review).
