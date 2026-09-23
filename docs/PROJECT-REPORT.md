# EV Charging & Battery-Swap Finder and Booking System
## Full Project Report — Features, Architecture, Technologies and Structures

**Project type:** AOOP (Advanced Object-Oriented Programming) course project
**Repository:** `EV finder` — backend `EV-finder-api`, Android client `EV-finder-android`
**Client:** Native Android application (Kotlin + Jetpack Compose) — *not* a web app
**Server:** Java Spring Boot REST API + MySQL
**Document status:** reflects the codebase as committed (`640af94`)

---

## 1. Project Overview

EV Finder is a mobile platform that lets drivers find EV charging points and battery-swap
stations, reserve a charging slot for a specific vehicle and time window, pay through a
simulated gateway, and rate the station afterwards. Station owners (operators) manage their
stations and see incoming bookings; platform administrators manage users and the platform as a
whole. A parallel **fuel-station module** covers petrol/diesel/octane/LPG pumps with live queue
length, remaining stock and price per litre — informational only, no booking. A **news feed**
surfaces the latest energy, fuel, LPG and EV headlines inside the app.

### 1.1 Problem it solves
- EV adoption is limited by "range anxiety" and uncertainty about whether a charger is free.
- Battery-swap points, petrol pumps and LPG stations are scattered and their status is unknown.
- Station operators have no simple digital tool to publish availability and inventory.

### 1.2 Objectives
1. Let a user discover stations on a map and in a rated list, and see **live** availability.
2. Prevent double-booking of the same slot (capacity-based conflict control).
3. Provide a complete, realistic booking → payment → receipt → review flow.
4. Give operators a management dashboard, and admins platform-wide control.
5. Show energy/fuel news and fuel-inventory updates so the app is useful beyond charging.
6. Keep the stack open and free: no paid maps, no paid APIs, no AI/ML.

---

## 2. Scope, Requirements and Constraints

### 2.1 Functional requirements (implemented)

| ID | Requirement | Status |
|----|-------------|--------|
| FR-1 | Register / log in with JWT authentication | Done |
| FR-2 | Three roles: USER, OPERATOR, ADMIN with separate app shells | Done |
| FR-3 | Manage vehicles (add / edit / delete, type, connector, battery) | Done |
| FR-4 | Browse stations: search, sort, filter, ranked by rating | Done |
| FR-5 | Map view of stations with markers (free OpenStreetMap) | Done |
| FR-6 | Station detail with services, price, live availability, reviews | Done |
| FR-7 | Book a slot for a vehicle/date/time with conflict prevention | Done |
| FR-8 | Simulated payment (success + forced failure + refund on cancel) | Done |
| FR-9 | Booking receipt with reference number | Done |
| FR-10 | Booking history with filters, cancel, and rating CTA | Done |
| FR-11 | Star rating + comment per completed session; ranking by rating | Done |
| FR-12 | In-app notifications per role, with unread badge | Done |
| FR-13 | Operator: create/edit stations on a map picker, manage services | Done |
| FR-14 | Operator: view customer bookings and dashboard KPIs | Done |
| FR-15 | Admin: platform overview, user management, booking oversight | Done |
| FR-16 | Fuel station module: queue, stock, price; operator management | Done |
| FR-17 | Issue reporting: user → admin, with status and reply | Done |
| FR-18 | Energy/fuel/EV news feed on the user side | Done |
| FR-19 | Live availability pushed over WebSocket | Done |

### 2.2 Constraints (locked by the project context)

| Constraint | How it is respected |
|-----------|---------------------|
| One client: native Kotlin Android | No React/JS client exists in the repo |
| No AI/ML | No model, no recommendation engine, no AI tables |
| No real payment gateway | Payment is simulated server-side (`PaymentMethod` enum of *simulated* channels) |
| Passwords never stored in plain text | BCrypt via `BCryptPasswordEncoder` |
| Free map provider, no billing | osmdroid + OpenStreetMap tiles; no Google Maps API key used |
| Free POI / news data | Overpass API (OSM) and Google News RSS — both keyless |
| Relational storage | MySQL with Spring Data JPA / Hibernate |

### 2.3 Explicitly out of scope
AI/ML recommendation, real payment gateways, IoT integration with chargers, iOS client,
web dashboard, SMS/email delivery, production deployment/DevOps.

---

## 3. Roles and Permissions

| Capability | USER | OPERATOR | ADMIN |
|---|---|---|---|
| Register / log in | ✅ | ✅ | ✅ |
| Manage own vehicles | ✅ | — | — |
| Browse stations / map / news | ✅ | ✅ | ✅ |
| Create bookings + pay | ✅ | — | — |
| Rate a finished session | ✅ | — | — |
| Report an issue | ✅ | ✅ | ✅ |
| Manage own fuel/EV stations | — | ✅ | ✅ |
| View customer bookings | — | ✅ (own stations) | ✅ (all) |
| Platform overview | — | — | ✅ |
| Change user roles / delete users | — | — | ✅ |
| Force-cancel any booking | — | — | ✅ |
| Triage issue reports | — | — | ✅ |
| Refresh the news cache | — | — | ✅ |

Enforcement is centralised with Spring Security method-level annotations
(`@PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")`, `hasRole('ADMIN')`) on the
operator and admin controllers, plus a JWT filter on every request.

---

## 4. System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Android Client (Kotlin / Compose)                │
│  Role-based shells:  EVFinderApp (USER) | OperatorApp | AdminApp    │
│  UI (Compose) → ViewModel → Repository → Retrofit / WebSocket       │
│  Local: TokenStore (JWT + role), osmdroid MapView, OSM tiles        │
└───────────────┬─────────────────────────────────┬───────────────────┘
                │ REST (JSON + JWT bearer)        │ WebSocket (ws://…/ws)
                ▼                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                Spring Boot API — modular monolith                   │
│  Controllers → Services (interfaces) → impl → Repositories (JPA)    │
│  Cross-cutting: SecurityConfig + JWT, GlobalExceptionHandler,       │
│  NotificationService, AvailabilityWebSocketHandler, DemoDataSeeder  │
└───────────────┬─────────────────────────────────┬───────────────────┘
                │ JDBC / Hibernate                │ HTTP (server-side)
                ▼                                 ▼
        ┌───────────────┐              ┌────────────────────────────┐
        │ MySQL (XAMPP) │              │ Free external services:    │
        │ 12 tables     │              │ • Overpass API (OSM POIs)  │
        └───────────────┘              │ • Google News RSS          │
                                       │ • OSM tile servers         │
                                       └────────────────────────────┘
```

### 4.1 Architectural pattern
- **Server:** layered modular monolith — Controller → Service (interface) → ServiceImpl →
  Repository → Entity, with DTO records as the API boundary (entities never serialised).
- **Client:** MVVM with unidirectional data flow — Compose UI observes `StateFlow` from a
  ViewModel, which calls a Repository, which wraps Retrofit or the WebSocket.
- **Real-time:** one raw WebSocket at `/ws` broadcasting `availability_changed` events; any
  broadcast triggers a silent re-fetch on the stations, home and bookings screens.

---

## 5. Technology Stack

### 5.1 Backend
| Concern | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot (`spring-boot-starter-parent`) | 4.1.1 |
| Web | `spring-boot-starter-webmvc` (Tomcat) | — |
| Persistence | `spring-boot-starter-data-jpa` + Hibernate ORM | — |
| Security | `spring-boot-starter-security` | — |
| Validation | `spring-boot-starter-validation` (Jakarta Bean Validation) | — |
| Real-time | `spring-boot-starter-websocket` | — |
| JWT | `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | 0.12.6 |
| Boilerplate | Lombok | — |
| Database driver | `mysql-connector-j` | — |
| Build | Maven (with `mvnw` wrapper) | — |
| Test starters | data-jpa, security, validation, webmvc, websocket test starters | — |

### 5.2 Frontend (Android)
| Concern | Technology | Version |
|---|---|---|
| Language | Kotlin | (Kotlin Android plugin) |
| UI toolkit | Jetpack Compose + Material 3 | Compose BOM |
| Build | Gradle (Kotlin DSL) | Gradle 8.7 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 24 | — |
| JVM target | 17 | — |
| Navigation | `navigation-compose` | 2.8.1 |
| HTTP | Retrofit + Gson converter | 2.11.0 |
| HTTP client | OkHttp (+ logging interceptor) | 4.12.0 |
| Async | `kotlinx-coroutines-android` | 1.8.1 |
| Lifecycle / VM | `lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose` | 2.8.6 |
| Activity | `activity-compose` | 1.9.2 |
| Maps | **osmdroid-android** (OpenStreetMap, free) | 6.1.18 |
| Icons | `material-icons-extended` | — |
| Package / app id | `com.example.evfinder` | v1.0 |

> Note: `play-services-maps` remains declared in the build file as a legacy dependency; the
> app's map screens use **osmdroid** exclusively, so no Google Maps API key is required.

### 5.3 External / free services
| Service | Purpose | Key required |
|---|---|---|
| OpenStreetMap tile servers (MAPNIK) | base map imagery | No |
| Overpass API (`overpass-api.de`, `overpass.kumi.systems`) | public fuel/LPG POIs near stations | No |
| Google News RSS | energy / fuel / LPG / EV headlines | No |
| Android geo intents (`google.navigation:` / `geo:`) | turn-by-turn directions hand-off | No |

### 5.4 Development tools
Android Studio, XAMPP (MySQL 3306 + phpMyAdmin), Git/GitHub, draw.io (architecture diagrams),
dbdiagram.io (schema), Stitch mockups for the UI language.

---

## 6. Backend Structure

Base package: `com.example.EV_finder_api`

```
EV-finder-api/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/main/
    ├── java/com/example/EV_finder_api/
    │   ├── EvFinderApiApplication.java
    │   ├── config/        SecurityConfig, WebSocketConfig, DemoDataSeeder
    │   ├── controller/    16 REST controllers
    │   ├── dto/           30 request/response records
    │   ├── entity/        12 entities + 10 enums
    │   ├── exception/     6 exceptions + GlobalExceptionHandler
    │   ├── repository/    12 Spring Data repositories
    │   ├── security/      JwtService, JwtAuthenticationFilter,
    │   │                  CustomUserDetailsService, CurrentUserProvider
    │   ├── service/       11 service interfaces + impl/ 12 implementations
    │   └── websocket/     AvailabilityWebSocketHandler
    └── resources/application.properties
```

### 6.1 Entities (12) and enums (10)
| Entity | Table | Purpose |
|---|---|---|
| `User` | `users` | account, BCrypt hash, role, phone |
| `Vehicle` | `vehicles` | user's EV: manufacturer, model, type, connector, battery kWh, registration |
| `Station` | `stations` | EV/swap station: geo, hours, status, **energy reserve (`fuel_level`)** |
| `StationService` | `station_services` | a charge point / swap bay: type, connector, kW, price, capacity |
| `Booking` | `bookings` | reservation: user, vehicle, station, service, window, status |
| `Payment` | `payments` | one per booking: amount, method, status, transaction ref |
| `Review` | `reviews` | rating 1–5 + comment, **one per booking** (verified session) |
| `Notification` | `notifications` | per-user in-app message with a type |
| `FuelStation` | `fuel_stations` | petrol/LPG station (separate module) |
| `FuelStationInventory` | `fuel_station_inventories` | per fuel type: queue, litres, BDT/litre |
| `Issue` | `issues` | user-filed bug/station report with admin triage |
| `NewsArticle` | `news_articles` | cached headline for the news section |

Enums: `Role`, `VehicleType`, `StationStatus`, `ServiceType`, `ServiceStatus`, `BookingStatus`,
`PaymentStatus`, `PaymentMethod`, `FuelType`, `IssueStatus` (+ nested `Notification.NotificationType`).

`NotificationType` values: `BOOKING_CONFIRMED`, `BOOKING_CANCELLED`, `PAYMENT_FAILED`,
`NEW_BOOKING`, `NEW_REVIEW`, `NEW_ISSUE`, `ISSUE_RESOLVED`, `GENERAL`.

### 6.2 Services
| Interface | Implementation | Responsibility |
|---|---|---|
| `AuthService` | `AuthServiceImpl` | register, login, JWT issue, BCrypt |
| `VehicleService` | `VehicleServiceImpl` | per-user vehicle CRUD with ownership checks |
| `StationQueryService` | `StationQueryServiceImpl` | search / nearby / details, **rating ranking**, live availability |
| `StationManagementService` | `StationManagementServiceImpl` | operator station + service CRUD |
| `AvailabilityService` | `AvailabilityServiceImpl` | slot grid for a date (capacity − overlapping bookings) |
| `BookingService` | `BookingServiceImpl` | create (conflict-checked), list, details, cancel |
| `PaymentService` | `PaymentServiceImpl` | simulated payment, failure path, refund |
| `NotificationService` | (self-contained) | create notifications, non-fatal by design |
| `AdminService` | `AdminServiceImpl` | overview KPIs, user role changes, force-cancel |
| `FuelStationQueryService` | `FuelStationQueryServiceImpl` | browse fuel stations with filters |
| `FuelStationManagementService` | `FuelStationManagementServiceImpl` | operator fuel CRUD + inventory |
| (no interface) | `IssueServiceImpl` | file/triage issue reports |
| (no interface) | `NewsServiceImpl` | fetch + cache RSS headlines |

### 6.3 Cross-cutting backend components
- **`SecurityConfig`** — stateless filter chain, `BCryptPasswordEncoder`, `/api/auth/**` open,
  everything else authenticated, `@EnableMethodSecurity` for `@PreAuthorize`.
- **`JwtService`** — HS256 token, subject = user id, `role` claim, expiry from
  `app.jwt.expiration-ms` (86 400 000 ms = 24 h).
- **`JwtAuthenticationFilter`** — validates the `Authorization: Bearer` header on every call.
- **`CurrentUserProvider`** — resolves the authenticated `User` inside services.
- **`GlobalExceptionHandler`** — maps `ResourceNotFoundException`, `ForbiddenException`,
  `ValidationException`, `DuplicateResourceException`, `BookingUnavailableException`,
  `UnauthorizedException` to clean JSON + HTTP status.
- **`AvailabilityWebSocketHandler`** — broadcasts `availability_changed` to all sockets at `/ws`.
- **`DemoDataSeeder`** — `CommandLineRunner` seeding demo accounts, EV stations and fuel
  stations **independently** (so an existing database still receives new fuel data).

### 6.4 Configuration (`application.properties`)
```
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/ev_finder?…&serverTimezone=UTC
spring.datasource.username=root
spring.jpa.hibernate.ddl-auto=update      # schema auto-managed from entities
spring.jpa.show-sql=true
spring.jpa.open-in-view=false             # no lazy loading in controllers
app.jwt.secret=…                          # override in production
app.jwt.expiration-ms=86400000
```

---

## 7. REST API Reference

All endpoints are JSON over HTTP; every non-auth endpoint requires
`Authorization: Bearer <jwt>`.

### 7.1 Authentication — `/api/auth`
| Method | Path | Body / Notes |
|---|---|---|
| POST | `/api/auth/register` | `RegisterRequest` (name, email, password, role) → `AuthResponse` (token, role, user) |
| POST | `/api/auth/login` | `LoginRequest` → `AuthResponse` |

### 7.2 Profile — `/api/users`
| Method | Path | Notes |
|---|---|---|
| GET | `/api/users/me` | current profile (`UserMeResponse`) |
| PUT | `/api/users/me` | update name / phone |
| PUT | `/api/users/me/password` | `ChangePasswordRequest` (BCrypt re-hash) |

### 7.3 Vehicles — `/api/vehicles`
| Method | Path | Notes |
|---|---|---|
| POST | `/api/vehicles` | add vehicle |
| GET | `/api/vehicles/my` | list own vehicles |
| PUT | `/api/vehicles/{id}` | update |
| DELETE | `/api/vehicles/{id}` | delete |

### 7.4 Stations (public discovery) — `/api/stations`
| Method | Path | Notes |
|---|---|---|
| GET | `/api/stations?q=` | search; **ranked by average rating, then review count**; each service carries live availability + capacity |
| GET | `/api/stations/nearby?latitude&longitude&radiusKm` | haversine-filtered |
| GET | `/api/stations/{id}` | details incl. services and rating |

### 7.5 Availability — `/api/services/{serviceId}/slots`
| Method | Path | Notes |
|---|---|---|
| GET | `/api/services/{serviceId}/slots?date=` | slot grid: capacity, remaining, bookable |

### 7.6 Bookings — `/api/bookings`
| Method | Path | Notes |
|---|---|---|
| POST | `/api/bookings` | create (PENDING); 409 if no free slot |
| GET | `/api/bookings/my?status=` | own bookings (incl. `reviewed` flag) |
| GET | `/api/bookings/{id}` | single booking (owner or admin) |
| PUT | `/api/bookings/{id}/cancel` | cancel; refunds a SUCCESS payment |

### 7.7 Payment (simulated) — `/api/payments`
| Method | Path | Notes |
|---|---|---|
| POST | `/api/payments/{bookingId}` | `PaymentRequest` (method + `forceFailure`) → SUCCESS / FAILED |

### 7.8 Reviews — `/api/reviews`
| Method | Path | Notes |
|---|---|---|
| POST | `/api/reviews` | one review per booking (unique constraint) |
| GET | `/api/reviews/station/{stationId}` | average, count, list |

### 7.9 Notifications — `/api/notifications`
| Method | Path | Notes |
|---|---|---|
| GET | `/api/notifications/my` | newest first |
| GET | `/api/notifications/unread-count` | `{ "count": n }` — drives the bell badge |
| PUT | `/api/notifications/{id}/read` | mark one read |
| PUT | `/api/notifications/read-all` | mark all read |

### 7.10 Issue reports — `/api/issues`
| Method | Path | Notes |
|---|---|---|
| POST | `/api/issues` | file a report; notifies every admin |
| GET | `/api/issues/my` | own reports with status + admin reply |

### 7.11 News — `/api/news`
| Method | Path | Notes |
|---|---|---|
| GET | `/api/news?category=&limit=` | cached energy/fuel/EV headlines (categories FUEL, LPG, EV, POLICY) |
| POST | `/api/news/refresh` | **ADMIN** — force a feed refresh |

### 7.12 Fuel stations — `/api/fuel-stations`
| Method | Path | Notes |
|---|---|---|
| GET | `/api/fuel-stations?q=&fuel=&availableOnly=` | browse, filter by LPG/DIESEL/OCTANE/PETROL and stock |
| GET | `/api/fuel-stations/nearby?latitude&longitude&radiusKm` | nearest first |
| GET | `/api/fuel-stations/{id}` | detail incl. inventory rows |

### 7.13 Operator (role: OPERATOR or ADMIN)
| Method | Path | Notes |
|---|---|---|
| POST | `/api/operator/stations` | create station |
| GET | `/api/operator/stations/my` | own stations |
| PUT | `/api/operator/stations/{id}` | update details |
| PUT | `/api/operator/stations/{id}/status` | ACTIVE / INACTIVE / TEMPORARILY_UNAVAILABLE |
| POST | `/api/operator/stations/{id}/services` | add a service |
| PUT | `/api/operator/stations/{id}/services/{serviceId}` | edit service |
| DELETE | `/api/operator/stations/{id}/services/{serviceId}` | remove service |
| GET | `/api/operator/bookings` | bookings at own stations (with customer name) |
| POST | `/api/operator/fuel-stations` | create fuel station |
| GET | `/api/operator/fuel-stations/my` | own fuel stations |
| PUT | `/api/operator/fuel-stations/{id}` | update |
| DELETE | `/api/operator/fuel-stations/{id}` | delete |
| PUT | `/api/operator/fuel-stations/{id}/open?isOpen=` | open / close |
| PUT | `/api/operator/fuel-stations/{id}/fuel/{fuelType}` | update queue, litres, price |
| DELETE | `/api/operator/fuel-stations/{id}/fuel/{fuelType}` | remove a fuel type |

### 7.14 Admin (role: ADMIN)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/overview` | platform KPIs (`PlatformOverview`) |
| GET | `/api/admin/users?role=` | list / filter users |
| PUT | `/api/admin/users/{id}/role?role=` | change a user's role |
| DELETE | `/api/admin/users/{id}` | delete a user |
| GET | `/api/admin/bookings` | all bookings |
| PUT | `/api/admin/bookings/{id}/cancel` | force-cancel |
| GET | `/api/admin/issues` | issue inbox |
| PUT | `/api/admin/issues/{id}` | set status + reply note |

### 7.15 WebSocket
| Endpoint | Direction | Payload |
|---|---|---|
| `ws://<host>:8080/ws` | server → client | `availability_changed` events (service id + availability) |

---

## 8. Database Design

**Engine:** InnoDB, **charset:** utf8mb4. Schema auto-maintained by Hibernate
(`ddl-auto=update`) and fully documented in `schema.sql`.

### 8.1 Tables and relationships

```
users ──1:N── vehicles
  │              │
  │              └────────────┐
  ├──1:N── stations ──1:N── station_services
  │           │                    │
  │           │                    │
  │           └──1:N── bookings ───┘
  │                     │
  │                     ├──1:1── payments        (UNIQUE booking_id)
  │                     └──1:1── reviews         (UNIQUE booking_id)
  ├──1:N── notifications
  ├──1:N── issues ──N:1── stations (optional)
  ├──1:N── fuel_stations ──1:N── fuel_station_inventories  (UNIQUE station+fuel_type)
  └── (operators own both stations and fuel_stations)
news_articles  (standalone cache, UNIQUE link)
```

### 8.2 Key design decisions
| Decision | Rationale |
|---|---|
| `password_hash VARCHAR(255)` | BCrypt output; never plain text |
| `DECIMAL(10,7)/(11,7)` for lat/lng | enough precision for POI mapping |
| `bookings` + index `idx_bookings_conflict(service_id, status, start_time, end_time)` | fast overlap counting for double-booking prevention |
| `station_services.available_slots` | installed capacity (Option B capacity model), not a single-slot model |
| `payments.booking_id` UNIQUE | exactly one payment per booking |
| `reviews.booking_id` UNIQUE | one review per verified session (prevents review spam) |
| `reviews.rating` CHECK 1–5 | data integrity at the DB level |
| `notifications.type VARCHAR(30)` | extensible; avoids enum-migration breakage |
| Soft-ish delete (`ON DELETE SET NULL`) for `issues.station_id` | a report survives its station being removed |
| Timestamps `created_at` / `updated_at` everywhere | auditability |

---

## 9. Android Client Structure

Base package: `com.example.evfinder` — **66 Kotlin source files**

```
com/example/evfinder/
├── MainActivity.kt                  role switch + app shell + NavHost
├── core/
│   ├── model/     StationDtos, BookingDtos, NotificationDto, IssueDtos, NewsDtos, FuelStationDtos
│   ├── network/   ApiClient, ApiService, AuthInterceptor, AvailabilitySocket, OverpassClient
│   └── storage/   TokenStore (JWT + role + user id)
├── feature/
│   ├── auth/          LoginScreen, RegisterScreen, AuthRepository, AuthViewModel, EvTextField
│   ├── home/          HomeScreen (dashboard), HomeViewModel
│   ├── map/           MapScreen, MapViewModel
│   ├── station/       StationDetailScreen, StationRepository
│   ├── booking/       BookingScreen, PaymentScreen, BookingReceiptScreen,
│   │                  BookingsScreen (+ViewModels, BookingRepository)
│   ├── vehicle/       VehiclesScreen, VehiclesViewModel, VehiclesRepository
│   ├── notifications/ NotificationsScreen, NotificationsViewModel, UnreadNotifications
│   ├── news/          NewsScreen, NewsViewModel, NewsRepository
│   ├── fuel/          FuelStationDetailScreen, FuelUi, FuelViewModels,
│   │                  FuelRepository, OperatorFuelStationsScreen
│   ├── support/       ReportIssueScreen, ReportIssueViewModel, IssueRepository
│   ├── operator/      OperatorApp, OperatorHomeScreens, OperatorStationsScreen,
│   │                  OperatorViewModels, OperatorRepository
│   ├── admin/         AdminApp, AdminScreens, AdminViewModels, AdminRepository
│   └── profile/       ProfileScreen, ProfileViewModel
└── ui/
    ├── components/    EvComponents, EvWidgets, EvNavBar, EvNotificationBell
    └── theme/         Theme.kt (EvColors + typography), Color.kt, Type.kt
```

### 9.1 Navigation map (Navigation Compose)

**User shell (`EVFinderApp`)** — bottom tabs: Stations · Bookings · Vehicles · Account
```
login ─► register
home ─┬─► map
      ├─► station/{stationId} ─► book/{serviceId} ─► payment/{bookingId}
      │                              └─► receipt/{bookingId}
      ├─► fuel/{fuelStationId}
      ├─► news
      ├─► notifications
      ├─► report   |   report/{stationId}/{stationName}
      └─► vehicles, profile
bookings ─► station/{stationId}
```
**Operator shell (`OperatorApp`)** — tabs: Dashboard · Stations · Bookings · Fuel · Profile
(plus `op_notifications`, `op_report`)
**Admin shell (`AdminApp`)** — tabs: Overview · Users · Bookings · Reports · Profile
(plus `admin_notifications`, `admin_report`)

30 distinct routes in total.

### 9.2 Screen inventory (by role)

**USER — 14 screens:** Home dashboard, Full map, Station detail, Booking (slot picker),
Payment, Receipt, My bookings, Vehicles, Profile, Notifications, News list, Fuel-station
detail, Issue report, Login/Register.

**OPERATOR — 6 screens:** Dashboard (KPIs), Stations (list + map-picker create/edit +
service management), Customer bookings, Fuel-station management (CRUD + inventory), Profile,
Notifications, Report an issue.

**ADMIN — 6 screens:** Command Center (platform KPIs), Users (role change, delete),
All bookings (force-cancel), Issue reports inbox (triage + reply), Profile, Notifications.

### 9.3 State management (MVVM)
- Each screen has a `ViewModel` exposing `StateFlow<UiState>` where `UiState` is an immutable
  data class; the UI is a pure function of that state.
- Parameterised screens (payment, booking, fuel detail, news preview) use
  `viewModelFactory { initializer { … } }`.
- Shared, app-wide state uses singletons: `TokenStore` (session) and `UnreadNotifications`
  (polled unread badge).
- One-shot navigation events use `LaunchedEffect` keyed on state (e.g. `createdBooking`).

### 9.4 Networking layer
- `ApiClient` — Retrofit instance, base URL from a single `HOST_IP` constant, OkHttp with a
  20 s connect / 30 s read timeout and `AuthInterceptor` adding the bearer token.
- `ApiService` — ~60 endpoint declarations grouped by feature.
- `AvailabilitySocket` — OkHttp WebSocket to `/ws` exposing a `SharedFlow` of events;
  screens re-fetch silently on each event.
- `OverpassClient` — Overpass QL query for public fuel/LPG POIs with a hand-curated Dhaka
  seed fallback so the layer is never empty.
- Repository classes translate HTTP failures into human messages
  (401 → "Session expired", 403 → "You don't have access", etc.).

### 9.5 Design system
A dark "Voltage Mobility" theme defined once in `ui/theme/Theme.kt` (~30 named tokens) and
consumed by every screen:

| Token group | Examples |
|---|---|
| Surfaces | `Background #131313`, `SurfaceLowest`, `SurfaceLow`, `Surface`, `SurfaceHigh`, `SurfaceHighest` |
| Content | `OnBackground/OnSurface #E5E2E1`, `OnSurfaceVar #BCCBB9` |
| Brand | `Primary #4BE277`, `PrimaryContainer #22C55E`, `PrimaryDim`, `OnPrimary #003915` |
| Accents | `Secondary #4EDEA3`, `Tertiary #AFC7FF`, `Warning #FFBB33` |
| State | `Error #FFB4AB`, `ErrorContainer #93000A` |
| Inputs | `InputBackground`, `InputBorder`, `SurfaceBorder` |

Shared components (`ui/components`): `EvPrimaryButton`, `EvOutlinedButton`, `EvCard`,
`EvTextField`, `EvTopBar`, `EvFilterChip`, `EvStatBlock`/`EvStatTile`, `EvProgressBar`,
`EvProgressRing`, `EvSettingRow`, `EvToggle`, `EvSectionHeader`, `EvSpecTile`, `StatusPill`,
`LiveDot`, `EvStepBar`, `EvBottomNavBar` (pill tabs with green hover), `EvNotificationBell`
(animated unread badge: bell swing + badge pop + glow ring on new notification).

---

## 10. Feature Catalogue (detailed)

### 10.1 Authentication & session
Registration and login return a JWT plus the user's role; the role selects which app shell is
mounted. Passwords are BCrypt-hashed. The client stores the token locally and attaches it to
every request; a 401 surfaces a "Session expired → log in again" action instead of a dead end.

### 10.2 Home dashboard (USER)
Search bar, category toggle (EV charging / fuel stations), map preview with EV + fuel/LPG
markers, sort and filter chips (Top rated, Most reviewed, Price, A–Z, Available now), an
upcoming-booking banner, a featured station card, a compact station list, a fuel/LPG section,
and the **Energy & fuel updates** news carousel. Live/Offline indicator reflects the WebSocket.

### 10.3 Station discovery & map
Full-screen osmdroid map with markers for EV stations (green), fuel (amber) and LPG (blue),
a legend, POI info cards, and Get Directions handing off to a navigation app. Search filters
by name/address; results are ranked by average rating then review count, so the best-rated
station is always first.

### 10.4 Booking flow (USER)
Vehicle → date → time slot → continue. The slot grid shows remaining capacity per slot; the
backend rejects a booking that would exceed capacity with HTTP 409 and a clear message. The
flow is: Booking (PENDING) → Payment → Receipt (booking reference) → My Bookings.

### 10.5 Payment (simulated)
A payment screen with express-wallet buttons and a card form, a real cost breakdown built from
the booking, and a "simulate failure" switch for demonstration. Wallet buttons map to the
backend's `MOBILE_BANKING` method. Success → CONFIRMED booking + notification; failure → slot
released and the user is told. Cancelling a paid booking marks the payment REFUNDED.

### 10.6 Reviews & rating
A finished session (status COMPLETED, or a CONFIRMED booking whose slot has elapsed) shows
"Rate this station". One review per booking, submitted with a star rating and optional
comment. The station's average rating is recomputed and drives list ranking; the station
detail screen shows the rating summary and all reviews.

### 10.7 Notifications (all roles)
Users get booking/payment/review/issue outcomes; operators get "new booking" and "cancellation"
alerts for their stations; admins get "new issue reported". The bell badge polls the unread
count every 20 s, animates on a new arrival, and clears when the inbox is read.

### 10.8 Vehicles (USER)
Full CRUD for the user's EVs: manufacturer, model, type, connector, battery capacity,
registration number. A vehicle must exist before booking.

### 10.9 Operator module
Dashboard KPIs (stations, active, bookings, revenue-style counters) with progress visuals;
station list with edit; station creation via an **Uber-style map picker** (tap the map, the
coordinates fill the form); service management (add/edit/remove charge points and swap bays
with type, connector, kW, price, slots); a customer-bookings list for their stations; and a
fuel-station manager with per-fuel inventory updates (queue, litres, price).

### 10.10 Admin module
Command Center with platform KPIs; user management (search, filter by role, change role,
delete); all-bookings view with force-cancel; and the **issue inbox** with status tabs
(Open / Working / Resolved / All), a reply dialog that notifies the reporter, and
`/api/news/refresh` for the news cache.

### 10.11 Fuel station module
A separate domain from EV stations (no booking, no payment). Users browse pumps filtered by
fuel type and stock, and see queue length, remaining litres and BDT/litre updated by the
operator. Operators create stations on the map picker and maintain inventory per fuel type.

### 10.12 Issue reporting
Any signed-in user files a report (category, subject, description, optionally attached to a
station — reachable directly from the station detail screen). Every admin is notified. Admins
triage with a status and a reply note; the reporter sees the status and the admin's reply in
the app and receives a notification.

### 10.13 Energy / fuel / EV news
The backend queries Google News RSS for four search phrases (fuel prices, LPG, EV charging,
energy policy), parses the RSS, de-duplicates by link, caches in `news_articles`, and serves
the newest items at `/api/news`. A refresh runs at startup and whenever the cache is older
than 30 minutes; admins can force it. Home shows the newest few as a horizontal carousel;
"See all" opens a filterable list. Tapping a headline opens the original article. Nothing is
fabricated — if the feed is unreachable and the cache is empty, the section stays empty.

### 10.14 Support & profile
Profile shows identity, stats, account actions (edit profile, change password), appearance,
notification preferences, a Support entry to report an issue, and logout. Operator/admin share
the same profile screen with role-appropriate entries.

---

## 11. Core Algorithms and Business Logic

### 11.1 Double-booking prevention (capacity model)
A service declares installed capacity (`available_slots`). On booking, inside one transaction:

```java
long overlapping = bookingRepository.countActiveOverlapping(
        request.serviceId(), request.startTime(), request.endTime());
if (overlapping >= service.getAvailableSlots())
    throw new BookingUnavailableException("No free slots left for the selected time");
```
`countActiveOverlapping` counts PENDING/CONFIRMED bookings whose `[start, end)` overlaps the
requested window (half-open interval, so back-to-back bookings don't collide), supported by
`idx_bookings_conflict`. Verified: a third concurrent booking against capacity 2 returns 409.

### 11.2 Live availability ("how much is free right now")
Static capacity alone would always display 100 %. The API therefore computes, per request:

```java
SELECT b.service.id, COUNT(b) FROM Booking b
WHERE b.status IN ('PENDING','CONFIRMED') AND b.startTime < :now AND b.endTime > :now
GROUP BY b.service.id
```
`availableSlots = capacity − activeNow` (floored at 0), and the response additionally carries
`capacitySlots` as the denominator. The client renders a colour-coded bar (green > 50 %,
amber > 15 %, red below) with the percentage and the raw counts. Because a broadcast is sent
on every booking/cancel, the bars update live.

### 11.3 Rating-based ranking
```java
.sorted(Comparator.comparingDouble(StationResponse::averageRating).reversed()
        .thenComparing(Comparator.comparingInt(StationResponse::reviewCount).reversed())
        .thenComparing(StationResponse::name))
```
Highest-rated first; review count breaks ties; the name makes ordering deterministic.

### 11.4 Slot generation
For a requested date, slots are generated from the station's opening/closing hours in
one-hour steps; each slot reports capacity, remaining slots and a `bookable` flag (remaining
> 0, and not in the past for today).

### 11.5 Notifications — robustness pattern
Notification creation must never break the business operation it reports on:
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void notify(...) {
    try { notificationRepository.save(...); }
    catch (Exception e) { log.warn("Could not create notification: {}", e.getMessage()); }
}
```
This was introduced after an enum value mismatch rolled back a whole booking; it is the reason
a notification failure can no longer destroy a successful reservation.

### 11.6 Simulated payment
`PaymentServiceImpl` records a `Payment` row with a generated transaction reference
(`SIM-XXXXXXXX`); `forceFailure` drives the failure path. Success flips the booking to
CONFIRMED and notifies both user and operator; failure releases the slot and notifies the user.
Cancelling a paid booking sets the payment to REFUNDED.

### 11.7 News retrieval & caching
1. Warm the cache at `ApplicationReadyEvent` on a daemon thread.
2. `latest()` refreshes synchronously only if the cache is empty (first run), otherwise
   triggers a background refresh when stale (> 30 min) and returns cached rows immediately.
3. Parsing uses JDK `DocumentBuilder` with DTDs disabled and entity expansion off
   (XXE-hardened, since feeds are third-party input).
4. Rows are de-duplicated by unique `link` and the cache is pruned to stay bounded.

### 11.8 Overpass POI fetch with fallback
Public fuel/LPG POIs are queried from two Overpass mirrors; if both fail or return nothing, a
curated Dhaka seed is used. The seed is painted immediately so the fuel layer is never blank
while the network request is in flight.

---

## 12. Non-Functional Aspects

### 12.1 Security
- BCrypt password hashing; no plain-text passwords anywhere.
- Stateless JWT (HS256) with a role claim; verified on every request by a filter.
- Method-level authorisation (`@PreAuthorize`) on operator/admin controllers.
- Ownership checks inside services (a user can only read/cancel their own bookings and
  vehicles; an operator only edits their own stations).
- DTO boundary: entities are never serialised, so lazy-loaded relations can't leak
  (`open-in-view=false`).
- XML parsing hardened against XXE.
- CORS/WebSocket origin: `setAllowedOrigins("*")` — acceptable for a course demo, flagged as a
  production hardening item.

### 12.2 Robustness
- Centralised exception handler with meaningful messages and correct status codes.
- Non-fatal notifications (see 11.5).
- Silent background refreshes that keep the last good data instead of replacing it with an
  error screen.
- External dependency fallbacks (Overpass seed, news cache).
- Optimistic-UI-safe immutable state; no lost updates on recomposition.

### 12.3 Performance
- Overlap counting backed by a composite index.
- Single grouped query for live availability (avoids N+1 across stations).
- News served from a local cache rather than proxying the feed per request.
- osmdroid tiles cached by the library; map markers rebuilt only when the data key changes.

### 12.4 Usability / accessibility
- Consistent dark theme and shared components across all three roles.
- Content descriptions on icon-only buttons.
- Explicit loading, empty and error states on every data screen.
- Colour-coded availability with numeric labels (not colour alone).
- Back navigation, and a navigation bar whose items highlight on hover as well as selection.

---

## 13. Setup and Running

### 13.1 Prerequisites
JDK 17, Android Studio (SDK 35), XAMPP (MySQL), Git.

### 13.2 Database
1. Start **Apache + MySQL** from the XAMPP control panel.
2. Create the schema: `CREATE DATABASE ev_finder;` (Hibernate can also auto-create it —
   the datasource URL includes `createDatabaseIfNotExist=true`).
3. Tables are created/updated by Hibernate; `schema.sql` documents the full DDL.

### 13.3 Backend
```bash
cd "EV finder/EV-finder-api"
./mvnw spring-boot:run          # → http://localhost:8080
```
Seeds automatically on first run: demo accounts, 3 EV stations with services, and 4 fuel
stations with per-fuel inventory.

### 13.4 Android client
1. Set the backend host in
   `core/network/ApiClient.kt` → `HOST_IP` (your PC's LAN IPv4 from `ipconfig`). Use
   `10.0.2.2` for an emulator in some configurations.
2. Phone/emulator must be on the same network as the backend.
3. Build & run:
   ```bash
   cd "EV finder/EV-finder-android"
   ./gradlew :app:assembleDebug        # or Run ▶ in Android Studio
   ```

### 13.5 Demo accounts
| Role | Email | Password |
|---|---|---|
| Admin | `admin@ev.com` | `admin123` |
| Operator | `operator@ev.com` | `operator123` |
| User | `test@ev.com` | `secret123` |

---

## 14. Verification Performed

| Area | How it was verified | Result |
|---|---|---|
| Backend compiles | `./mvnw -o compile` | Build success |
| Android compiles & packages | Gradle `:app:assembleDebug` | APK produced, no errors |
| Registration / login | On-device, real backend | Works; role selects the shell |
| Double-booking prevention | 3rd booking against capacity 2 | HTTP 409 (as designed) |
| Simulated payment success/failure | On-device, both paths | Success → CONFIRMED; failure → slot released |
| Receipt hand-off | Payment → receipt → My Bookings | Works |
| Tab navigation | All four tabs + Back | Correct; a stale-back-stack bug was found and fixed |
| Live availability bars | Booking made while watching Home | Percentage updates live |
| Ratings & ranking | Review submitted | Station average updated (4.5 from 2 reviews) |
| Reviews CTA | Rated vs unrated finished bookings | CTA hidden when already rated |
| Notifications | Booking/payment/issue flows | Delivered; bell badge animates |
| Issue report loop | User files → admin replies | Status + reply visible to the reporter, notification sent |
| Fuel module (API) | Endpoints exercised | Browse/create/inventory works |
| News feed source | Live RSS fetch | Real headlines returned (Reuters, The Daily Star, The Business Standard) |

**Pending runtime verification:** the `/api/news` endpoint and the app's news carousel have
not been exercised end-to-end, because at the time of writing **MySQL (XAMPP) and the Spring
Boot backend were both stopped** on the machine. The code compiles and the upstream RSS feed
was confirmed reachable; start XAMPP + the backend and open Home to see the section populate.

---

## 15. Known Limitations and Future Work

### 15.1 Limitations
1. **No scheduler to complete bookings.** Nothing flips a CONFIRMED booking to COMPLETED, so
   the app treats a confirmed booking whose slot has ended as reviewable. A scheduled job
   would make the domain model exact.
2. **Two "fuel" concepts coexist.** The curated/OSM fuel-LPG layer on Home (public pumps with
   directions) and the operator-managed fuel-station module (queue/stock/price) are separate
   data sources. They could be unified.
3. **Single-instance, no queue.** The demo backend runs one node; MySQL transactions provide
   the concurrency guarantee, not a distributed lock.
4. **News depends on an external feed.** Offline, the section shows what is cached (or an
   empty state), and it never fabricates content.
5. **WebSocket origin is open** and the JWT secret is a placeholder — production hardening.
6. **Profile notification preferences are device-local**, not persisted server-side.
7. **`play-services-maps` is still declared** but unused (legacy; the app maps via osmdroid).
8. **No automated test suite** — verification has been manual plus compile checks.
9. **A parallel review implementation from a teammate's branch was intentionally not merged**,
   because it would duplicate the existing booking-linked review system with a second table
   and a second rating source.

### 15.2 Suggested future work
- A scheduled job (`@Scheduled`) for booking auto-completion and no-show expiry.
- Email/SMS or push notifications in addition to in-app ones.
- Server-persisted notification preferences and quiet hours.
- Payment gateway integration behind the existing simulated interface.
- Unit/integration tests (JUnit + MockMvc; Compose UI tests).
- Analytics dashboards for operators (utilisation over time).
- Offline caching on the client (Room) for stations and bookings.
- Consolidating the two fuel data sources into one canonical module.

---

## 16. Appendix

### 16.1 Backend component counts
| Component | Count |
|---|---|
| REST controllers | 16 |
| Entities / enums | 12 / 10 |
| Repositories | 12 |
| Service interfaces / implementations | 11 / 12 |
| DTO records | 30 |
| Exception types | 6 + global handler |
| WebSocket handlers | 1 |

### 16.2 Android counts
| Component | Count |
|---|---|
| Kotlin source files | 66 |
| Packages | 20 |
| Navigation routes | 30 |
| User / operator / admin screens | 14 / 7 / 6 |
| Shared UI components | 20+ |
| Design tokens | ~30 |

### 16.3 Git history (milestones)
```
640af94  Merge fuel station module + add live energy/fuel/EV news feed
02ccb65  Issue reporting, live availability bars, shared nav bar, animated bell
c86009c  Fix tab navigation, restore review/rating flows, enrich fuel stations
61032a0  Merge branch 'member1-handoff'
48c9067  Premium UI rebuild, notifications, ratings, booking receipt, KPIs
```

### 16.4 Glossary
| Term | Meaning |
|---|---|
| AOOP | Advanced Object-Oriented Programming (the course) |
| Slot | A one-hour bookable window at a service |
| Service | A charge point or battery-swap bay belonging to a station |
| Capacity model (Option B) | A service holds N slots; bookings consume capacity for their window |
| Verified session review | A review tied to a booking, one per booking |
| Availability | Free capacity right now = capacity − bookings covering the current instant |
| Overpass | Free OSM query service used for public fuel/LPG POIs |
