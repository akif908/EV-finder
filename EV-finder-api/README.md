# EV-finder-api — Spring Boot Backend

Backend for the **EV Charging & Battery-Swap Finder and Booking System** (AOOP course project).

- Java 17, Spring Boot 4.1.1, MySQL (XAMPP), Spring Security + JWT, WebSocket
- Client: ONE native Kotlin Android app (role-based screens for USER / OPERATOR / ADMIN)

## Run

1. Start MySQL from XAMPP.
2. Create the schema: `mysql -u root -p < "D:/EV finder/schema.sql"`
   (or just start the app — `createDatabaseIfNotExist=true` is set, and Hibernate `ddl-auto=update` builds the tables).
3. `./mvnw spring-boot:run` → http://localhost:8080

## Package layout

```
com.example.EV_finder_api
├── config        # SecurityFilterChain, WebSocket config, CORS, @Async
├── controller    # thin REST controllers (/api/auth, /api/stations, ...)
├── service       # business-rule interfaces
│   └── impl      # implementations (booking conflict checks live here)
├── repository    # Spring Data JPA repositories
├── entity        # JPA entities + enums (Role, BookingStatus, ServiceType, ...)
├── dto           # request/response records (LoginRequest, BookingResponse, ...)
├── mapper        # entity <-> DTO mapping
├── security      # JWT filter, token provider, UserDetails service
├── websocket     # STOMP endpoints for live availability updates
├── exception     # custom exceptions + @RestControllerAdvice handler
└── util          # helpers
```

## Build order (context §29)

1. ~~Project setup~~ (this scaffold + schema) 2. Authentication (JWT, roles) 3. Core data
(users/vehicles/stations/services) 4. Booking + double-booking prevention 5. Simulated payment
6. Android app core 7. Map (osmdroid + OpenStreetMap) 8. Operator features 9. Admin
10. WebSocket 11. Finalization.

No AI/ML. Payment is simulated. The backend is the source of truth for availability and
authorization — never trust frontend validation alone.
