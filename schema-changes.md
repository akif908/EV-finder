# Schema Changes vs `final schema-aoop.png`

Keep this file so the dbdiagram.io source (and the exported PNG) can be updated to match.

| # | Table | Change | Why |
|---|---|---|---|
| 1 | `users` | **Add `password_hash VARCHAR(255) NOT NULL`** | Auth is impossible without it; BCrypt hash from Spring Security (context §24) |
| 2 | *(new table)* | **Add `notifications`** (id, user_id FK, title, message, type enum, is_read, created_at) | Core module #12 in the project context; was missing from both schema and architecture diagram |
| 3 | `stations` | Add `address`, `description`, `opening_time`, `closing_time`, `created_at`, `updated_at` | Context §10 station fields; opening hours bound the bookable day |
| 4 | `stations` | `latitude`/`longitude`: `Float` → `DECIMAL(10,7)` / `DECIMAL(11,7)` | FLOAT loses coordinate precision; breaks nearby-search accuracy |
| 5 | `vehicles` | Add `manufacturer`, `model`, `battery_capacity_kwh`, `created_at` | Context §12 vehicle fields |
| 6 | `station_services` | Add `status` enum (ACTIVE/INACTIVE) and optional `power_kw` | Context §11: operator must be able to deactivate a single service |
| 7 | `bookings` | Add `created_at`; add index `(service_id, status, start_time, end_time)` | The overlap-conflict query that prevents double booking needs this index; context §14 lists created timestamp |
| 8 | `payments` | Add `transaction_ref`, `created_at` | Proper backend payment record for the simulated gateway (context §15) |
| 9 | `reviews` | Add `created_at`; `CHECK (rating BETWEEN 1 AND 5)` | Context §19: backend validates ratings |
| 10 | all FKs | Explicit `ON DELETE` behavior + FK indexes | Referential integrity and query performance (context §21) |

Unchanged (already correct in the PNG): 7-table entity set; `booking → payment` 1:1 via unique FK; `reviews.booking_id` unique (one review per booking); role/service-type/status enums; `registration_no` and `email` unique; string UUID-style PKs (keep consistent — every FK referencing them is also VARCHAR(36)).

dbdiagram.io source (paste to regenerate the PNG):

```dbml
Table users {
  user_id varchar [pk]
  name varchar
  email varchar [unique]
  password_hash varchar
  role enum
  created_at timestamp
}
Table vehicles {
  vehicle_id varchar [pk]
  user_id varchar [ref: > users.user_id]
  manufacturer varchar
  model varchar
  vehicle_type enum
  connector_type varchar
  battery_capacity_kwh decimal
  registration_no varchar [unique]
  created_at timestamp
}
Table stations {
  station_id varchar [pk]
  operator_id varchar [ref: > users.user_id]
  name varchar
  description varchar
  address varchar
  latitude decimal
  longitude decimal
  opening_time time
  closing_time time
  status enum
  created_at timestamp
  updated_at timestamp
}
Table station_services {
  service_id varchar [pk]
  station_id varchar [ref: > stations.station_id]
  service_type enum
  connector_type varchar
  power_kw decimal
  price_per_unit decimal
  available_slots int
  status enum
}
Table bookings {
  booking_id varchar [pk]
  user_id varchar [ref: > users.user_id]
  vehicle_id varchar [ref: > vehicles.vehicle_id]
  station_id varchar [ref: > stations.station_id]
  service_id varchar [ref: > station_services.service_id]
  start_time timestamp
  end_time timestamp
  status enum
  created_at timestamp
}
Table payments {
  payment_id varchar [pk]
  booking_id varchar [unique, ref: - bookings.booking_id]
  amount decimal
  payment_method enum
  status enum
  transaction_ref varchar
  created_at timestamp
}
Table reviews {
  review_id varchar [pk]
  booking_id varchar [unique, ref: - bookings.booking_id]
  user_id varchar [ref: > users.user_id]
  station_id varchar [ref: > stations.station_id]
  rating int
  comment varchar
  created_at timestamp
}
Table notifications {
  notification_id varchar [pk]
  user_id varchar [ref: > users.user_id]
  title varchar
  message varchar
  type enum
  is_read boolean
  created_at timestamp
}
```
