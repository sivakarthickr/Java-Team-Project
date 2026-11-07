-- =========================================================================================
-- AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
-- Database Schema & Analytical Queries: av_fleet_schema.sql
-- Compatible with: MySQL 8.0+ / MySQL Workbench / OCI Linux VM MySQL Deployment
-- =========================================================================================

-- Create database if it does not exist (safely preserve existing databases)
CREATE DATABASE IF NOT EXISTS av_fleet_db;
USE av_fleet_db;

-- =========================================================================================
-- TABLE 1: vehicles
-- Master entity table storing active vehicle inventory, sensor health, and safety ratings.
-- =========================================================================================
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id INT AUTO_INCREMENT PRIMARY KEY,
    vin VARCHAR(17) NOT NULL UNIQUE,
    vehicle_type ENUM('URBAN_ROBOTAXI', 'HIGHWAY_FREIGHT_TRUCK', 'LAST_MILE_DELIVERY_ROVER') NOT NULL,
    lidar_status VARCHAR(50) DEFAULT 'CALIBRATED',
    radar_status VARCHAR(50) DEFAULT 'CALIBRATED',
    camera_status VARCHAR(50) DEFAULT 'CALIBRATED',
    battery_level DECIMAL(5, 2) NOT NULL DEFAULT 100.00,
    safety_score DECIMAL(5, 2) DEFAULT NULL,
    safety_grade ENUM('PLATINUM_SAFETY', 'GOLD_FLEET', 'RECALIBRATION_REQUIRED', 'GROUNDED') DEFAULT NULL,
    fleet_status ENUM('ACTIVE', 'SAFETY_QUARANTINE', 'MAINTENANCE', 'RETIRED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints & Indexes
    CONSTRAINT chk_battery_level CHECK (battery_level >= 0.00 AND battery_level <= 100.00),
    CONSTRAINT chk_safety_score CHECK (safety_score IS NULL OR (safety_score >= 0.00 AND safety_score <= 100.00)),
    INDEX idx_vehicles_vin (vin),
    INDEX idx_vehicles_type (vehicle_type),
    INDEX idx_vehicles_fleet_status (fleet_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================================================
-- TABLE 2: trips
-- Journey metrics, trip distances, safety infractions, and disengagement rates.
-- =========================================================================================
CREATE TABLE IF NOT EXISTS trips (
    trip_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    trip_distance DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    speeding_violations INT NOT NULL DEFAULT 0,
    arrival_condition DECIMAL(5, 2) NOT NULL DEFAULT 100.00,
    disengagements INT NOT NULL DEFAULT 0,
    disengagement_rate DECIMAL(6, 2) NOT NULL DEFAULT 0.00,
    trip_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints, Foreign Keys & Indexes
    CONSTRAINT chk_trip_distance CHECK (trip_distance >= 0.00),
    CONSTRAINT chk_speeding_violations CHECK (speeding_violations >= 0),
    CONSTRAINT chk_arrival_condition CHECK (arrival_condition >= 0.00 AND arrival_condition <= 100.00),
    CONSTRAINT chk_disengagements CHECK (disengagements >= 0),
    CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id) 
        REFERENCES vehicles(vehicle_id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    INDEX idx_trips_vehicle_id (vehicle_id),
    INDEX idx_trips_date (trip_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================================================
-- TABLE 3: telemetry_logs
-- High-frequency sensor fidelity telemetry and intervention events per vehicle.
-- =========================================================================================
CREATE TABLE IF NOT EXISTS telemetry_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    lidar_confidence DECIMAL(5, 4) NOT NULL DEFAULT 1.0000,
    manual_interventions INT NOT NULL DEFAULT 0,
    hard_braking_events INT NOT NULL DEFAULT 0,
    log_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints, Foreign Keys & Indexes
    CONSTRAINT chk_lidar_confidence CHECK (lidar_confidence >= 0.0000 AND lidar_confidence <= 1.0000),
    CONSTRAINT chk_manual_interventions CHECK (manual_interventions >= 0),
    CONSTRAINT chk_hard_braking_events CHECK (hard_braking_events >= 0),
    CONSTRAINT fk_telemetry_vehicle FOREIGN KEY (vehicle_id) 
        REFERENCES vehicles(vehicle_id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    INDEX idx_telemetry_vehicle_id (vehicle_id),
    INDEX idx_telemetry_timestamp (log_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================================================
-- TABLE 4: quarantine_records
-- Audit enforcement logs for vehicles barred from autonomous dispatch.
-- =========================================================================================
CREATE TABLE IF NOT EXISTS quarantine_records (
    quarantine_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    quarantine_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ACTIVE_QUARANTINE', 'CLEARED_AFTER_MAINTENANCE') DEFAULT 'ACTIVE_QUARANTINE',

    -- Foreign Keys & Indexes
    CONSTRAINT fk_quarantine_vehicle FOREIGN KEY (vehicle_id) 
        REFERENCES vehicles(vehicle_id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    INDEX idx_quarantine_vehicle_id (vehicle_id),
    INDEX idx_quarantine_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================================================
-- TABLE 5: pre_trip_inspections
-- Verification records for Phase 1 Pre-Trip Sensor Calibration & Battery clearance.
-- =========================================================================================
CREATE TABLE IF NOT EXISTS pre_trip_inspections (
    inspection_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    vin VARCHAR(17) NOT NULL,
    lidar_status VARCHAR(50) NOT NULL,
    radar_status VARCHAR(50) NOT NULL,
    camera_status VARCHAR(50) NOT NULL,
    battery_level DECIMAL(5, 2) NOT NULL,
    inspection_status ENUM('PASSED', 'FAILED') NOT NULL,
    failure_reason VARCHAR(255) DEFAULT NULL,
    inspection_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inspection_vehicle FOREIGN KEY (vehicle_id) 
        REFERENCES vehicles(vehicle_id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    INDEX idx_inspections_vehicle_id (vehicle_id),
    INDEX idx_inspections_vin (vin),
    INDEX idx_inspections_status (inspection_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================================================
-- SAMPLE SEED DATA (For Immediate Verification & Testing)
-- =========================================================================================
INSERT INTO vehicles (vin, vehicle_type, lidar_status, radar_status, camera_status, battery_level, safety_score, safety_grade, fleet_status)
VALUES 
    ('1HGCR2F83HA001234', 'URBAN_ROBOTAXI', 'CALIBRATED', 'CALIBRATED', 'CALIBRATED', 94.50, 96.20, 'PLATINUM_SAFETY', 'ACTIVE'),
    ('1FTFW1ED4MFA56789', 'HIGHWAY_FREIGHT_TRUCK', 'CALIBRATED', 'CALIBRATED', 'CALIBRATED', 88.00, 84.50, 'GOLD_FLEET', 'ACTIVE'),
    ('2C3CDZAG1KH112233', 'LAST_MILE_DELIVERY_ROVER', 'CALIBRATED', 'CALIBRATED', 'CALIBRATED', 76.00, 71.00, 'RECALIBRATION_REQUIRED', 'ACTIVE'),
    ('1G1ZD5ST8HF445566', 'URBAN_ROBOTAXI', 'DEGRADED', 'CALIBRATED', 'BLURRED', 82.00, 48.00, 'GROUNDED', 'SAFETY_QUARANTINE'),
    ('3VW2B7AJ9HM778899', 'HIGHWAY_FREIGHT_TRUCK', 'DEGRADED', 'DEGRADED', 'FAULT', 65.00, 42.00, 'GROUNDED', 'SAFETY_QUARANTINE')
ON DUPLICATE KEY UPDATE vin = vin;

INSERT INTO trips (vehicle_id, trip_distance, speeding_violations, arrival_condition, disengagements, disengagement_rate)
VALUES
    (1, 150.00, 0, 98.00, 1, 0.67),
    (2, 320.00, 0, 95.50, 1, 0.31),
    (3, 45.00, 0, 92.00, 1, 2.22),
    (4, 80.00, 2, 65.00, 4, 5.00),
    (5, 200.00, 1, 58.00, 5, 2.50)
ON DUPLICATE KEY UPDATE vehicle_id = vehicle_id;

INSERT INTO telemetry_logs (vehicle_id, lidar_confidence, manual_interventions, hard_braking_events)
VALUES
    (1, 0.9650, 1, 2),
    (2, 0.9420, 1, 1),
    (3, 0.9100, 1, 3),
    (4, 0.7200, 4, 8),
    (5, 0.6800, 5, 6)
ON DUPLICATE KEY UPDATE vehicle_id = vehicle_id;

INSERT INTO quarantine_records (vehicle_id, reason, status)
VALUES
    (4, 'Exceeded Urban Robotaxi Disengagement Threshold (> 2.0 per 100km)', 'ACTIVE_QUARANTINE'),
    (5, 'Exceeded Highway Freight Truck Disengagement Threshold (> 1.0 per 100km)', 'ACTIVE_QUARANTINE')
ON DUPLICATE KEY UPDATE vehicle_id = vehicle_id;

-- =========================================================================================
-- REQUIRED ANALYTICAL & AUDIT SQL QUERIES
-- =========================================================================================

-- 1. Display all vehicles
SELECT 
    vehicle_id, 
    vin, 
    vehicle_type, 
    battery_level, 
    safety_score, 
    safety_grade, 
    fleet_status, 
    created_at 
FROM vehicles
ORDER BY vehicle_id ASC;

-- 2. Display all trips
SELECT 
    t.trip_id, 
    v.vin, 
    v.vehicle_type, 
    t.trip_distance, 
    t.speeding_violations, 
    t.arrival_condition, 
    t.disengagements, 
    t.disengagement_rate, 
    t.trip_date 
FROM trips t
JOIN vehicles v ON t.vehicle_id = v.vehicle_id
ORDER BY t.trip_id ASC;

-- 3. Display telemetry information
SELECT 
    tl.log_id, 
    v.vin, 
    v.vehicle_type, 
    tl.lidar_confidence, 
    tl.manual_interventions, 
    tl.hard_braking_events, 
    tl.log_timestamp 
FROM telemetry_logs tl
JOIN vehicles v ON tl.vehicle_id = v.vehicle_id
ORDER BY tl.log_timestamp DESC;

-- 4. Display quarantined vehicles
SELECT 
    qr.quarantine_id, 
    v.vin, 
    v.vehicle_type, 
    qr.reason, 
    qr.quarantine_date, 
    qr.status AS quarantine_status,
    v.fleet_status
FROM quarantine_records qr
JOIN vehicles v ON qr.vehicle_id = v.vehicle_id
WHERE qr.status = 'ACTIVE_QUARANTINE';

-- 5. Count vehicles by category
SELECT 
    vehicle_type, 
    COUNT(*) AS total_vehicles 
FROM vehicles 
GROUP BY vehicle_type
ORDER BY total_vehicles DESC;

-- 6. Calculate average safety score
SELECT 
    ROUND(AVG(safety_score), 2) AS average_fleet_safety_score,
    ROUND(MIN(safety_score), 2) AS lowest_safety_score,
    ROUND(MAX(safety_score), 2) AS highest_safety_score
FROM vehicles
WHERE safety_score IS NOT NULL;
