# Autonomous Vehicle (AV) Fleet Safety & Telemetry Audit Engine

> **Academic Java Mini Project (Set 21)**  
> **Simulation Scope:** This software is a data audit and safety analytics simulation engine. It does **not** connect to or control physical autonomous vehicle hardware.

---

## 1. Project Overview

The **Autonomous Vehicle (AV) Fleet Safety & Telemetry Audit Engine** is an academic enterprise-grade Java system designed to ingest, process, and audit high-frequency telemetry and trip streams across 100+ simulated Autonomous Vehicles (AVs).

The engine enforces stringent safety compliance thresholds across three vehicle operating categories:
1. **Urban Robotaxi**: Sensor Weight = 50%, Disengagement Rate = 30%, Rider Safety Rating = 20% (Max Disengagement Threshold: 2 per 100 km).
2. **Highway Freight Truck**: Sensor Weight = 40%, Lane Discipline = 40%, Braking Response = 20% (Max Disengagement Threshold: 1 per 100 km).
3. **Last-Mile Delivery Rover**: Obstacle Detection = 60%, Pedestrian Avoidance = 30%, Speed Adherence = 10% (Max Disengagement Threshold: 4 per 100 km).

Vehicles exceeding category thresholds are tagged with `SAFETY_QUARANTINE` and barred from autonomous dispatch, while overall health is graded into Autonomous Fleet Safety Bands (**Platinum Safety**, **Gold Fleet**, **Recalibration Required**, **Grounded**).

---

## 2. Team Architecture & Cloud Deployment (OCI)

The project is structured for a multi-node distributed cloud architecture deployed on **Oracle Cloud Infrastructure (OCI)**:

```
+---------------------------------------------+          JDBC Connection          +---------------------------------------------+
|    Student 1: Application Host (OCI Linux)  | --------------------------------> |     Student 2: Database Host (OCI Linux)    |
|                                             |    Port: 3306 (MySQL)             |                                             |
|  - Multithreaded Telemetry Processing       |    Ingress Security List Rule     |  - MySQL 8.0+ Service                       |
|  - String Parsing & Regex Validation        |    OS Firewall (iptables/ufw)     |  - Schema Management (av_fleet_schema.sql)  |
|  - File I/O (CSV Ingestion & Audit Reports) |                                   |  - Automated Persistence & Query Optimization|
+---------------------------------------------+                                   +---------------------------------------------+
```

---

## 3. Directory Structure

```text
AV_FLEET_SAFETY_AUDIT/
│
├── .vscode/
│   ├── settings.json         # VS Code Java classpath and source configuration
│   └── launch.json           # VS Code one-click execution configuration
│
├── src/
│   ├── MainApp.java          # Orchestrator & entry point (regex check, worker dispatch)
│   ├── DBConnection.java     # JDBC connection manager (supports local & OCI remote DB)
│   ├── FleetDAO.java         # Data Access Object separating SQL logic from business logic
│   ├── SafetyWorker.java     # Multithreaded evaluator implementing Runnable
│   └── ReportExporter.java   # File I/O module generating certificates & audit reports
│
├── database/
│   └── av_fleet_schema.sql   # DDL schema (vehicles, trips, telemetry_logs, quarantine_records)
│
├── data/
│   └── telemetry_logs.csv    # Phased incremental telemetry data for simulation
│
├── reports/
│   └── .gitkeep              # Destination folder for generated quarantine notices and reports
│
├── lib/
│   ├── mysql-connector-j-8.3.0.jar  # MySQL JDBC Driver
│   └── README.md                    # Dependency documentation & download instructions
│
├── .gitignore                # Ignores compiled bytecode (bin/) and generated reports
└── README.md                 # Project master documentation
```

---

## 4. Module Descriptions

| Module | File | Description & Key Responsibilities |
|---|---|---|
| **Vehicle Entity** | `src/Vehicle.java` | Domain model representing AV attributes: VIN, category, sensor calibration, battery, and safety ratings. |
| **Pre-Trip Entity** | `src/PreTripInspection.java` | Domain model for Phase 1 pre-trip inspections, storing calibration and battery clearance. |
| **Telemetry Entity** | `src/TelemetryLog.java` | Domain model for Phase 2 in-trip telemetry metrics (LiDAR confidence, disengagements, hard brakes, timestamp). |
| **Trip Entity** | `src/Trip.java` | Domain model for Phase 3 post-trip audit metrics (distance, violations, arrival score, disengagement rate). |
| **Quarantine Entity** | `src/QuarantineRecord.java` | Domain model representing formal safety compliance quarantine actions in MySQL `quarantine_records`. |
| **ASI Result Entity** | `src/ASIEvaluationResult.java` | Value object encapsulating calculated ASI scores, component contributions, and quarantine status. |
| **ASI Engine** | `src/ASIEngine.java` | Core mathematical engine computing category-specific ASI scores and enforcing quarantine overrides. |
| **Quarantine Module** | `src/SafetyQuarantineModule.java` | Manages automated quarantine evaluation, duplicate prevention, official notices, and admin resolutions. |
| **ASI Module** | `src/ASICalculationModule.java` | CLI module providing live MySQL vehicle safety audits, fleet batch evaluations, and formula simulations. |
| **Registration Module** | `src/VehicleRegistrationModule.java` | CLI menu module managing vehicle onboarding, regex enforcement, category checks, and search workflows. |
| **Inspection Module** | `src/PreTripInspectionModule.java` | Phase 1 pre-trip safety evaluator enforcing sensor calibration and battery dispatch thresholds. |
| **Telemetry Module** | `src/InTripTelemetryModule.java` | Phase 2 in-trip telemetry ingestion module using `trim()`, `split(",")`, and `String.format()`. |
| **Post-Trip Audit** | `src/PostTripAuditModule.java` | Phase 3 post-trip evaluation calculating disengagement rate and recording completed journeys. |
| **Main Orchestrator** | `src/MainApp.java` | Master application launcher providing interactive CLI routing and automated test triggers. |
| **Database Manager** | `src/DBConnection.java` | Manages JDBC connections to MySQL. Reads environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`) for local and OCI VM remote connectivity. |
| **Data Access Object** | `src/FleetDAO.java` | Encapsulates all SQL CRUD operations for `vehicles`, `trips`, `telemetry_logs`, `quarantine_records`, and `pre_trip_inspections`. |
| **Safety Evaluator** | `src/SafetyWorker.java` | Implements `Runnable` for concurrent background threads. Calculates Disengagement Rate and ASI, enforcing quarantine thresholds. |
| **Multithreading Engine**| `src/MultithreadedFleetAuditEngine.java` | Bounded thread pool (`FixedThreadPool`) orchestrating parallel safety evaluations for 100+ active AVs. |
| **CSV Ingestion** | `src/TelemetryCSVReader.java` | Java File Handling: parses `data/telemetry_logs.csv` with `BufferedReader`, `split(",")`, `trim()`, data range validation, error logging, and MySQL storage. |
| **Report Exporter** | `src/ReportExporter.java` | Uses Java File I/O (`BufferedWriter`) and `String.format()` to write formal `.txt` reports (`fleet_safety_report.txt`, `quarantine_report.txt`, and safety certificates) to `reports/`. |
| **Database DDL** | `database/av_fleet_schema.sql` | Defines the relational schema with foreign key constraints, indexes, and ENUM classifications. |
| **Simulation Data** | `data/telemetry_logs.csv` | Incremental telemetry logs containing `vehicle_id`, `lidar_confidence`, `manual_interventions`, `hard_braking_events`. |

---

## 5. Mathematical Formulas Implemented

### 1. Disengagement Rate
$$\text{DisengagementRate} = \left(\frac{\text{Total Manual Interventions}}{\text{Total Trip Distance (km)}}\right) \times 100$$

### 2. Autonomous Safety Index (ASI)
$$\text{ASI} = (\text{SensorScore} \times W_s) + \left[100 - \left(\frac{\text{Disengagements}}{\text{Distance}}\times 100\right)\right] \times W_d + (\text{Metric}_3 \times W_3)$$

### 3. Fleet Safety Bands
- **Platinum Safety (90–100):** Full Fleet Deployment
- **Gold Fleet (75–89):** Standard Operational Clearance
- **Recalibration Required (60–74):** Sensor Recalibration Mandatory
- **Grounded (< 60 or Exceeded Threshold):** Safety Quarantine Flagged (`SAFETY_QUARANTINE`)

---

## 6. Required Software & Prerequisites

1. **Java Development Kit (JDK):** Version 11, 17, or 21 (tested with OpenJDK / Oracle JDK).
2. **Database Engine:** MySQL Server 8.0+ or PostgreSQL 14+ (Local or OCI VM).
3. **IDE:** Visual Studio Code with the *Extension Pack for Java* (Red Hat / Microsoft).
4. **JDBC Driver:** MySQL Connector/J (`lib/mysql-connector-j-8.3.0.jar` included).

---

## 7. How to Compile and Run

### A. Using Visual Studio Code
1. Open the project folder in VS Code: `File -> Open Folder... -> JAVA PROJECT AV_FLEET_SAFETY_AUDIT`.
2. Open `src/MainApp.java`.
3. Press `F5` or click **Run Java** in the top-right corner.

### B. Using Command Prompt / PowerShell (Windows)

**1. Compile All Classes:**
```powershell
javac -cp "lib/*" -d bin src/*.java
```

**2. Test Database Connection:**
```powershell
java -cp "bin;lib/*" DBConnection
```

**3. Run Main Application:**
```powershell
java -cp "bin;lib/*" MainApp
```

### C. Using Bash (Linux / OCI VM)

**1. Compile All Classes:**
```bash
javac -cp "lib/*" -d bin src/*.java
```

**2. Test Database Connection:**
```bash
java -cp "bin:lib/*" DBConnection
```

**3. Run Main Application:**
```bash
java -cp "bin:lib/*" MainApp
```

**4. Run Automated Full-Cycle Verification:**
```bash
java -cp "bin:lib/*" MainApp --test-all
```

---

## 8. Academic Project Documentation Suite

For detailed deployment blueprints, network topologies, and verification reports, consult:
- 📘 [**DEPLOYMENT_GUIDE.md**](DEPLOYMENT_GUIDE.md): Complete step-by-step setup for VM 1 (Application) and VM 2 (Database), OS firewalls (`firewalld`/`ufw`), and OCI Security Lists.
- 📐 [**ARCHITECTURE.md**](ARCHITECTURE.md): System architecture diagrams, component specifications, and relational ERD.
- 🧪 [**TESTING_REPORT.md**](TESTING_REPORT.md): 12-part master verification checklist, execution logs, and academic submission screenshot checklist.
- 🔐 [**.env.example**](.env.example): Template for zero-hardcoded-credentials environment configuration.
