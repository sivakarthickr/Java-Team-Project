import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: PreTripInspectionModule.java (Phase 1 Safety Verification)
 * 
 * Description:
 * Collects and verifies pre-trip safety criteria before an autonomous vehicle is cleared
 * for trip dispatch:
 * 1. Vehicle VIN & Registration check
 * 2. LiDAR calibration status (Must be CALIBRATED)
 * 3. Radar calibration status (Must be CALIBRATED)
 * 4. Camera calibration status (Must be CALIBRATED)
 * 5. Battery/Fuel level (Must be >= MIN_BATTERY_THRESHOLD)
 * 
 * Generates standardized PRE-TRIP INSPECTION REPORT and persists audit records in MySQL.
 * =========================================================================================
 */
public class PreTripInspectionModule {

    // Minimum battery/fuel percentage required for autonomous dispatch
    public static final double MIN_BATTERY_THRESHOLD = 25.00;
    public static final double MAX_BATTERY_THRESHOLD = 100.00;

    private final FleetDAO fleetDAO;

    public PreTripInspectionModule() {
        this.fleetDAO = new FleetDAO();
    }

    public PreTripInspectionModule(FleetDAO fleetDAO) {
        this.fleetDAO = fleetDAO;
    }

    /**
     * Starts the interactive Pre-Trip Inspection console workflow.
     * 
     * @param scanner Shared input scanner
     */
    public void startMenu(Scanner scanner) {
        System.out.println("\n========================================================");
        System.out.println("     PHASE 1: PRE-TRIP SAFETY INSPECTION MODULE         ");
        System.out.println("========================================================");

        // 1. Enter and Validate VIN
        System.out.print("Enter 17-Character Vehicle VIN for Pre-Trip Inspection: ");
        String vin = scanner.nextLine().trim().toUpperCase();

        if (!VehicleRegistrationModule.isValidVin(vin)) {
            System.err.println("[INSPECTION REJECTED] Invalid VIN format: '" + vin + "'");
            System.err.println("  -> Must be 17 alphanumeric characters (excluding I, O, Q).");
            return;
        }

        // 2. Search for registered vehicle in database
        Vehicle vehicle = fleetDAO.getVehicleByVin(vin);
        if (vehicle == null) {
            System.err.println("\n[INSPECTION REJECTED] Unregistered Vehicle!");
            System.err.println("  -> Vehicle with VIN '" + vin + "' is not registered in the database.");
            System.err.println("  -> Vehicles must be registered in the system before undergoing pre-trip inspection.");
            return;
        }

        // 3. Display existing vehicle details
        System.out.println("\n[VEHICLE IDENTIFIED] Displaying existing vehicle records:");
        System.out.println(vehicle.toDisplayCard());

        // 4. Prompt for inspection telemetry mode
        System.out.println("\nInspection Telemetry Source:");
        System.out.println("  [1] Verify with Current Recorded Sensor & Battery Telemetry");
        System.out.println("  [2] Input New Simulated Pre-Trip Sensor & Battery Readings");
        System.out.print("Select mode (1 or 2): ");
        String modeChoice = scanner.nextLine().trim();

        String lidar = vehicle.getLidarStatus();
        String radar = vehicle.getRadarStatus();
        String camera = vehicle.getCameraStatus();
        double battery = vehicle.getBatteryLevel();

        if ("2".equals(modeChoice)) {
            lidar = promptSensorStatus(scanner, "LiDAR");
            radar = promptSensorStatus(scanner, "Radar");
            camera = promptSensorStatus(scanner, "Camera");
            battery = promptBattery(scanner);
        }

        // 5. Perform Pre-Trip Inspection Evaluation
        PreTripInspection inspection = evaluateInspection(vehicle, lidar, radar, camera, battery);

        // 6. Display Pre-Trip Inspection Report
        System.out.println("\n" + inspection.toReportString());

        // 7. Persist Inspection Record to MySQL
        boolean saved = fleetDAO.savePreTripInspection(inspection);
        if (saved) {
            System.out.println("[DB LOG] Pre-trip inspection record persisted in database.");
            if (PreTripInspection.STATUS_PASSED.equals(inspection.getInspectionStatus())) {
                fleetDAO.updateVehicleSensorAndBattery(vehicle.getVehicleId(), lidar, radar, camera, battery);
            }
        } else {
            System.out.println("[DB NOTICE] Pre-trip inspection record processed (Database offline or demo mode).");
        }
    }

    /**
     * Evaluates vehicle pre-trip parameters against autonomous safety dispatch rules.
     * 
     * @param vehicle The registered vehicle entity
     * @param lidar LiDAR calibration status
     * @param radar Radar calibration status
     * @param camera Camera calibration status
     * @param battery Battery/fuel percentage
     * @return PreTripInspection populated result object
     */
    public PreTripInspection evaluateInspection(Vehicle vehicle, String lidar, String radar, String camera, double battery) {
        List<String> failureReasons = new ArrayList<>();

        // Rule 0: Vehicle must not be in active quarantine or grounded
        if (Vehicle.STATUS_SAFETY_QUARANTINE.equalsIgnoreCase(vehicle.getFleetStatus())) {
            failureReasons.add("Vehicle is flagged under active SAFETY_QUARANTINE (Autonomous dispatch revoked)");
        }
        if ("GROUNDED".equalsIgnoreCase(vehicle.getSafetyGrade())) {
            failureReasons.add("Vehicle is GROUNDED due to critical safety score");
        }

        // Rule 1: All sensors must be CALIBRATED
        if (!"CALIBRATED".equalsIgnoreCase(lidar)) {
            failureReasons.add("LiDAR sensor is not calibrated (Current: " + lidar + ")");
        }
        if (!"CALIBRATED".equalsIgnoreCase(radar)) {
            failureReasons.add("Radar sensor is not calibrated (Current: " + radar + ")");
        }
        if (!"CALIBRATED".equalsIgnoreCase(camera)) {
            failureReasons.add("Camera sensor is not calibrated (Current: " + camera + ")");
        }

        // Rule 2: Battery/Fuel percentage must be within acceptable range [25% - 100%]
        if (battery < MIN_BATTERY_THRESHOLD) {
            failureReasons.add(String.format("Battery level (%.1f%%) is below minimum dispatch threshold (%.1f%%)", battery, MIN_BATTERY_THRESHOLD));
        } else if (battery > MAX_BATTERY_THRESHOLD) {
            failureReasons.add(String.format("Invalid battery level (%.1f%%) exceeds maximum 100.0%%", battery));
        }

        String status = failureReasons.isEmpty() ? PreTripInspection.STATUS_PASSED : PreTripInspection.STATUS_FAILED;
        String combinedReason = failureReasons.isEmpty() ? null : String.join("; ", failureReasons);

        return new PreTripInspection(vehicle.getVehicleId(), vehicle.getVin(), lidar, radar, camera, battery, status, combinedReason);
    }

    // =========================================================================
    // HELPER INPUT PROMPTS
    // =========================================================================

    private String promptSensorStatus(Scanner scanner, String sensorName) {
        System.out.println("Select " + sensorName + " Calibration Reading:");
        System.out.println("  [1] CALIBRATED");
        System.out.println("  [2] DEGRADED");
        System.out.println("  [3] UNCALIBRATED");
        System.out.print("Choice (1-3): ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "2": return "DEGRADED";
            case "3": return "UNCALIBRATED";
            case "1":
            default:
                return "CALIBRATED";
        }
    }

    private double promptBattery(Scanner scanner) {
        while (true) {
            System.out.print("Enter Measured Battery/Fuel Percentage (0 - 100): ");
            String input = scanner.nextLine().trim();
            try {
                double level = Double.parseDouble(input);
                if (level >= 0.0 && level <= 100.0) {
                    return Math.round(level * 10.0) / 10.0;
                }
                System.err.println("[ERROR] Battery percentage must be between 0.0% and 100.0%.");
            } catch (NumberFormatException e) {
                System.err.println("[ERROR] Enter a valid numeric decimal percentage.");
            }
        }
    }

    // =========================================================================
    // AUTOMATED TEST SUITE FOR PRE-TRIP INSPECTION
    // =========================================================================

    /**
     * Executes automated verification for pre-trip inspection rules.
     */
    public boolean runAutomatedInspectionTests() {
        System.out.println("\n========================================================");
        System.out.println("      AUTOMATED PRE-TRIP INSPECTION TESTS               ");
        System.out.println("========================================================");

        Vehicle sampleVehicle = new Vehicle(1, "1HGCR2F83HA001234", "URBAN_ROBOTAXI",
                "CALIBRATED", "CALIBRATED", "CALIBRATED", 95.0, null, null, "ACTIVE", null);

        // Test 1: All Sensors Calibrated & Good Battery -> PASSED
        System.out.println("[TEST 1] Nominal Vehicle Inspection (All Calibrated, Battery 95%):");
        PreTripInspection t1 = evaluateInspection(sampleVehicle, "CALIBRATED", "CALIBRATED", "CALIBRATED", 95.0);
        System.out.println(t1.toReportString());
        boolean pass1 = PreTripInspection.STATUS_PASSED.equals(t1.getInspectionStatus());
        System.out.println("Result: " + (pass1 ? "PASS" : "FAIL") + "\n");

        // Test 2: Uncalibrated Sensor (LiDAR DEGRADED) -> FAILED
        System.out.println("[TEST 2] Sensor Calibration Failure (LiDAR DEGRADED):");
        PreTripInspection t2 = evaluateInspection(sampleVehicle, "DEGRADED", "CALIBRATED", "CALIBRATED", 90.0);
        System.out.println(t2.toReportString());
        boolean pass2 = PreTripInspection.STATUS_FAILED.equals(t2.getInspectionStatus());
        System.out.println("Result: " + (pass2 ? "PASS (Correctly Rejected)" : "FAIL") + "\n");

        // Test 3: Insufficient Battery Level (15% < 25%) -> FAILED
        System.out.println("[TEST 3] Insufficient Battery Inspection (Battery 15.0%):");
        PreTripInspection t3 = evaluateInspection(sampleVehicle, "CALIBRATED", "CALIBRATED", "CALIBRATED", 15.0);
        System.out.println(t3.toReportString());
        boolean pass3 = PreTripInspection.STATUS_FAILED.equals(t3.getInspectionStatus());
        System.out.println("Result: " + (pass3 ? "PASS (Correctly Rejected)" : "FAIL") + "\n");

        // Test 4: Quarantined Vehicle Attempting Pre-Trip Inspection -> FAILED
        System.out.println("[TEST 4] Quarantined Vehicle Inspection Check:");
        Vehicle quarantinedVehicle = new Vehicle(2, "1G1ZD5ST8HF445566", "URBAN_ROBOTAXI",
                "CALIBRATED", "CALIBRATED", "CALIBRATED", 85.0, 48.0, "GROUNDED", "SAFETY_QUARANTINE", null);
        PreTripInspection t4 = evaluateInspection(quarantinedVehicle, "CALIBRATED", "CALIBRATED", "CALIBRATED", 85.0);
        System.out.println(t4.toReportString());
        boolean pass4 = PreTripInspection.STATUS_FAILED.equals(t4.getInspectionStatus());
        System.out.println("Result: " + (pass4 ? "PASS (Correctly Blocked from Dispatch)" : "FAIL") + "\n");

        System.out.println("========================================================");
        System.out.println("      PRE-TRIP TEST SUITE COMPLETED                     ");
        System.out.println("========================================================\n");

        return pass1 && pass2 && pass3 && pass4;
    }
}
