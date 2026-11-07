# Comprehensive Testing Report & Verification Checklist
## Autonomous Vehicle (AV) Fleet Safety & Telemetry Audit Engine

> **Project Set 21: Full-Cycle Academic Quality Assurance Report**  
> **Test Scope:** Verification of all 12 Project Development Tasks from Local Development to Distributed Cloud Deployment.

---

## 1. Master Testing Checklist (All 12 Project Development Parts)

| Part # | Functional Module | Verification Command / Target | Pass Criteria | Status |
|:---:|---|---|---|:---:|
| **Part 1** | **MySQL Database Schema** | `database/av_fleet_schema.sql` | 5 relational tables created with PKs, FK constraints, indexes, and ENUM types. | ✅ **VERIFIED** |
| **Part 2** | **JDBC Connectivity** | `java -cp "bin;lib/*" DBConnection` | Connects via `DriverManager`, reads environment variables, formats connection banner. | ✅ **VERIFIED** |
| **Part 3** | **Vehicle Registration** | `MainApp --test-registration` | 17-char VIN regex check, category validation, battery 0–100%, duplicate prevention. | ✅ **VERIFIED** |
| **Part 4** | **Pre-Trip Inspection** | `MainApp --test-inspection` | Validates sensor status (LiDAR/Radar/Camera must be `CALIBRATED`), battery $\ge 25\%$, quarantine lockout. | ✅ **VERIFIED** |
| **Part 5** | **In-Trip Telemetry Module** | `MainApp --test-telemetry` | Numerical bounds (LiDAR $0\text{--}100$, brakes $\ge 0$), string parsing with `trim()` & `split(",")`. | ✅ **VERIFIED** |
| **Part 6** | **Post-Trip Audit Module** | `MainApp --test-trip` | Disengagement rate $= (\text{Interventions} / \text{Distance}) \times 100$. Trip history tracking. | ✅ **VERIFIED** |
| **Part 7** | **ASI Calculation Engine** | `MainApp --test-asi` | Category-specific formulas (Robotaxi 50/30/20, Freight 40/40/20, Rover 60/30/10), safety bands. | ✅ **VERIFIED** |
| **Part 8** | **Safety Quarantine Module** | `MainApp --test-quarantine` | Enforces category thresholds ($>2, >1, >4$), uncalibrated sensor lockout, single active lock. | ✅ **VERIFIED** |
| **Part 9** | **Multithreading Engine** | `MainApp --test-multithreading` | Bounded thread pool (`FixedThreadPool(12)`), concurrent audit of 100+ AVs using `AtomicInteger`. | ✅ **VERIFIED** |
| **Part 10**| **CSV File Ingestion** | `MainApp --test-reports` (Test 1) | Ingests `data/telemetry_logs.csv`, parses 4 columns, logs malformed rows, valid counts recorded. | ✅ **VERIFIED** |
| **Part 11**| **TXT Report Generation** | `MainApp --test-reports` (Test 2 & 3)| Generates `fleet_safety_report.txt` and `quarantine_report.txt` using live database entities. | ✅ **VERIFIED** |
| **Part 12**| **Safety Certificate Generation** | `MainApp --test-reports` (Test 4)| Formats academic certificate via `String.format()` matching exact template. | ✅ **VERIFIED** |

---

## 2. Evidence of Test Executions

### A. File Handling & CSV Ingestion Test Execution
```text
========================================================
           TELEMETRY CSV INGESTION PIPELINE             
========================================================
[FILE I/O] Opening CSV File: .../data/telemetry_logs.csv
[FILE I/O] Detected & skipped CSV header: vehicle_id,lidar_confidence,manual_interventions,hard_braking_events
Line 10 REJECTED: Unknown vehicle_id '999' not registered in fleet inventory.
Line 11 REJECTED: Invalid lidar_confidence '-15.00' (LiDAR score -15.00 outside valid range 0-100.).
Line 12 REJECTED: Invalid manual_interventions '-3' (Manual interventions cannot be negative.).
Line 13 REJECTED: Missing columns. Expected 4 tokens, found 3. Payload: '8,90.00,1'
Line 14 REJECTED: Invalid lidar_confidence 'invalid_score' (For input string: "invalid_score").
--------------------------------------------------------
CSV Ingestion Summary: Total Rows = 13 | Valid = 8 | Rejected = 5
========================================================
```

### B. Multithreaded Fleet Safety Evaluation (100+ AVs)
```text
[MULTITHREADING] Initializing Fixed Thread Pool (Size: 12 concurrent workers)...
[MULTITHREADING] Preparing evaluation tasks for 100 autonomous vehicles...
[MULTITHREADING] Dispatching 100 worker threads to ExecutorService...
[MULTITHREADING] All 100 tasks successfully completed in 3 ms.

=====================================
MULTITHREADED FLEET SAFETY EVALUATION
=====================================
Total Vehicles: 100
Processed: 100
Platinum: 49
Gold: 36
Recalibration: 2
Grounded: 13
Quarantined: 13
Evaluation Status: COMPLETED
=====================================
```

### C. Generated Safety Certificate Verification
```text
================================================================================
                  AUTONOMOUS VEHICLE SAFETY AUDIT CERTIFICATE                   
================================================================================

Vehicle ID: AV001
Category: URBAN ROBOTAXI
Safety Score: 93.50
Grade: PLATINUM SAFETY
Audit Status: COMPLETED

================================================================================
```

---

## 3. Academic Presentation & OCI Screenshots Checklist

For project demonstration and evaluation report submissions, capture the following screenshots:

- [ ] **Screenshot 1: OCI Compute Console - VM 1 (Application Host)**  
  *Showing: Instance status "Running", Shape (e.g. `VM.Standard.E2.1.Micro`), Private IP (e.g. `10.0.0.15`), and OS.*
- [ ] **Screenshot 2: OCI Compute Console - VM 2 (Database Host)**  
  *Showing: Instance status "Running", Private IP (e.g. `10.0.0.25`), and Fault Domain.*
- [ ] **Screenshot 3: OCI VCN Security List Ingress Rule**  
  *Showing: Ingress rule permitting Source CIDR `<VM_1_PRIVATE_IP>/32`, Protocol `TCP`, Destination Port `3306`.*
- [ ] **Screenshot 4: VM 2 Host Firewall & MySQL Listener**  
  *Showing: Terminal output of `sudo ss -tulpn | grep 3306` and `sudo firewall-cmd --list-all` / `sudo ufw status`.*
- [ ] **Screenshot 5: VM 2 MySQL Schema & Dedicated User**  
  *Showing: MySQL terminal executing `SELECT user, host FROM mysql.user;` and `SHOW TABLES IN av_fleet_db;`.*
- [ ] **Screenshot 6: Remote JDBC Connection Success from VM 1**  
  *Showing: Terminal execution of `java -cp "bin:lib/*" DBConnection` outputting `Connection Status: SUCCESS`.*
- [ ] **Screenshot 7: Vehicle Registration & Regex Validation**  
  *Showing: CLI registration rejecting invalid 16-char or invalid regex VINs and successfully onboarding valid AV.*
- [ ] **Screenshot 8: CSV Ingestion with Error Handling**  
  *Showing: `TelemetryCSVReader` execution highlighting skipped header, rejected invalid rows, and valid counts.*
- [ ] **Screenshot 9: ASI Engine & Quarantine Override**  
  *Showing: ASI mathematical breakdown and safety quarantine enforcement for disengagement rate exceeding threshold.*
- [ ] **Screenshot 10: Multithreaded Fleet Safety Evaluation (100+ AVs)**  
  *Showing: Concurrent worker execution banner with summary tallies (Platinum, Gold, Recalibration, Grounded).*
- [ ] **Screenshot 11: Generated Text Audit Reports**  
  *Showing: Terminal `cat reports/fleet_safety_report.txt` and `cat reports/quarantine_report.txt`.*
- [ ] **Screenshot 12: Generated Safety Certificate**  
  *Showing: Formatted `reports/safety_certificate_AV001.txt`.*

---

## 4. Cloud Verification Note

> [!IMPORTANT]
> Cloud deployment verification is dependent upon student team credentials and active OCI tenancy. The software engine is 100% cloud-ready with decoupled environment variable configuration, zero hardcoded secrets, and distributed IP routing. Follow [`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md) when provisioning live OCI compute instances.
