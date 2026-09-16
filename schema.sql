-- ============================================================
-- EV Charging & Battery-Swap Finder and Booking System
-- MySQL schema (XAMPP / MySQL 8.x)
-- Matches: users, vehicles, stations, station_services,
--          bookings, payments, reviews, notifications
-- Run: mysql -u root -p < schema.sql   (or import via phpMyAdmin)
-- ============================================================

CREATE DATABASE IF NOT EXISTS ev_finder
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ev_finder;

-- ------------------------------------------------------------
-- USERS  (USER / OPERATOR / ADMIN all live here, role enum)
-- ------------------------------------------------------------
CREATE TABLE users (
  user_id       VARCHAR(36)  NOT NULL,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(150) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,          -- BCrypt hash, never plain text
  role          ENUM('USER','OPERATOR','ADMIN') NOT NULL DEFAULT 'USER',
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- VEHICLES  (USER 1 -> N VEHICLES)
-- ------------------------------------------------------------
CREATE TABLE vehicles (
  vehicle_id       VARCHAR(36) NOT NULL,
  user_id          VARCHAR(36) NOT NULL,
  manufacturer     VARCHAR(60) NULL,             -- e.g. Tesla, Nissan
  model            VARCHAR(60) NULL,             -- e.g. Model 3, Leaf
  vehicle_type     ENUM('ELECTRIC_CAR','ELECTRIC_BIKE','ELECTRIC_THREE_WHEELER') NOT NULL,
  connector_type   VARCHAR(40) NULL,             -- e.g. CCS2, Type2, GB/T
  battery_capacity_kwh DECIMAL(5,2) NULL,
  registration_no  VARCHAR(30) NOT NULL,
  created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (vehicle_id),
  UNIQUE KEY uq_vehicles_registration (registration_no),
  KEY idx_vehicles_user (user_id),
  CONSTRAINT fk_vehicles_user FOREIGN KEY (user_id)
    REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- STATIONS  (OPERATOR 1 -> N STATIONS, ownership via operator_id)
-- ------------------------------------------------------------
CREATE TABLE stations (
  station_id   VARCHAR(36)  NOT NULL,
  operator_id  VARCHAR(36)  NOT NULL,
  name         VARCHAR(120) NOT NULL,
  description  VARCHAR(500) NULL,
  address      VARCHAR(255) NULL,
  latitude     DECIMAL(10,7) NOT NULL,          -- DECIMAL, not FLOAT: no precision loss
  longitude    DECIMAL(11,7) NOT NULL,
  opening_time TIME NULL,                       -- availability bounded by these
  closing_time TIME NULL,
  status       ENUM('ACTIVE','INACTIVE','TEMPORARILY_UNAVAILABLE') NOT NULL DEFAULT 'ACTIVE',
  fuel_level   INT NULL,                        -- remaining energy reserve, 0-100 (%)
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (station_id),
  KEY idx_stations_operator (operator_id),
  KEY idx_stations_status (status),
  CONSTRAINT fk_stations_operator FOREIGN KEY (operator_id)
    REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- STATION_SERVICES  (STATION 1 -> N SERVICES)
-- Availability model = "Option B": service capacity + booking records
-- ------------------------------------------------------------
CREATE TABLE station_services (
  service_id      VARCHAR(36) NOT NULL,
  station_id      VARCHAR(36) NOT NULL,
  service_type    ENUM('CHARGING','BATTERY_SWAP') NOT NULL,
  connector_type  VARCHAR(40) NULL,              -- applicable for CHARGING
  power_kw        DECIMAL(6,2) NULL,             -- optional capacity info
  price_per_unit  DECIMAL(10,2) NOT NULL,
  available_slots INT NOT NULL DEFAULT 1,        -- concurrent capacity
  status          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (service_id),
  KEY idx_services_station (station_id),
  CONSTRAINT fk_services_station FOREIGN KEY (station_id)
    REFERENCES stations(station_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- BOOKINGS
-- Index supports the overlap-conflict query used to prevent
-- double booking: WHERE service_id = ? AND status IN
--   ('PENDING','CONFIRMED') AND start_time < ? AND end_time > ?
-- ------------------------------------------------------------
CREATE TABLE bookings (
  booking_id  VARCHAR(36) NOT NULL,
  user_id     VARCHAR(36) NOT NULL,
  vehicle_id  VARCHAR(36) NOT NULL,
  station_id  VARCHAR(36) NOT NULL,
  service_id  VARCHAR(36) NOT NULL,
  start_time  TIMESTAMP NOT NULL,
  end_time    TIMESTAMP NOT NULL,
  status      ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (booking_id),
  KEY idx_bookings_user (user_id),
  KEY idx_bookings_vehicle (vehicle_id),
  KEY idx_bookings_station (station_id),
  KEY idx_bookings_conflict (service_id, status, start_time, end_time),
  CONSTRAINT fk_bookings_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)           ON DELETE CASCADE,
  CONSTRAINT fk_bookings_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)     ON DELETE RESTRICT,
  CONSTRAINT fk_bookings_station FOREIGN KEY (station_id) REFERENCES stations(station_id)     ON DELETE RESTRICT,
  CONSTRAINT fk_bookings_service FOREIGN KEY (service_id) REFERENCES station_services(service_id) ON DELETE RESTRICT,
  CONSTRAINT chk_booking_times CHECK (end_time > start_time)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- PAYMENTS  (BOOKING 1 -> 1 PAYMENT, enforced by UNIQUE booking_id)
-- ------------------------------------------------------------
CREATE TABLE payments (
  payment_id      VARCHAR(36) NOT NULL,
  booking_id      VARCHAR(36) NOT NULL,
  amount          DECIMAL(10,2) NOT NULL,
  payment_method  ENUM('MOBILE_BANKING','CARD','CASH_AT_STATION') NOT NULL,
  status          ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
  transaction_ref VARCHAR(60) NULL,              -- simulated gateway reference
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (payment_id),
  UNIQUE KEY uq_payments_booking (booking_id),
  CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id)
    REFERENCES bookings(booking_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- REVIEWS  (one review per booking, rating validated 1..5)
-- ------------------------------------------------------------
CREATE TABLE reviews (
  review_id  VARCHAR(36) NOT NULL,
  booking_id VARCHAR(36) NOT NULL,
  user_id    VARCHAR(36) NOT NULL,
  station_id VARCHAR(36) NOT NULL,
  rating     INT NOT NULL,
  comment    VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (review_id),
  UNIQUE KEY uq_reviews_booking (booking_id),     -- no duplicate review per booking
  KEY idx_reviews_user (user_id),
  KEY idx_reviews_station (station_id),
  CONSTRAINT fk_reviews_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)       ON DELETE CASCADE,
  CONSTRAINT fk_reviews_station FOREIGN KEY (station_id) REFERENCES stations(station_id) ON DELETE CASCADE,
  CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
  CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- NOTIFICATIONS  (in-app notifications, context §18)
-- ------------------------------------------------------------
CREATE TABLE notifications (
  notification_id VARCHAR(36) NOT NULL,
  user_id         VARCHAR(36) NOT NULL,
  title           VARCHAR(150) NOT NULL,
  message         VARCHAR(500) NOT NULL,
  type            ENUM('BOOKING_CONFIRMED','BOOKING_CANCELLED','BOOKING_REMINDER','AVAILABILITY','GENERAL') NOT NULL DEFAULT 'GENERAL',
  is_read         BOOLEAN NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notification_id),
  KEY idx_notifications_user (user_id, is_read),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
    REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;
