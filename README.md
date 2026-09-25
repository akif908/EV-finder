# ⚡ EV Finder — EV Charging & Battery-Swap Finder and Booking System

A full-stack mobile platform for **finding, reserving and paying for EV charging and battery-swap
slots**, with a parallel **fuel / LPG station module** and a built-in **energy news feed**.

> **Native Android client (Kotlin + Jetpack Compose) · Java Spring Boot REST API · MySQL · JWT · WebSocket**
> AOOP (Advanced Object-Oriented Programming) course project.

---

## 📖 Table of Contents

- [What it does](#-what-it-does)
- [Roles at a glance](#-roles-at-a-glance)
- [Feature highlights](#-feature-highlights)
- [Tech stack](#-tech-stack)
- [Architecture](#-architecture)
- [Quick start](#-quick-start)
- [Demo accounts](#-demo-accounts)
- [Project structure](#-project-structure)
- [API overview](#-api-overview)
- [Database](#-database)
- [Development process](#-development-process)
- [Verification status](#-verification-status)
- [Documentation index](#-documentation-index)
- [Known limitations & roadmap](#-known-limitations--roadmap)
- [Credits](#-credits)

---

## 🎯 What it does

EV adoption is held back by *range anxiety* — drivers don't know whether a charger is free, and
battery-swap points, petrol pumps and LPG stations have no live status anywhere. EV Finder solves
this for three kinds of people:

| Who | What they get |
|---|---|
| **Driver (USER)** | Find stations on a map, see **live** availability, book a slot for their own EV, pay, get a receipt, then rate the station |
| **Station owner (OPERATOR)** | Publish stations and services from a map picker, watch customer bookings, keep fuel inventory up to date |
| **Platform manager (ADMIN)** | Platform KPIs, user and role management, booking oversight, and triage of user-reported issues |

The system also answers a broader question — *"where can I refuel, and what's the situation
there?"* — through a fuel-station module (queue length, remaining litres, price per litre) and a
news feed of the latest fuel, LPG, EV and energy-policy headlines.

---

## 👥 Roles at a glance

| Capability | USER | OPERATOR | ADMIN |
|---|:--:|:--:|:--:|
| Register / log in | ✅ | ✅ | ✅ |
| Manage own vehicles | ✅ | — | — |
| Browse stations, map, news | ✅ | ✅ | ✅ |
| Book a slot & pay (simulated) | ✅ | — | — |
| Rate a finished session | ✅ | — | — |
| Report an issue | ✅ | ✅ | ✅ |
| Manage own stations & fuel inventory | — | ✅ | ✅ |
| See customer bookings | — | ✅ (own) | ✅ (all) |
| Platform KPIs, user/role admin, issue triage | — | — | ✅ |

---

## ✨ Feature highlights

### For drivers
- **Dashboard home** — search, sort (Top rated / Most reviewed / Price / A–Z / Available now),
  featured station card, upcoming-booking banner, fuel section and news carousel.
- **Live availability bars** — a colour-coded percentage (green > 50 %, amber > 15 %, red below)
  computed from real bookings, updating live over WebSocket.
- **Free map** — osmdroid + OpenStreetMap with green EV, amber fuel and blue LPG markers,
  POI info cards and one-tap hand-off to turn-by-turn navigation. No API key, no billing.
- **Booking → Payment → Receipt** — pick vehicle, date and time; conflicts are rejected
  server-side; simulated payment with a real cost breakdown and a demo-failure switch;
  a receipt with a booking reference.
- **Ratings that matter** — one review per completed session; the station's average rating
  decides its position in every list, so the best stations surface first.
- **Notifications** — an animated bell badge (swings + pops on a new arrival) and a full inbox.
- **Fuel & LPG** — browse pumps filtered by fuel type and stock, with queue/stock/price detail.
- **Issue reporting** — report a bug or station problem straight to the admins and follow
  the reply.

### For operators
- KPI dashboard (stations, active, bookings, counters with progress visuals).
- Create/edit stations on an **Uber-style map picker**; manage charge points and swap bays
  (type, connector, kW, price, slots).
- Customer booking list for their own stations.
- Fuel-station manager: per-fuel queue length, remaining litres and BDT price.

### For admins
- Command Center with platform-wide KPIs.
- User management: filter by role, change roles, delete accounts.
- All bookings with force-cancel.
- Issue inbox with status tabs (Open / Working / Resolved / All) and a reply that notifies
  the reporter.
- Manual refresh of the news cache.

### Engineering highlights
- **Double-booking prevention** — capacity-based overlap counting inside the booking transaction.
- **Live availability** — one grouped query returns current occupancy for every service (no N+1).
- **Rating-ranked search** — average rating → review count → name, deterministic ordering.
- **Resilient notifications** — created in their own transaction and never able to roll back the
  business action that triggered them.
- **Graceful external dependencies** — Overpass POI fetch falls back to a curated seed; news is
  cached so the section survives an unreachable feed.
- **Consistent design system** — ~30 named colour tokens and 20+ shared Compose components, so
  all three role shells look and behave the same.

---

## 🧰 Tech stack

### Backend
| Concern | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.1 (`webmvc` starter) |
| Persistence | Spring Data JPA + Hibernate ORM |
| Database | MySQL 8 (XAMPP) |
| Security | Spring Security + JWT (jjwt 0.12.6), BCrypt |
| Real-time | Spring WebSocket (raw handler at `/ws`) |
| Validation | Jakarta Bean Validation |
| Boilerplate | Lombok |
| Build | Maven (`mvnw` wrapper) |

### Android client
| Concern | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM, unidirectional data flow (`StateFlow`) |
| Navigation | Navigation Compose (30 routes, 3 role shells) |
| Networking | Retrofit 2.11 + Gson, OkHttp 4.12 (+ logging) |
| Real-time | OkHttp WebSocket client |
| Maps | osmdroid 6.1.18 + OpenStreetMap tiles (free) |
| SDK | compileSdk/targetSdk 35, minSdk 24, JVM 17 |

### Free services (no API key, no billing)
| Service | Used for |
|---|---|
| OpenStreetMap tiles | Base map imagery |
| Overpass API | Public fuel / LPG points of interest |
| Google News RSS | Energy, fuel, LPG and EV headlines |

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                  Android client (Kotlin + Compose)                   │
│    Role shells:  EVFinderApp (USER) │ OperatorApp │ AdminApp         │
│    UI (Compose) → ViewModel → Repository → Retrofit / WebSocket       │
│    Local: TokenStore (JWT + role) · osmdroid MapView                 │
└───────────────┬──────────────────────────────────┬───────────────────┘
                │ REST + JWT bearer                │ WebSocket ws://…/ws
                ▼                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│                 Spring Boot API — modular monolith                   │
│   Controllers → Services → impl → Repositories (JPA) → Entities      │
│   Security (JWT filter · @PreAuthorize) · GlobalExceptionHandler     │
│   NotificationService · AvailabilityWebSocketHandler · DemoDataSeeder │
└───────────────┬──────────────────────────────────┬───────────────────┘
                │ Hibernate / JDBC                 │ server-side HTTP
                ▼                                  ▼
        ┌───────────────┐              ┌──────────────────────────────┐
        │ MySQL (12     │              │ Overpass API · Google News   │
        │ tables)       │              │ RSS · OSM tile servers       │
        └───────────────┘              └──────────────────────────────┘
```

**Request flow example (booking):**
`Compose screen → BookingViewModel → BookingRepository → Retrofit → BookingController →
BookingServiceImpl` (capacity check + overlap count + save + WebSocket broadcast + notify operator)
`→ BookingResponse → state update → UI renders PENDING → Payment screen`

---

## 🚀 Quick start

### 1. Prerequisites
- **JDK 17**
- **Android Studio** (Android SDK 35)
- **XAMPP** (MySQL on port 3306)
- **Git**

### 2. Start the database
1. Open the XAMPP Control Panel and start **MySQL**.
2. Create the database (Hibernate can also auto-create it):
   ```sql
   CREATE DATABASE ev_finder;
   ```

### 3. Run the backend
```bash
cd EV-finder-api
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```
API comes up on **http://localhost:8080**. Tables are created/updated automatically by
Hibernate, and demo data seeds on first run (accounts, 3 EV stations, 4 fuel stations).
Full DDL for reference: [`schema.sql`](schema.sql).

### 4. Run the Android app
1. Point the client at your backend — edit
   [`ApiClient.kt`](EV-finder-android/app/src/main/java/com/example/evfinder/core/network/ApiClient.kt):
   ```kotlin
   private const val HOST_IP = "192.168.x.x"   // your PC's LAN IPv4 (run: ipconfig)
   ```
   The phone/emulator must be on the same network as the backend.
2. Build and install:
   ```bash
   cd EV-finder-android
   ./gradlew :app:assembleDebug     # or press ▶ Run in Android Studio
   ```

---

## 🔑 Demo accounts

| Role | Email | Password | Lands on |
|---|---|---|---|
| **User** | `test@ev.com` | `secret123` | Stations / Home dashboard |
| **Operator** | `operator@ev.com` | `operator123` | Operator dashboard |
| **Admin** | `admin@ev.com` | `admin123` | Command Center |

The role stored in the JWT decides which app shell is mounted — there is no role switcher in the UI.

---

## 📁 Project structure

```
EV finder/
├── README.md                    ← you are here
├── schema.sql                   ← full MySQL DDL (12 tables)
├── schema-changes.md            ← schema deltas vs the original diagram
├── docs/
│   ├── PROJECT-REPORT.md        ← full technical report (features, structures, algorithms)
│   └── ev finder ui.zip         ← UI design references
│
├── EV-finder-api/               ← Spring Boot backend
│   ├── pom.xml · mvnw
│   └── src/main/java/com/example/EV_finder_api/
│       ├── config/        SecurityConfig · WebSocketConfig · DemoDataSeeder
│       ├── controller/    16 REST controllers
│       ├── dto/           30 request/response records
│       ├── entity/        12 entities + 10 enums
│       ├── exception/     6 exceptions + GlobalExceptionHandler
│       ├── repository/    12 Spring Data repositories
│       ├── security/      JwtService · JwtAuthenticationFilter · CurrentUserProvider
│       ├── service/       11 interfaces + 12 implementations
│       └── websocket/     AvailabilityWebSocketHandler
│
└── EV-finder-android/           ← Kotlin Android client (66 source files)
    └── app/src/main/java/com/example/evfinder/
        ├── MainActivity.kt      role switch + app shell + NavHost
        ├── core/
        │   ├── model/           DTOs (stations, bookings, notifications, issues, news, fuel)
        │   ├── network/         ApiClient · ApiService · AvailabilitySocket · OverpassClient
        │   └── storage/         TokenStore (JWT + role)
        ├── feature/
        │   ├── auth/ home/ map/ station/ booking/ vehicle/
        │   ├── notifications/ news/ fuel/ support/
        │   ├── operator/        dashboard · stations · bookings · fuel management
        │   ├── admin/           overview · users · bookings · issue inbox
        │   └── profile/
        └── ui/
            ├── components/      EvCard · EvPrimaryButton · EvBottomNavBar · EvNotificationBell …
            └── theme/           EvColors design tokens + typography
```

---

## 🔌 API overview

All endpoints return JSON; every endpoint outside `/api/auth/**` requires
`Authorization: Bearer <jwt>`.

| Group | Base path | Highlights |
|---|---|---|
| Authentication | `/api/auth` | register, login → JWT + role |
| Profile | `/api/users/me` | read, update, change password |
| Vehicles | `/api/vehicles` | CRUD (owner-scoped) |
| Stations | `/api/stations` | search (rating-ranked), nearby, details |
| Availability | `/api/services/{id}/slots` | slot grid for a date |
| Bookings | `/api/bookings` | create (409 on conflict), list, cancel |
| Payments | `/api/payments/{bookingId}` | simulated success / failure |
| Reviews | `/api/reviews` | one per booking; station aggregates |
| Notifications | `/api/notifications` | list, unread count, mark read |
| Issue reports | `/api/issues` | file a report, track status + reply |
| News | `/api/news` | cached energy/fuel/EV headlines |
| Fuel stations | `/api/fuel-stations` | browse, filter, nearby, detail |
| Operator | `/api/operator/**` | station + service + fuel CRUD, customer bookings |
| Admin | `/api/admin/**` | KPIs, users, roles, bookings, issue triage |
| Real-time | `ws://<host>:8080/ws` | `availability_changed` broadcasts |

> The complete endpoint-by-endpoint reference (with request/response fields and role
> requirements) is in [`docs/PROJECT-REPORT.md`](docs/PROJECT-REPORT.md#7-rest-api-reference).

---

## 🗄 Database

12 tables, InnoDB / utf8mb4. Schema is auto-maintained by Hibernate (`ddl-auto=update`) and
documented in [`schema.sql`](schema.sql).

```
users ──1:N── vehicles
  ├──1:N── stations ──1:N── station_services
  │            └──1:N── bookings ──┬──1:1── payments   (UNIQUE booking_id)
  │                                └──1:1── reviews    (UNIQUE booking_id)
  ├──1:N── notifications
  ├──1:N── issues ──N:1── stations (optional context)
  └──1:N── fuel_stations ──1:N── fuel_station_inventories (UNIQUE station+fuel_type)
news_articles   (standalone cache, UNIQUE link)
```

Notable design choices: `password_hash` (BCrypt, never plain text) · composite index
`idx_bookings_conflict(service_id, status, start_time, end_time)` for overlap checking ·
`available_slots` as installed capacity · one payment and one review per booking ·
`DECIMAL(10,7)/(11,7)` coordinates.

---

## 🛠 Development process

The project was built in **vertical slices** — each phase delivered a working end-to-end
feature rather than a layer in isolation:

| Phase | Delivered |
|---|---|
| **0 · Design** | Project context, architecture diagram (draw.io), ER schema, UI mockups; inconsistencies in the original artifacts corrected (`schema-changes.md`) |
| **1 · Foundations** | Backend scaffold, MySQL schema, JWT auth + BCrypt, Android project, Retrofit client, role-based shells, login/register |
| **2 · Core data** | Stations, services, vehicles; station list, detail, map (osmdroid + OSM) |
| **3 · Booking engine** | Slot generation, capacity/overlap conflict control, booking lifecycle |
| **4 · Payment & receipt** | Simulated gateway, success/failure paths, refund on cancel, receipt with reference |
| **5 · Operator module** | KPI dashboard, map-picker station creation, service management, customer bookings |
| **6 · Admin module** | Platform overview, user & role management, booking oversight |
| **7 · Engagement** | Notifications (all roles) + WebSocket live availability, ratings/reviews driving ranking, search filters |
| **8 · Design system** | Full Stitch-inspired visual rebuild: shared components, colour tokens, animated payment flow, redesigned auth screens |
| **9 · Extensions** | Issue reporting (user → admin), fuel-station module with inventory, live energy/fuel/EV news feed |
| **10 · Hardening & docs** | Live availability bars, animated notification badge, tab-navigation fix, full project report, this README |

### Team workflow
Work was split across teammates using **feature branches and handoffs**:

```
main                         ← integrated, always-runnable state
├── member1-handoff          ← UI/UX screens, design system, fuel + news work
└── member2-handoff          ← booking/payment, WebSocket, backend features
```

Contributions arrived via branches/PRs and were **selectively integrated** into `main` rather
than merged blindly — each incoming screen was checked for regressions before adoption, and
duplicate or conflicting implementations (for example a second, parallel review system) were
deliberately left out to keep a single source of truth.

---

## ✅ Verification status

| Area | Result |
|---|---|
| Backend compiles (`mvnw -o compile`) | ✅ |
| Android builds & packages (`assembleDebug`) | ✅ |
| Register / login, role routing | ✅ verified on device |
| Double-booking prevention | ✅ 3rd booking vs capacity 2 → HTTP 409 |
| Simulated payment success **and** failure | ✅ both paths verified |
| Booking → receipt → my bookings hand-off | ✅ |
| Tab navigation & back stack | ✅ (a stale-stack bug was found and fixed) |
| Live availability bars | ✅ update live on booking |
| Ratings & rating-based ranking | ✅ average recalculated, list reordered |
| Notifications, animated badge | ✅ |
| Issue report → admin reply loop | ✅ reporter notified |
| Fuel module (browse, CRUD, inventory) | ✅ |
| News feed source reachability | ✅ live RSS returns real headlines |
| **News endpoint end-to-end in the app** | ⏳ pending — needs MySQL + backend running to observe |

---

## 📚 Documentation index

| Document | What's inside |
|---|---|
| [`docs/Spring_Boot_Project_Report.md`](docs/Spring_Boot_Project_Report.md) | **Submission report** in the CSE 2118 template — architecture, implementation, endpoint reference, database config, 49-case test results, challenges, conclusion |
| [`docs/HOW-TO-SCREENSHOTS.md`](docs/HOW-TO-SCREENSHOTS.md) | Step-by-step guide to capturing the Section 8 test screenshots (Postman import, run order, which request maps to which figure) |
| [`docs/EV-Finder.postman_collection.json`](docs/EV-Finder.postman_collection.json) | Importable Postman collection — 9 folders, 47 requests with assertions; run the logins + Setup once and the rest authenticate automatically |
| [`docs/run-api-tests.sh`](docs/run-api-tests.sh) | 50-case automated acceptance suite (curl); prints a markdown results table and writes `docs/test-evidence.log` |
| [`docs/validate_collection.js`](docs/validate_collection.js) | Runs the whole Postman collection outside Postman (stubs the `pm` API) — `node docs/validate_collection.js` → 59/59 assertions |
| [`docs/PROJECT-REPORT.md`](docs/PROJECT-REPORT.md) | Full technical report: requirements, architecture, all technologies and versions, structures, complete API reference, DB design, algorithms with code, non-functional aspects, limitations |
| [`schema.sql`](schema.sql) | Complete, runnable MySQL DDL for all 12 tables |
| [`schema-changes.md`](schema-changes.md) | Every change made to the original schema diagram, and why |
| `feature/*/README.md` | Per-feature notes inside the Android client (auth, booking, home, map, operator, admin, station, vehicle, profile) |
| [`EV-finder-android/README.md`](EV-finder-android/README.md) · [`EV-finder-api/README.md`](EV-finder-api/README.md) | Module-level notes |

---

## ⚠️ Known limitations & roadmap

**Current limitations**
1. No scheduler flips a booking to `COMPLETED`, so a confirmed booking whose slot has elapsed
   is treated as reviewable.
2. Two fuel concepts coexist: the public OSM fuel/LPG layer (directions only) and the
   operator-managed fuel stations (queue/stock/price). They could be unified.
3. Payment is simulated by design; the JWT secret and the WebSocket origin are still
   development defaults.
4. Notification preferences are stored on the device, not server-side.
5. No automated test suite yet — verification is manual plus compile checks.

**Roadmap**
- `@Scheduled` job for booking auto-completion and no-show expiry.
- Push/email notifications on top of in-app ones.
- Unit + integration tests (JUnit/MockMvc) and Compose UI tests.
- Server-persisted notification preferences and quiet hours.
- Real payment gateway behind the existing simulated interface.
- Offline caching (Room) for stations and bookings.

---

## 🙏 Credits

Built as an **AOOP course project** by the EV Finder team (`akif908` and collaborators) using
only free and open tooling: Spring Boot, MySQL, Kotlin/Jetpack Compose, OpenStreetMap/osmdroid,
Overpass API and Google News RSS.

No AI/ML, no paid maps, no paid APIs, no real payment gateway — every external dependency is
free and keyless.
