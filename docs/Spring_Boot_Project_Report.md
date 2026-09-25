## United International University

B.Sc. in Computer Science and Engineering (CSE)

CSE 2118: Advanced Object-Oriented Programming

## PROJECT REPORT

## EV Charging & Battery-Swap Finder and Booking System

| | |
|---|---|
| **Student Name** | Akif |
| **Student ID** | *[FILL IN]* |
| **Section** | *[FILL IN]* |
| **Submitted To** | *[FILL IN: course instructor]* |
| **Date of Submission** | *[FILL IN]* |
| **GitHub Repository Link** | https://github.com/akif908/EV-finder |

> **Before submitting:** fill the four fields above and paste an image into every empty
> **FIGURE** box. The figure checklist is in Section 8.3.

---

## Abstract

Electric-vehicle adoption is limited by range anxiety: drivers cannot tell whether a charger is
free, and battery-swap points, petrol pumps and LPG stations publish no live status. This project
implements an EV charging and battery-swap finder as a Spring Boot REST API backed by MySQL, with a
native Android client. The API manages users, vehicles, stations, bookable services, bookings,
simulated payments, reviews, notifications, issue reports and a fuel-inventory module across three
roles — USER, OPERATOR and ADMIN — secured with JSON Web Tokens and BCrypt password hashing.
Booking conflicts are prevented by counting active overlapping bookings against a service's
installed capacity inside a single transaction, and availability changes are pushed to clients over
a WebSocket. The API exposes 54 endpoints across 16 controllers and was verified with two
independent suites: a 50-case command-line acceptance suite and a 47-request Postman collection
carrying 59 assertions. Both cover success *and* failure paths, and both finished with a 100% pass
rate. Testing also uncovered and fixed a real defect: deleting a vehicle that bookings referenced
returned HTTP 500 instead of 409.

---

## 1. Introduction

### 1.1 Background and Problem Statement

An EV driver planning a journey has to answer three questions before leaving: *where* is a charger,
*is it free*, and *can I reserve it*. Today these are answered by scattered apps, phone calls or
guesswork. The same uncertainty applies to conventional fuel: a driver has no way to know that a
nearby pump has run out of octane or that a queue of eight vehicles is waiting.

A backend for this problem must do more than store records. It has to:

- model **capacity** — a station has several charge points, each usable by only one vehicle at a time;
- **refuse** a reservation that would exceed that capacity, even when two users click at the same instant;
- keep availability information **fresh** for every client watching the station;
- **enforce who may do what**, because station owners, drivers and platform administrators need
  different powers;
- record a **payment and a review** so the platform can rank the best stations.

A **REST API** expresses this naturally: resources (stations, bookings, payments) are addressed by
URL, manipulated with standard HTTP verbs, and represented as JSON, so any client — here an Android
app — can consume it. **Spring Boot** is a good fit because its starter dependencies bring in a web
server, an ORM and security with almost no configuration, its embedded Tomcat removes deployment
steps, and dependency injection keeps the layers loosely coupled and testable.

This project builds that API. It is deliberately broader than a single-resource CRUD service: the
booking conflict rule, role-based authorisation and the simulated payment lifecycle carry the real
engineering weight, and they are the parts documented in depth below.

### 1.2 Objectives

1. **Implement** authenticated access with JWT for three roles (USER, OPERATOR, ADMIN) and store
   passwords only as BCrypt hashes.
2. **Model** stations, services and vehicles so that each service has an installed capacity and each
   vehicle belongs to exactly one user.
3. **Prevent** double-booking by rejecting any reservation whose overlapping active bookings would
   exceed a service's capacity, returning HTTP 409.
4. **Implement** a full booking lifecycle (PENDING → CONFIRMED / CANCELLED) with a simulated payment
   that supports both a success path and a forced-failure path.
5. **Expose** a documented REST API with correct status codes (200, 201, 204, 400, 401, 403, 404,
   409) and a consistent JSON error body.
6. **Verify** the API with automated suites that cover both success and failure cases.

### 1.3 Scope

**In scope**

- REST API for: registration/login, profile, vehicles (full CRUD), stations and services,
  availability slots, bookings, simulated payments, reviews and rating aggregation, notifications,
  issue reports, fuel stations with per-fuel inventory, and cached energy news.
- Role-based authorisation for USER, OPERATOR and ADMIN, plus owner-scoped operations.
- MySQL persistence via Spring Data JPA/Hibernate; schema documented in `schema.sql`.
- Live availability broadcast over a WebSocket.
- A native Android client (Kotlin, Jetpack Compose) that consumes the API — included in the
  repository and described in Section 4, though this report concentrates on the backend.

**Out of scope** (deliberately not built)

- Real payment processing — the gateway is simulated; no card data is stored or transmitted.
- AI/ML recommendation or demand prediction.
- IoT integration with physical chargers.
- Email/SMS delivery; notifications are in-app only.
- A web front end and an iOS client.
- Production deployment concerns (containers, CI/CD, horizontal scaling).

---

## 2. Tools and Technologies

| Category | Tool / Version | Why it was used |
|---|---|---|
| Language | Java 17 | LTS release; records, `var` and text blocks used throughout |
| Framework | Spring Boot 4.1.1 | Auto-configuration, embedded Tomcat, starter dependencies |
| Web layer | `spring-boot-starter-webmvc` | REST controllers over embedded Tomcat |
| Persistence | Spring Data JPA / Hibernate ORM | CRUD without boilerplate SQL; derived query methods |
| Database | MySQL 8 (XAMPP) | Free, familiar, reliable relational store |
| Security | Spring Security + jjwt 0.12.6 | Method-level authorisation and stateless JWT auth |
| Password hashing | `BCryptPasswordEncoder` | Adaptive hashing with salt; never stores plain text |
| Validation | Jakarta Bean Validation | Declarative `@Valid` rules on request DTOs |
| Real-time | `spring-boot-starter-websocket` | Pushes availability changes to connected clients |
| Boilerplate | Lombok | `@Getter/@Setter/@Builder` on entities |
| Build tool | Maven (`mvnw` wrapper) | Reproducible build; wrapper pins the Maven version |
| API testing | curl + Postman + a Node validator | Two independent suites, 50 cases and 59 assertions |
| Front-end | Kotlin + Jetpack Compose, Retrofit, osmdroid | Native Android client; free OpenStreetMap tiles |
| Version control | Git + GitHub | Feature branches with hand-offs between members |

---

## 3. Project Setup

The project was generated with **Spring Initializr** (start.spring.io) and then extended.

| Initializr field | Value |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot version | 4.1.1 |
| Group | `com.example` |
| Artifact | `EV-finder-api` |
| Name | `EV-finder-api` |
| Package name | `com.example.EV_finder_api` |
| Packaging | Jar |
| Java version | 17 |
| Dependencies | Spring Web (WebMVC), Spring Data JPA, MySQL Driver, Spring Security, Validation, WebSocket, Lombok |

### Package structure

```
EV-finder-api/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/main/
    ├── java/com/example/EV_finder_api/
    │   ├── EvFinderApiApplication.java      @SpringBootApplication entry point
    │   ├── config/       SecurityConfig, WebSocketConfig, DemoDataSeeder
    │   ├── controller/   16 REST controllers
    │   ├── dto/          30 request/response records (API boundary)
    │   ├── entity/       12 entities + 10 enums
    │   ├── exception/    7 custom exceptions + GlobalExceptionHandler
    │   ├── repository/   12 Spring Data JPA repositories
    │   ├── security/     JwtService, JwtAuthenticationFilter,
    │   │                 CustomUserDetailsService, CurrentUserProvider
    │   ├── service/      11 service interfaces + service/impl (12 implementations)
    │   └── websocket/    AvailabilityWebSocketHandler
    └── resources/
        └── application.properties
```

The layering is strictly one-directional (`controller → service → repository → entity`); nothing in
`repository` refers upwards, and controllers never touch repositories directly.

### How to run the project

```bash
# 1. Start MySQL from the XAMPP Control Panel. Create the schema once:
#    CREATE DATABASE ev_finder;

# 2. Run the API
cd EV-finder-api
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
# API available on http://localhost:8080

# 3. Confirm it is up (401 means the API is running and asking for a token)
curl -i http://localhost:8080/api/stations
```

**FIGURE 2 — Console showing the application started successfully**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|      (console output showing "Tomcat started on port 8080" and            |
|              "Started EvFinderApiApplication in ... seconds")             |
|                                                                           |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 2: The application starting successfully on the embedded Tomcat server.*

---

## 4. System Architecture

The backend is a **layered modular monolith**. Each layer has one responsibility and talks only to
the layer beneath it. Figure 1 shows the layers, the arrows between them, and the cross-cutting
concerns that wrap every request.

**FIGURE 1 — Layered architecture** *(image provided: `docs/figures/figure1-architecture.svg`)*

```
+---------------------------------------------------------------------------+
|                                                                           |
|     INSERT Figure 1 HERE — docs/figures/figure1-architecture.svg          |
|                                                                           |
|     Open the SVG in a browser and copy it, or convert it to PNG:          |
|     rsvg-convert -o figure1.png docs/figures/figure1-architecture.svg     |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 1: Layered architecture of the EV Finder API.*

### 4.1 Dependency injection

Every layer receives its collaborators through **constructor injection**, and Spring supplies them
from its application context:

```java
@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final StationServiceRepository stationServiceRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AvailabilityWebSocketHandler availabilitySocket;
    private final NotificationService notificationService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              VehicleRepository vehicleRepository,
                              StationServiceRepository stationServiceRepository,
                              CurrentUserProvider currentUserProvider,
                              AvailabilityWebSocketHandler availabilitySocket,
                              NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.stationServiceRepository = stationServiceRepository;
        this.currentUserProvider = currentUserProvider;
        this.availabilitySocket = availabilitySocket;
        this.notificationService = notificationService;
    }
}
```

This is preferable to `new BookingRepository()` inside the class for four concrete reasons:

1. **Testability** — a unit test can pass a mock repository without a database.
2. **Single instance sharing** — Spring reuses one repository proxy (and one connection pool)
   instead of creating a new object per call.
3. **Declarative cross-cutting behaviour** — `@Transactional` works only on Spring-managed beans;
   an object created with `new` would silently lose transaction and security context.
4. **Swappability** — the service depends on the `BookingRepository` interface, not an implementation.

Because a single constructor exists, `@Autowired` is not required; Spring 4.3+ injects it implicitly.

### 4.2 End-to-end request flow

Consider `POST /api/bookings` — a driver reserving a slot:

1. **Filter chain.** `JwtAuthenticationFilter` reads the `Authorization: Bearer …` header, validates
   the signature and expiry with `JwtService`, extracts the user id (subject) and role claim, and
   places an authenticated principal in the `SecurityContext`. A request without a valid token is
   rejected with **401** before it reaches any controller.
2. **Controller.** `BookingController.create(@Valid @RequestBody BookingRequest request)`
   deserialises the JSON into a record. Bean Validation checks the constraints; a violation throws
   `MethodArgumentNotValidException`, which `GlobalExceptionHandler` converts to **400**. The
   controller delegates immediately — it contains no business rules.
3. **Service.** `BookingServiceImpl.create` runs inside one transaction and:
   - loads the caller from `CurrentUserProvider` (from the security context);
   - rejects a window that ends before it starts, or starts in the past (**400**);
   - loads the vehicle and enforces ownership → **403** if it belongs to someone else, **404** if absent;
   - loads the service and checks that both service and station are ACTIVE;
   - **counts overlapping active bookings** and compares against capacity → **409** if full;
   - saves the booking with status `PENDING`, broadcasts the new availability over the WebSocket,
     and notifies the station's operator.
4. **Repository.** Hibernate issues the `SELECT COUNT` and the `INSERT`, using the composite index
   `idx_bookings_conflict`.
5. **Response.** `BookingResponse.from(...)` maps the entity to a DTO — deliberately avoiding the
   lazy `user`/`vehicle` proxies — and the controller returns **201 Created** with the JSON body.

A read such as `GET /api/stations/{id}` follows the same path but is `@Transactional(readOnly = true)`
and returns **200** or **404**.

---

## 5. Implementation

### 5.1 Entity: Booking

`Booking` is the central entity: it links a user, a vehicle, a station and a bookable service to a
time window.

| Field | Java type | Annotations | Description |
|---|---|---|---|
| `id` | `String` | `@Id`, `@Column(name="booking_id", length=36)` | UUID primary key, assigned in `@PrePersist` |
| `user` | `User` | `@ManyToOne(fetch=LAZY, optional=false)`, `@JoinColumn(name="user_id")` | The driver who booked |
| `vehicle` | `Vehicle` | `@ManyToOne(fetch=LAZY, optional=false)` | Which EV the slot is for |
| `station` | `Station` | `@ManyToOne(fetch=LAZY, optional=false)` | Denormalised for fast listing |
| `service` | `StationService` | `@ManyToOne(fetch=LAZY, optional=false)` | The charge point or swap bay |
| `startTime` | `LocalDateTime` | `@Column(name="start_time", nullable=false)` | Slot start |
| `endTime` | `LocalDateTime` | `@Column(name="end_time", nullable=false)` | Slot end |
| `status` | `BookingStatus` | `@Enumerated(STRING)`, `@Builder.Default` | PENDING / CONFIRMED / COMPLETED / CANCELLED |
| `createdAt` | `LocalDateTime` | `@Column(updatable=false)` | Audit timestamp |

```java
@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @Column(name = "booking_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private StationService service;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
```

**In my own words.** `@Entity` tells Hibernate that this class maps to a table, and `@Table` names
that table explicitly. `@Id` marks the primary key; because the API exposes UUIDs rather than
auto-increment numbers, the id is generated in a `@PrePersist` callback instead of by
`@GeneratedValue` — this keeps ids unguessable in URLs and lets the client know the id before the
insert commits. `@ManyToOne` with `fetch = LAZY` stores only a foreign key and defers loading the
related row until it is actually read, which avoids pulling a user, vehicle, station and service
graph on every query; `optional = false` makes the join column `NOT NULL`. `@Enumerated(STRING)`
persists the enum by name (`"PENDING"`) rather than by ordinal, so inserting a new constant later
cannot silently reinterpret existing rows. `@Builder.Default` keeps the default `PENDING` status
when the builder is used. `@PrePersist` guarantees the id and timestamp exist on every insert, so no
caller can forget them.

### 5.2 Repository

```java
public interface BookingRepository extends JpaRepository<Booking, String> {

    /**
     * Double-booking prevention: counts active bookings whose [start,end)
     * window overlaps the requested window. Supported by index
     * idx_bookings_conflict (service_id, status, start_time, end_time).
     */
    @Query("""
           SELECT COUNT(b) FROM Booking b
           WHERE b.service.id = :serviceId
             AND b.status IN ('PENDING', 'CONFIRMED')
             AND b.startTime < :end
             AND b.endTime > :start
           """)
    long countActiveOverlapping(@Param("serviceId") String serviceId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    /** True if any booking (of any status) references this vehicle. */
    boolean existsByVehicleId(String vehicleId);
}
```

Extending `JpaRepository<Booking, String>` provides `save`, `findById`, `findAll`, `deleteById`,
`count`, `existsById` and paging without a single line of implementation — Spring Data generates the
implementation at startup. On top of that I declared **derived query methods** whose names Spring
parses into JPQL, for example `findByUserIdOrderByCreatedAtDesc(String userId)`, and the explicit
`@Query` above for the overlap rule, which is too specific to express as a method name.

The overlap test uses **half-open intervals**: `startTime < :end AND endTime > :start`. This is the
standard way to detect a time clash — it correctly treats 10:00–11:00 and 11:00–12:00 as *not*
overlapping, so back-to-back bookings are allowed, while 10:30 correctly collides with both. Only
`PENDING` and `CONFIRMED` rows count; a `CANCELLED` booking releases its capacity immediately.

### 5.3 Service

The service layer holds the business rules. The creation method is the heart of the system:

```java
@Override
public BookingResponse create(BookingRequest request) {
    User user = currentUserProvider.getCurrentUser();

    if (!request.endTime().isAfter(request.startTime())) {
        throw new ValidationException("End time must be after start time");
    }
    if (request.startTime().isBefore(LocalDateTime.now())) {
        throw new ValidationException("Cannot book a slot in the past");
    }

    Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
    if (!vehicle.getOwner().getId().equals(user.getId())) {
        throw new ForbiddenException("You can only book with your own vehicle");
    }

    StationService service = stationServiceRepository.findById(request.serviceId())
            .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + request.serviceId()));
    Station station = service.getStation();
    if (service.getStatus() != ServiceStatus.ACTIVE || station.getStatus() != StationStatus.ACTIVE) {
        throw new BookingUnavailableException("Service or station is not active");
    }

    long overlapping = bookingRepository.countActiveOverlapping(
            request.serviceId(), request.startTime(), request.endTime());
    if (overlapping >= service.getAvailableSlots()) {
        throw new BookingUnavailableException("No free slots left for the selected time");
    }

    Booking booking = Booking.builder()
            .user(user).vehicle(vehicle).station(station).service(service)
            .startTime(request.startTime()).endTime(request.endTime())
            .status(BookingStatus.PENDING)
            .build();
    booking = bookingRepository.save(booking);
    availabilitySocket.broadcastAvailability(service);   // live update to all clients
    notificationService.notify(station.getOperator().getId(), "New booking received", ...);
    return withAmount(booking, service.getPricePerUnit());
}
```

**Why `findById` is called before saving.** The rule mirrors the classic update case: the entity is
loaded first so the service can (a) prove the resource exists and otherwise return **404**, and
(b) read its current state before deciding. For a booking that means loading the vehicle and the
service, checking ownership and ACTIVE status, and reading `availableSlots` to size the capacity
check. Only then is a new row saved. The same pattern appears in every update method, for example:

```java
public StationResponse update(String stationId, StationRequest request) {
    Station station = getOwnedStation(stationId);   // findById -> 404 if absent, 403 if not owner
    station.setName(request.name());
    station.setAddress(request.address());
    if (request.fuelLevel() != null) {
        station.setFuelLevel(request.fuelLevel());
    }
    return StationResponse.from(stationRepository.save(station));   // update, never insert
}

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
```

Loading the managed entity first is what makes `save` an **UPDATE**. Had the code built a fresh
`Station` from the request and saved it, Hibernate would have treated it as a new row (or
overwritten unrelated columns), and a non-existent id would have silently created data instead of
returning 404.

A third example shows a guard added as a result of testing (see Section 8.4):

```java
public void delete(String vehicleId) {
    Vehicle vehicle = getOwnedVehicle(vehicleId);
    // Bookings reference the vehicle and the FK is ON DELETE RESTRICT, so an
    // unguarded delete surfaced as a 500. Refuse it with a clear 409 instead.
    if (bookingRepository.existsByVehicleId(vehicleId)) {
        throw new ResourceInUseException(
                "This vehicle has booking history and cannot be deleted: " + vehicle.getRegistrationNo());
    }
    vehicleRepository.delete(vehicle);
}
```

**How "not found" is handled.** Services never return `null` or a boolean flag; they throw a domain
exception (`ResourceNotFoundException`, `ForbiddenException`, `ValidationException`,
`BookingUnavailableException`, `ResourceInUseException`). A single `@RestControllerAdvice`
translates each into the right status code, so no controller contains error-handling branches.

### 5.4 Controller

```java
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings(@RequestParam(required = false) BookingStatus status) {
        return status != null ? bookingService.myBookingsByStatus(status) : bookingService.myBookings();
    }

    @GetMapping("/{id}")
    public BookingResponse details(@PathVariable String id) {
        return bookingService.details(id);
    }

    @PutMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable String id) {
        return bookingService.cancel(id);
    }
}
```

And the delete case, which must return an empty body with **204**:

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable String id) {
    vehicleService.delete(id);
    return ResponseEntity.noContent().build();
}
```

**In my own words.** `@RestController` combines `@Controller` with `@ResponseBody`, so every method's
return value is serialised to JSON by Jackson instead of resolving a view. `@RequestMapping` on the
class sets the base path; the method-level `@PostMapping`/`@GetMapping` complete it. `@PathVariable`
binds `{id}` from the URL, `@RequestParam` binds query parameters such as `?status=CONFIRMED`, and
`@RequestBody` deserialises the JSON payload into a record. `@Valid` triggers Bean Validation before
the method body runs.

Status codes are explicit rather than left to defaults, because returning 200 for every response is
misleading:

| Situation | Returned |
|---|---|
| Read succeeded | `200 OK` with the resource |
| Resource created | `201 Created` with the new resource (`ResponseEntity.status(HttpStatus.CREATED)`) |
| Delete succeeded | `204 No Content` with an empty body (`ResponseEntity.noContent().build()`) |
| Validation failed | `400 Bad Request` |
| Missing/invalid token | `401 Unauthorized` |
| Authenticated but not allowed | `403 Forbidden` |
| Resource absent | `404 Not Found` |
| Capacity exhausted, or resource still referenced | `409 Conflict` |

Role restrictions are declared, not coded:

```java
@RestController
@RequestMapping("/api/operator/stations")
@PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
public class OperatorStationController { ... }

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController { ... }
```

### 5.5 Additional layers beyond the basic four

The template's minimum is entity/repository/service/controller. This project adds four layers that
carry real weight, each chosen for a specific reason:

| Layer | Purpose | Example |
|---|---|---|
| **DTO records** | Decouple the API contract from entities. Entities hold lazy associations that would either leak internals or fail to serialise. | `BookingResponse.from(booking, amount, reviewed)` |
| **`GlobalExceptionHandler`** | One place mapping every domain exception to an HTTP status and a uniform body. | `@ExceptionHandler(ResourceNotFoundException.class) → 404` |
| **Security filter chain** | Stateless JWT authentication and method-level authorisation. | `JwtAuthenticationFilter`, `@PreAuthorize` |
| **WebSocket handler** | Push availability changes so clients need not poll. | `AvailabilityWebSocketHandler.broadcastAvailability(service)` |

The uniform error body produced by the handler (a real response captured from the running server):

```json
{
  "timestamp": "2026-09-25T14:10:40.8055529",
  "status": 404,
  "error": "Not Found",
  "message": "Station not found: does-not-exist",
  "path": "/api/stations/does-not-exist"
}
```

---

## 6. REST API Endpoints

The API exposes **54 endpoints across 16 controllers**. Every endpoint outside `/api/auth/**`
requires `Authorization: Bearer <jwt>`. Representative endpoints are listed below; the full list
(including operator, admin, fuel, issue and news endpoints) is in the repository README.

### Core endpoints

| Method | URL | Purpose | Request body | Success | Error |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | Create an account | Register JSON | 201 Created | 400 Bad Request |
| POST | `/api/auth/login` | Authenticate, receive JWT | Login JSON | 200 OK | 401 Unauthorized |
| GET | `/api/users/me` | Current profile | None | 200 OK | 401 Unauthorized |
| PUT | `/api/users/me` | Update profile | Profile JSON | 200 OK | 400 / 401 |
| PUT | `/api/users/me/password` | Change password | Password JSON | 200 OK | 400 / 401 |
| POST | `/api/vehicles` | Add a vehicle | Vehicle JSON | 201 Created | 400 / 409 (duplicate registration) |
| GET | `/api/vehicles/my` | List own vehicles | None | 200 OK | 401 Unauthorized |
| PUT | `/api/vehicles/{id}` | Update a vehicle | Vehicle JSON | 200 OK | 404 Not Found |
| DELETE | `/api/vehicles/{id}` | Delete a vehicle | None | 204 No Content | 404 Not Found, 409 Conflict (has booking history) |
| GET | `/api/stations` | List stations (rating-ranked) | None | 200 OK | 401 Unauthorized |
| GET | `/api/stations/{id}` | One station + services | None | 200 OK | 404 Not Found |
| GET | `/api/stations/nearby` | Stations within radius | Query params | 200 OK | 400 Bad Request |
| GET | `/api/services/{id}/slots` | Slot grid for a date | Query: `date` | 200 OK | 404 Not Found |
| POST | `/api/bookings` | Create a booking | Booking JSON | 201 Created | 400 / 404 / 409 |
| GET | `/api/bookings/my` | Own bookings | Query: `status` | 200 OK | 401 Unauthorized |
| GET | `/api/bookings/{id}` | One booking | None | 200 OK | 403 Forbidden / 404 |
| PUT | `/api/bookings/{id}/cancel` | Cancel a booking | None | 200 OK | 400 / 403 / 404 |
| POST | `/api/payments/{bookingId}` | Pay (simulated) | Payment JSON | 200 OK | 400 (already paid) / 404 |
| POST | `/api/reviews` | Rate a finished session | Review JSON | 201 Created | 400 / 403 / 409 |
| GET | `/api/reviews/station/{id}` | Station reviews + average | None | 200 OK | 404 Not Found |
| GET | `/api/notifications/my` | Notification feed | None | 200 OK | 401 Unauthorized |
| GET | `/api/notifications/unread-count` | Badge count | None | 200 OK | 401 Unauthorized |
| PUT | `/api/notifications/read-all` | Mark all read | None | 200 OK | 401 Unauthorized |
| POST | `/api/issues` | Report a bug/problem | Issue JSON | 201 Created | 400 Bad Request |
| GET | `/api/fuel-stations` | Browse fuel pumps | Query: `q`,`fuel`,`availableOnly` | 200 OK | 401 Unauthorized |
| GET | `/api/news` | Cached energy headlines | Query: `category`,`limit` | 200 OK | 401 Unauthorized |

### Role-restricted endpoints

| Method | URL | Role required |
|---|---|---|
| POST / PUT / DELETE | `/api/operator/stations…` | OPERATOR or ADMIN |
| PUT | `/api/operator/fuel-stations/{id}/fuel/{fuelType}` | OPERATOR or ADMIN |
| GET | `/api/operator/bookings` | OPERATOR or ADMIN |
| GET | `/api/admin/overview` | ADMIN |
| GET / PUT / DELETE | `/api/admin/users…` | ADMIN |
| PUT | `/api/admin/bookings/{id}/cancel` | ADMIN |
| GET / PUT | `/api/admin/issues…` | ADMIN |
| POST | `/api/news/refresh` | ADMIN |

### Sample request: `POST /api/bookings`

```json
{
  "vehicleId": "583dc060-479e-4498-b110-1d6cdee5e6f6",
  "serviceId": "d848e542-1022-4d75-b6a3-7af48640cd71",
  "startTime": "2026-09-28T08:00:00",
  "endTime": "2026-09-28T09:00:00"
}
```

### Sample response (201 Created) — captured from the running server

```json
{
  "id": "eff02477-e45b-43c0-934c-ca561fa8647c",
  "userId": "f3c1f5a1-a288-452d-9000-a8c70947af3e",
  "userName": "Test User",
  "vehicleId": "583dc060-479e-4498-b110-1d6cdee5e6f6",
  "stationId": "be28ac01-970d-44de-a02c-8267a58adca8",
  "stationName": "Uttara UltraCharge",
  "serviceId": "d848e542-1022-4d75-b6a3-7af48640cd71",
  "serviceName": "CHARGING",
  "startTime": "2026-09-28T08:00:00",
  "endTime": "2026-09-28T09:00:00",
  "status": "PENDING",
  "amount": 55.0,
  "createdAt": "2026-09-25T14:10:40.4544377",
  "reviewed": false
}
```

### Sample error response (409 Conflict) — capacity exhausted

```json
{
  "timestamp": "2026-09-25T14:10:48.9070292",
  "status": 409,
  "error": "Conflict",
  "message": "No free slots left for the selected time",
  "path": "/api/bookings"
}
```

### Sample error response (409 Conflict) — vehicle still referenced

```json
{
  "timestamp": "2026-09-25T16:37:30.2977392",
  "status": 409,
  "error": "Conflict",
  "message": "This vehicle has booking history and cannot be deleted: TST-AC-001",
  "path": "/api/vehicles/8314fc91-b5d5-44e8-af29-89ef20f4b4ad"
}
```

### Idempotency

An operation is idempotent when repeating the identical request leaves the server in the same state
as the first call. **PUT** is idempotent because it replaces the state of a known resource: sending
the same body to `PUT /api/vehicles/{id}` ten times leaves one vehicle with those values, and every
call returns 200. **POST** is not idempotent because it asks the server to *create* a subordinate
resource with a server-assigned identity: each `POST /api/bookings` inserts a distinct row with a new
UUID, so ten identical calls attempt ten reservations — and once they exceed the service's capacity
the later ones are correctly rejected with 409 rather than silently merged. This is also why a
payment is modelled as `POST /api/payments/{bookingId}` (a new payment event) while the booking
lookup and cancel are `GET`/`PUT` on an identified booking.

One honest nuance: `PUT /api/bookings/{id}/cancel` is not idempotent in practice. The first call
returns 200 and moves the booking to CANCELLED, while a second call returns **400 Bad Request**
because a cancelled booking is no longer cancellable (tested as TC-49). The state after both calls
is identical, but the response differs, so it is best described as *state-idempotent but not
response-idempotent*.

---

## 7. Database Configuration

### `application.properties`

```properties
spring.application.name=EV-finder-api
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/ev_finder?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=****
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

app.jwt.secret=****
app.jwt.expiration-ms=86400000
```

### What `ddl-auto=update` does

On startup Hibernate compares the `@Entity` classes with the live schema and issues the `ALTER TABLE`
/ `CREATE TABLE` statements needed to reconcile them, leaving existing data in place. It is
convenient during development because adding a field to an entity immediately produces the column.
It is risky in production for three reasons: it can never *drop* or narrow a column, so the database
only ever drifts further from the entities; it makes destructive-looking changes without review; and
schema history is invisible — there is no record of what changed or when. A production system would
use versioned migrations instead.

| Setting | Development choice | Production recommendation |
|---|---|---|
| Database | Local MySQL (XAMPP) | Managed MySQL with backups and a replica |
| `ddl-auto` | `update` | `validate` (fail fast if entities and schema disagree) |
| Schema changes | Automatic from entities | Versioned migrations (Flyway / Liquibase) |
| Credentials | In `application.properties` | Environment variables / secret manager |
| JWT secret | Placeholder in properties | Random 256-bit secret injected at runtime |
| `show-sql` | `true` (learn and debug SQL) | `false` (log noise, possible data leakage) |
| Pooling | HikariCP defaults | Tuned pool size per instance |

### Schema

The full DDL for all 12 tables is committed as `schema.sql`, and the deltas from the original course
diagram are recorded in `schema-changes.md`. Key relationships:

```
users ──1:N── vehicles
  ├──1:N── stations ──1:N── station_services
  │            └──1:N── bookings ──┬──1:1── payments  (UNIQUE booking_id)
  │                                └──1:1── reviews   (UNIQUE booking_id)
  ├──1:N── notifications
  ├──1:N── issues ──N:1── stations (optional)
  └──1:N── fuel_stations ──1:N── fuel_station_inventories  (UNIQUE station+fuel_type)
news_articles  (standalone cache, UNIQUE link)
```

I did not add Spring Profiles or Flyway in this iteration: the project targets a single local
database, so a profile would add indirection without benefit, and `schema.sql` plus
`ddl-auto=update` covers the course requirement. Migrations are listed under future work in
Section 10.

**FIGURE 3 — Database tables in phpMyAdmin**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|     (phpMyAdmin -> database "ev_finder" showing all 12 tables in the      |
|      left sidebar, with the "bookings" table open and rows visible)       |
|                                                                           |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 3: The MySQL schema, showing the tables and saved rows.*

---

## 8. Testing and Results

### 8.1 How the tests were run

Testing is automated rather than manual. Two independent suites exercise the same API, so a mistake
in one harness cannot hide a defect:

| Suite | File | Scope |
|---|---|---|
| Command-line acceptance suite | `docs/run-api-tests.sh` | 50 cases via curl; prints a markdown results table and writes `docs/test-evidence.log` |
| Postman collection (+ validator) | `docs/EV-Finder.postman_collection.json`, `docs/validate_collection.js` | 47 requests in 9 folders carrying 59 assertions |

```bash
# Suite 1 — command line
bash docs/run-api-tests.sh http://localhost:8080
# TOTAL: 50   PASSED: 50   FAILED: 0

# Suite 2 — the Postman collection, run without Postman
node docs/validate_collection.js http://localhost:8080
# assertions: 59   passed: 59   failed: 0
```

Inside Postman the collection is run with the **Collection Runner** (hover the collection → `⋯` →
*Run collection*), keeping the folder order and Iterations = 1. For the report screenshots the
individual requests are sent one at a time, because a screenshot must show the request body as well
as the status code.

`docs/validate_collection.js` was written specifically because a defect in the collection's own
scripts (Section 8.4, finding 5) was invisible to a syntax check: it stubs the Postman `pm` API and
*executes* every script against the live backend, so a runtime error surfaces as a failure instead of
a confusing 404 three requests later.

### 8.2 Results

Both suites finished with no failures.

**Suite 1 — 50 cases, 50 passed, 0 failed**

| TC | Method | Endpoint | Expected | Actual | Pass? |
|---|---|---|---|---|---|
| TC-01 | POST | /api/auth/login (valid credentials) | 200 | 200 | Pass |
| TC-02 | POST | /api/auth/login (wrong password) | 401 | 401 | Pass |
| TC-03 | GET | /api/users/me (no token) | 401 | 401 | Pass |
| TC-04 | GET | /api/users/me (with token) | 200 | 200 | Pass |
| TC-05 | POST | /api/auth/register (invalid payload) | 400 | 400 | Pass |
| TC-06 | POST | /api/vehicles | 201 | 201 | Pass |
| TC-07 | PUT | /api/vehicles/{id} | 200 | 200 | Pass |
| TC-08 | PUT | /api/vehicles/{unknown} | 404 | 404 | Pass |
| TC-09 | DELETE | /api/vehicles/{id} | 204 | 204 | Pass |
| TC-10 | DELETE | /api/vehicles/{unknown} | 404 | 404 | Pass |
| TC-11 | GET | /api/stations | 200 | 200 | Pass |
| TC-12 | GET | /api/stations/{id} | 200 | 200 | Pass |
| TC-13 | GET | /api/stations/{unknown} | 404 | 404 | Pass |
| TC-14 | GET | /api/stations/nearby | 200 | 200 | Pass |
| TC-15 | GET | /api/services/{id}/slots?date=… | 200 | 200 | Pass |
| TC-16 | POST | /api/bookings | 201 | 201 | Pass |
| TC-17 | POST | /api/bookings (end before start) | 400 | 400 | Pass |
| TC-18 | POST | /api/bookings (slot in the past) | 400 | 400 | Pass |
| TC-19 | POST | /api/bookings (unknown service) | 404 | 404 | Pass |
| TC-20 | GET | /api/bookings/my | 200 | 200 | Pass |
| TC-21 | GET | /api/bookings/{id} | 200 | 200 | Pass |
| TC-22 | GET | /api/bookings/{unknown} | 404 | 404 | Pass |
| TC-23 | GET | /api/bookings/{id} as another user | 403 | 403 | Pass |
| TC-24 | POST | /api/bookings (capacity exceeded) | 409 | 409 | Pass |
| TC-25 | POST | /api/payments/{id} (success) | SUCCESS | SUCCESS | Pass |
| TC-26 | POST | /api/payments/{id} (forced failure) | FAILED | FAILED | Pass |
| TC-27 | POST | /api/payments/{unknown} | 404 | 404 | Pass |
| TC-28 | POST | /api/reviews (another user's booking) | 403 | 403 | Pass |
| TC-29 | GET | /api/reviews/station/{id} | 200 | 200 | Pass |
| TC-30 | GET | /api/notifications/my | 200 | 200 | Pass |
| TC-31 | GET | /api/notifications/unread-count | 200 | 200 | Pass |
| TC-32 | PUT | /api/notifications/read-all | 200 | 200 | Pass |
| TC-33 | GET | /api/admin/overview as USER | 403 | 403 | Pass |
| TC-34 | GET | /api/admin/overview as OPERATOR | 403 | 403 | Pass |
| TC-35 | GET | /api/admin/overview as ADMIN | 200 | 200 | Pass |
| TC-36 | GET | /api/operator/bookings as OPERATOR | 200 | 200 | Pass |
| TC-37 | POST | /api/operator/stations as USER | 403 | 403 | Pass |
| TC-38 | PUT | /api/operator/stations/{unknown} | 404 | 404 | Pass |
| TC-39 | POST | /api/issues | 201 | 201 | Pass |
| TC-40 | POST | /api/issues (empty payload) | 400 | 400 | Pass |
| TC-41 | GET | /api/admin/issues as USER | 403 | 403 | Pass |
| TC-42 | GET | /api/admin/issues as ADMIN | 200 | 200 | Pass |
| TC-43 | GET | /api/fuel-stations | 200 | 200 | Pass |
| TC-44 | GET | /api/fuel-stations/{unknown} | 404 | 404 | Pass |
| TC-45 | GET | /api/fuel-stations?fuel=LPG&availableOnly=true | 200 | 200 | Pass |
| TC-46 | PUT | /api/operator/fuel-stations/{id}/fuel/LPG as USER | 403 | 403 | Pass |
| TC-47 | GET | /api/news | 200 | 200 | Pass |
| TC-48 | PUT | /api/bookings/{id}/cancel | 200 | 200 | Pass |
| TC-49 | PUT | /api/bookings/{id}/cancel (second time) | 400 | 400 | Pass |
| TC-50 | DELETE | /api/vehicles/{id} (has booking history) | 409 | 409 | Pass |

**TOTAL: 50 · PASSED: 50 · FAILED: 0**

**Suite 2 — 47 requests in 9 folders, 59 assertions, all passed**

| Folder | Requests | Assertions | Failures |
|---|---|---|---|
| 1. Auth | 5 | 7 | 0 |
| 2. Setup (saves ids) | 3 | 1 | 0 |
| 3. Vehicles (CRUD + 404) | 5 | 5 | 0 |
| 4. Stations & availability | 5 | 7 | 0 |
| 5. Bookings (create / limits / conflict) | 11 | 9 | 0 |
| 6. Payment (simulated) | 5 | 6 | 0 |
| 7. Security (401 / 403) | 5 | 5 | 0 |
| 8. Reviews, notifications, issues, fuel, news | 7 | 7 | 0 |
| 9. Teardown (optional cleanup) | 1 | 2 | 0 |
| **Total** | **47** | **59** | **0** |

**SUMMARY FIGURE — Results diagram** *(image provided: `docs/figures/figure-results.svg`)*

```
+---------------------------------------------------------------------------+
|                                                                           |
|        INSERT THE RESULTS DIAGRAM HERE                                    |
|        docs/figures/figure-results.svg                                    |
|                                                                           |
|   Bar chart of requests per folder, the headline totals (50 cases,        |
|   47 requests, 59 assertions, 0 failures) and the HTTP status codes       |
|   exercised. Convert it to PNG with:                                      |
|   rsvg-convert -o figure-results.png docs/figures/figure-results.svg      |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 13: Test results — requests per folder, totals, and the status codes exercised.*

### 8.3 Screenshots

Each box below is intentionally empty: paste the matching image into it and keep the caption. The
two diagram figures are already produced as SVG files; the rest come from Postman and the running
stack. `docs/HOW-TO-SCREENSHOTS.md` gives the exact steps, and the request names match the Postman
collection.

**FIGURE 4 — TC-01 login returns a JWT (200 OK)**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    Postman -> folder 1. Auth -> "TC-01 Login as USER (200)".              |
|    Show the method + URL, the request body, the 200 status code, and      |
|    the token and role in the response.                                    |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 4: Successful login returning a JWT and the USER role.*

**FIGURE 5 — TC-16 booking created (201 Created)**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    Postman -> folder 5. Bookings -> "TC-16 POST /api/bookings (201)".     |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 5: A booking created with status PENDING.*

**FIGURE 6 — TC-24 capacity conflict (409 Conflict)**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    Postman -> folder 5 -> send the three "Fill capacity" requests, then   |
|    "TC-24 Capacity exceeded (409)". Show the 409 and the message          |
|    "No free slots left for the selected time".                            |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 6: The double-booking rule rejecting a reservation once capacity is reached.*

**FIGURE 7 — TC-25 payment SUCCESS**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    Postman -> folder 6. Payment -> "Setup: create booking to pay", then   |
|    "TC-25 Pay a booking — SUCCESS". Show status SUCCESS and the           |
|    SIM- transaction reference.                                            |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 7: The simulated gateway confirming a payment.*

**FIGURE 8 — TC-26 forced payment failure (FAILED)**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    Postman -> folder 6 -> "Setup: create booking for forced failure",     |
|    then "TC-26 Forced failure — FAILED".                                  |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 8: The failure path of the simulated gateway; the slot is released.*

**FIGURE 9 — TC-13 unknown station (404 Not Found)**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    Postman -> folder 4. Stations & availability -> "TC-13 GET unknown     |
|    station (404)".                                                        |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 9: A request for a resource that does not exist.*

**FIGURE 10 — TC-33 role violation (403 Forbidden)**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    Postman -> folder 7. Security -> "TC-33 USER calling admin endpoint    |
|    (403)".                                                                |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 10: A USER token rejected by an ADMIN-only endpoint.*

**FIGURE 11 — TC-03 missing token (401 Unauthorized)**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    Postman -> folder 7. Security -> "TC-03 GET /api/users/me without      |
|    token (401)".                                                          |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 11: A protected endpoint called without a token.*

**FIGURE 12 — Collection Runner results (47 requests, 0 failures)**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    Postman -> the collection's "..." menu -> "Run collection" -> Run.     |
|    Show the results grid with every request PASS and 0 failures.          |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 12: The whole collection passing in a single run.*

**FIGURE 14 — Android client using the API**

```
+---------------------------------------------------------------------------+
|                                                                           |
|                        PASTE SCREENSHOT HERE                              |
|                                                                           |
|    The app's Home screen with the backend running: station list,          |
|    availability bars and the fuel section.                                |
|                                                                           |
+---------------------------------------------------------------------------+
```

*Figure 14: The native Android client consuming the REST API.*

### 8.4 Findings from testing

Recording what testing actually revealed is more useful than a table of green ticks. Five issues
surfaced; two were defects in the API, three in the test harness.

1. **`POST /api/vehicles` returned 400** during the first run. The cause was the test payload:
   `vehicleType` was `"CAR"`, but the enum is `ELECTRIC_CAR` / `ELECTRIC_BIKE` /
   `ELECTRIC_THREE_WHEELER`. Bean Validation correctly rejected an unknown enum constant — the API
   was right, the test data was wrong.

2. **`PUT /api/vehicles/{unknown}` returned 400 instead of 404** because the test body was also
   invalid. Validation runs before the existence check by design, so a *valid* body is required to
   reach the 404 path. The test was corrected.

3. **State-dependent cases were fragile.** TC-24 only returns 409 when a window is full, and a
   booking can only be paid **once** (a second attempt correctly returns
   `400 "Booking is already paid and confirmed"`). Both suites now create the state each case needs —
   filling capacity before TC-24 and creating dedicated unpaid bookings for the two payment cases —
   so results are deterministic instead of relying on luck. **A test that depends on hidden state is
   a fragile test.**

4. **A real API defect: `DELETE /api/vehicles/{id}` answered 500 instead of 409.** The vehicle
   foreign key in `bookings` is `ON DELETE RESTRICT`, so deleting a vehicle that any booking
   referenced raised a `DataIntegrityViolationException` that `GlobalExceptionHandler` did not
   handle; it escaped as an unhandled 500. It had gone unnoticed because the original test deleted a
   *freshly created* vehicle, which has no booking history — the failing path was never exercised.
   Fixed with a guard in `VehicleServiceImpl.delete` (`existsByVehicleId` → `ResourceInUseException`
   → **409** with an explanatory message, which also preserves booking records rather than orphaning
   them) plus a `DataIntegrityViolationException` handler as a safety net so no foreign-key violation
   anywhere can surface as a 500. TC-50 now covers it. **A green suite can still hide an untested
   path.**

5. **A defect in the test harness that a syntax check could not catch.** One payment case failed with
   404 and "No response". A generator bug produced:

   ```javascript
   pm.collectionVariables.set('payOkBookingId', date + 'T' + pad(hour) + ':00:00');_start
   ```

   The trailing bare `_start` is *syntactically valid* JavaScript, so a syntax check passed it, but it
   throws a `ReferenceError` at runtime. Postman aborts a request whose pre-request script throws —
   hence "No response" — the variable is never set, and the next request posts a garbage id and
   receives 404. Two lessons followed: the collection was rewritten so no request depends on
   asynchronous code or on generated code that has not been executed, and
   `docs/validate_collection.js` was written to *execute* every script, with the generator now
   refusing to write a collection containing invalid JavaScript.

---

## 9. Challenges and Solutions

| Challenge | Cause | How I solved it |
|---|---|---|
| **Saving a booking failed with `Data truncated for column 'type'`, and the whole booking rolled back** | The `notifications.type` column was a MySQL `ENUM` that did not include the newly added `NEW_BOOKING` constant, so inserting the notification threw. Because the notification was written in the *same* transaction as the booking, Hibernate marked the transaction rollback-only and discarded a valid reservation. | Two-part fix. The column was migrated to `VARCHAR(30)` so new types no longer require DDL, and `NotificationService.notify(...)` was rewritten with `@Transactional(propagation = REQUIRES_NEW)` plus a `try/catch` that logs a warning instead of propagating. A notification is a side effect; it must never be able to cancel the business action that triggered it. |
| **Two users could reserve the same slot, overbooking a charge point** | The first implementation only checked that the service existed and was ACTIVE, so capacity was never considered. A single-slot flag would also have failed, because one charge point must serve many non-overlapping bookings per day. | Introduced a capacity model: `station_services.available_slots` is the installed capacity, and a booking is refused when the count of PENDING/CONFIRMED bookings overlapping the requested half-open window reaches that capacity. The count and the insert happen in one `@Transactional` method so two concurrent requests cannot both claim the last slot, and a composite index `idx_bookings_conflict(service_id, status, start_time, end_time)` keeps the count fast. Verified by TC-24 (409). |
| **`LazyInitializationException` when returning a review response** | `spring.jpa.open-in-view=false` closes the Hibernate session when the transaction ends. The controller then tried to serialise a response that still referenced a lazy `user`/`station` proxy, and there was no session left to resolve it. | Enabled lazy loading only where it is legitimate: the review creation path is `@Transactional` so the association loads inside the transaction, and all responses are built by DTO mappers in the service layer, which touch the associations while the session is still open. The API never serialises entities directly. |
| **`DELETE /api/vehicles/{id}` returned 500 instead of 409** | The vehicle foreign key in `bookings` is `ON DELETE RESTRICT`. Deleting a referenced vehicle raised a `DataIntegrityViolationException`, and `GlobalExceptionHandler` had no handler for it, so it escaped as a 500. The original test only deleted a freshly created vehicle, so the broken path was never exercised. | Added a guard in `VehicleServiceImpl.delete` that checks `bookingRepository.existsByVehicleId(...)` first and throws `ResourceInUseException`, mapped to **409 Conflict** with a message explaining the vehicle has booking history — which also preserves booking records. Added a `DataIntegrityViolationException` handler as a safety net so no foreign-key violation anywhere can surface as a 500, and added TC-50 to cover the case. |
| **A new controller returned 404 while the same path without a token returned 401** | The running server was an older process that predated the new controller class. The 401 came from the security filter chain, which authenticates *before* routing, so an unmapped path also answers 401 unauthenticated — which made the endpoint look "present but forbidden" rather than "missing". | Rebuilt and restarted the backend so the new mappings were registered, then re-tested. The lesson: when an authenticated call returns 404 but an unauthenticated call returns 401, check that the running server contains the code before debugging the routing. |
| **Android client could not reach the backend ("request failed")** | The client's base URL is a LAN IP constant; the PC's DHCP address changed (…102 → …103 → …105), so the app was calling an address that no longer existed. | Updated the `HOST_IP` constant and documented in the README that it must match the machine's current IPv4 address; a DHCP reservation or a static address is the durable fix. |
| **A payment test failed with 404 and "No response"** | A generator bug produced `...set('x', ...);_start`, which is valid syntax but throws at runtime. Postman aborts a request whose pre-request script throws, so the id was never set. | Fixed the generator, made the collection depend on no asynchronous code, added guards that reject a value which is not a booking id, and wrote `docs/validate_collection.js` to execute every script so this class of bug fails in the harness rather than in the Runner. |

---

## 10. Conclusion and Future Work

### Conclusion

The project set out to build a backend for finding and reserving EV charging and battery-swap slots,
and it delivers that end to end. Each objective from Section 1.2 was met:

| Objective | Outcome |
|---|---|
| JWT auth for three roles with BCrypt hashing | Implemented; `BCryptPasswordEncoder` stores only hashes; roles drive separate client shells. Verified by TC-01…TC-04 |
| Model stations, services and vehicles with capacity | Implemented across 12 tables; capacity lives on `station_services`, vehicles are owner-scoped. TC-06…TC-15 |
| Prevent double-booking with 409 | Implemented with the overlap-count rule, verified by TC-24 |
| Full booking lifecycle with simulated payment | PENDING → CONFIRMED/CANCELLED with SUCCESS, FAILED and REFUNDED payment states. TC-16, TC-25, TC-26, TC-48 |
| Documented REST API with correct status codes | 54 endpoints over 16 controllers; 200/201/204/400/401/403/404/409 all exercised. TC-01…TC-50 |
| Verify with automated suites | 50 curl cases and 47 Postman requests / 59 assertions, all passing, with scripts and raw evidence committed |

Beyond the original plan the project grew features that make it a usable product rather than a CRUD
demo: real-time availability over WebSocket, a notification system with an animated badge, a
fuel-station module with inventory, user issue reporting with an admin reply loop, and a cached
energy-news feed.

The most valuable outcomes were the lessons testing forced. A business rule belongs in the service
layer inside a transaction boundary, while a side effect such as a notification must be isolated so
it cannot roll back the operation it describes. A green test suite proves only the paths it
exercises — the 500 on vehicle deletion survived a passing suite because the test always deleted a
vehicle with no history. And a test harness is production code: it earned the same review, which is
why the collection is now validated by a script that executes it rather than one that merely reads it.

### Future work

1. **Scheduled booking completion.** A `@Scheduled` job should move CONFIRMED bookings to COMPLETED
   once their window ends (and expire no-shows). Today nothing performs that transition, which is why
   the client treats a confirmed booking whose slot has elapsed as reviewable.
2. **Automated tests in the build.** The two suites are external scripts; converting the
   highest-value cases into JUnit + MockMvc integration tests would run them on every commit, with
   Testcontainers providing a disposable MySQL.
3. **Versioned migrations.** Replace `ddl-auto=update` with Flyway migrations and set
   `ddl-auto=validate`, so schema history is explicit and production deployment is safe.
4. **Pagination and filtering.** `GET /api/stations` and `/api/admin/bookings` return complete lists;
   `Pageable` (or a keyset cursor) would bound the payload as data grows.
5. **Soft-delete or `ON DELETE SET NULL` for vehicles.** Refusing to delete a vehicle with history
   preserves the audit trail but leaves the user stuck; denormalising the vehicle details onto the
   booking, or a soft-delete flag, would allow both.
6. **Push notifications.** In-app notifications only reach a user who opens the app; Firebase Cloud
   Messaging would deliver booking and payment events to the device.
7. **Consolidate the two fuel sources.** The public OpenStreetMap fuel/LPG layer and the
   operator-managed fuel-station module are separate; one canonical module would remove the overlap.
8. **Secrets and storage hardening.** Move the JWT secret to an environment variable, restrict the
   WebSocket allowed origins, and add refresh tokens so a 24-hour token is not the only session
   mechanism.

---

## References

[1] Spring, "Spring Boot Reference Documentation." [Online]. Available:
https://docs.spring.io/spring-boot/ [Accessed: 25-Sep-2026].

[2] Spring, "Spring Data JPA Reference Documentation." [Online]. Available:
https://docs.spring.io/spring-data/jpa/reference/ [Accessed: 25-Sep-2026].

[3] Spring, "Spring Security Reference — Method Security." [Online]. Available:
https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html
[Accessed: 25-Sep-2026].

[4] M. Jones, J. Bradley, and N. Sakimura, "JSON Web Token (JWT)," RFC 7519, Internet Engineering
Task Force, May 2015. [Online]. Available: https://www.rfc-editor.org/rfc/rfc7519

[5] Oracle, "Java Platform, Standard Edition 17 API Specification." [Online]. Available:
https://docs.oracle.com/en/java/javase/17/docs/api/ [Accessed: 25-Sep-2026].

[6] Oracle, "MySQL 8.0 Reference Manual." [Online]. Available:
https://dev.mysql.com/doc/refman/8.0/en/ [Accessed: 25-Sep-2026].

[7] Hibernate, "Hibernate ORM User Guide." [Online]. Available:
https://hibernate.org/orm/documentation/ [Accessed: 25-Sep-2026].

[8] Project Lombok, "Lombok Features." [Online]. Available:
https://projectlombok.org/features/ [Accessed: 25-Sep-2026].

[9] jjwt, "Java JWT — JSON Web Token for Java and Android." [Online]. Available:
https://github.com/jwtk/jjwt [Accessed: 25-Sep-2026].

[10] Google, "Jetpack Compose — Android Developers." [Online]. Available:
https://developer.android.com/develop/ui/compose [Accessed: 25-Sep-2026].

[11] Square, "Retrofit — A type-safe HTTP client for Android and Java." [Online]. Available:
https://square.github.io/retrofit/ [Accessed: 25-Sep-2026].

[12] Postman, "Postman Learning Center — Collection Runner." [Online]. Available:
https://learning.postman.com/docs/collections/running-collections/intro-to-collection-runs/
[Accessed: 25-Sep-2026].

[13] OpenStreetMap contributors, "OpenStreetMap." [Online]. Available:
https://www.openstreetmap.org/ [Accessed: 25-Sep-2026].

[14] osmdroid, "osmdroid — OpenStreetMap Android library." [Online]. Available:
https://github.com/osmdroid/osmdroid [Accessed: 25-Sep-2026].

[15] Overpass API, "Overpass API — OpenStreetMap Wiki." [Online]. Available:
https://wiki.openstreetmap.org/wiki/Overpass_API [Accessed: 25-Sep-2026].

---

## Appendix A: Submission Checklist

| Done | Item |
|---|---|
| ⬜ | Cover page filled in completely *(Student ID, Section, Submitted To, Date, GitHub link)* |
| ⬜ | Every **FIGURE** box has an image pasted into it, and every `[FILL IN]` marker is gone |
| ✅ | Project runs with `mvnw spring-boot:run` without errors |
| ✅ | Layered structure: controller, service, repository, entity packages |
| ✅ | All CRUD endpoints implemented and documented in Section 6 |
| ✅ | Correct status codes: 200, 201, 204, 400, 401, 403, 404 and 409 |
| ✅ | Update logic checks existence with `findById` before saving |
| ✅ | Two independent test suites, both passing, with raw evidence committed |
| ✅ | No real passwords visible in code or screenshots *(`password=****`, JWT secret masked)* |
| ✅ | References listed; all writing in your own words |
| ⬜ | Report exported to PDF and named `StudentID_Name_SpringBootProject.pdf` |

---

## Appendix B: Sample Marking Rubric

Indicative only; the instructor's official rubric takes priority.

| Criterion | What is assessed | Marks |
|---|---|---|
| Report structure and writing | Clear sections, own words, correct formatting | 10 |
| Architecture and design | Correct layering, dependency injection explained | 15 |
| Implementation | Entity, repository, service, controller quality and explanation | 25 |
| REST API design | Correct methods, URLs and status codes | 15 |
| Database configuration | Properties, ddl-auto, profiles / migrations | 10 |
| Testing and results | Success and failure cases, clear screenshots | 15 |
| Reflection | Challenges, conclusion, future work | 10 |
| **Total** | | **100** |
