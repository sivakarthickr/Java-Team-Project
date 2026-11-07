import java.util.List;
import java.util.Scanner;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: InTripTelemetryModule.java (Phase 2 In-Trip Telemetry Processor)
 * 
 * Description:
 * Ingests, validates, and processes high-frequency in-trip telemetry streams:
 * 1. LiDAR Point-Cloud Confidence Score (0 - 100)
 * 2. Manual Safety Driver Disengagements / Interventions (non-negative integer)
 * 3. Hard-Braking Events (non-negative integer)
 * 
 * String Processing Highlights:
 * - trim(): Cleans whitespace from user inputs and raw streaming tokens.
 * - split(","): Parses comma-separated stream payloads (<LiDAR>, <Interventions>, <HardBraking>).
 * - String.format(): Standardizes structured terminal summary cards.
 * =========================================================================================
 */
public class InTripTelemetryModule {

    private final FleetDAO fleetDAO;

    public InTripTelemetryModule() {
        this.fleetDAO = new FleetDAO();
    }

    public InTripTelemetryModule(FleetDAO fleetDAO) {
        this.fleetDAO = fleetDAO;
    }

    /**
     * Starts the interactive In-Trip Telemetry console menu.
     * 
     * @param scanner Shared input scanner
     */
    public void startMenu(Scanner scanner) {
        boolean exitMenu = false;
        while (!exitMenu) {
            System.out.println("\n========================================================");
            System.out.println("     PHASE 2: IN-TRIP TELEMETRY COLLECTION MODULE       ");
            System.out.println("========================================================");
            System.out.println("  [1] Record Telemetry (Interactive Prompts)");
            System.out.println("  [2] Record Telemetry (Comma-Separated Stream: LiDAR,Disengagements,Braking)");
            System.out.println("  [3] View Telemetry History for a Vehicle");
            System.out.println("  [4] View All Fleet Telemetry Logs");
            System.out.println("  [5] Return to Main Menu");
            System.out.println("========================================================");
            System.out.print("Select an option (1-5): ");

            String input = scanner.nextLine().trim();
            switch (input) {
                case "1":
                    handleInteractiveTelemetry(scanner);
                    break;
                case "2":
                    handleCsvStreamTelemetry(scanner);
                    break;
                case "3":
                    handleViewVehicleTelemetryHistory(scanner);
                    break;
                case "4":
                    handleViewAllFleetTelemetry();
                    break;
                case "5":
                    System.out.println("[INFO] Returning to Main Menu...");
                    exitMenu = true;
                    break;
                default:
                    System.err.println("[ERROR] Invalid choice. Please enter a number between 1 and 5.");
            }
        }
    }

    /**
     * Option 1: Record Telemetry via step-by-step interactive prompts.
     */
    public void handleInteractiveTelemetry(Scanner scanner) {
        System.out.println("\n--------------------------------------------------------");
        System.out.println("             RECORD IN-TRIP TELEMETRY                   ");
        System.out.println("--------------------------------------------------------");

        Vehicle vehicle = promptVehicleLookup(scanner);
        if (vehicle == null) return;

        double lidarConfidence = promptLidarConfidence(scanner);
        int manualInterventions = promptNonNegativeInt(scanner, "Manual Safety Driver Disengagements / Interventions");
        int hardBraking = promptNonNegativeInt(scanner, "Hard-Braking Events");

        processAndSaveTelemetry(vehicle, lidarConfidence, manualInterventions, hardBraking);
    }

    /**
     * Option 2: Record Telemetry using String processing: trim() and split(",").
     */
    public void handleCsvStreamTelemetry(Scanner scanner) {
        System.out.println("\n--------------------------------------------------------");
        System.out.println("     STREAM INGESTION (COMMA-SEPARATED TELEMETRY)       ");
        System.out.println("--------------------------------------------------------");

        Vehicle vehicle = promptVehicleLookup(scanner);
        if (vehicle == null) return;

        System.out.println("Enter comma-separated telemetry payload:");
        System.out.println("Format: <LiDAR Confidence: 0-100>, <Manual Interventions>, <Hard Braking Events>");
        System.out.println("Example: 95, 1, 2");
        System.out.print("Payload: ");

        String rawPayload = scanner.nextLine();
        TelemetryLog parsedLog = parseCommaSeparatedTelemetry(vehicle.getVehicleId(), rawPayload);

        if (parsedLog != null) {
            parsedLog.setVin(vehicle.getVin());
            boolean success = fleetDAO.insertTelemetryLog(parsedLog);
            if (success) {
                System.out.println("\n" + parsedLog.toSummaryString());
            } else {
                System.out.println("\n" + parsedLog.toSummaryString());
                System.out.println("[DB NOTICE] Telemetry processed (Offline/Simulation mode).");
            }
        }
    }

    /**
     * Parses and cleans a comma-separated telemetry string using trim() and split(",").
     * 
     * @param vehicleId Target vehicle ID
     * @param payload Raw string payload (e.g. "95, 1, 2")
     * @return TelemetryLog object or null if invalid
     */
    public TelemetryLog parseCommaSeparatedTelemetry(int vehicleId, String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            System.err.println("[ERROR] Telemetry payload cannot be empty.");
            return null;
        }

        // Apply trim() and split(",")
        String[] tokens = payload.trim().split(",");
        if (tokens.length < 3) {
            System.err.println(String.format(
                "[ERROR] Invalid format. Expected 3 comma-separated tokens (<LiDAR>, <Interventions>, <HardBraking>), received %d.",
                tokens.length
            ));
            return null;
        }

        try {
            // Clean individual tokens with trim()
            String token0 = tokens[0].trim();
            String token1 = tokens[1].trim();
            String token2 = tokens[2].trim();

            double lidarConfidence = Double.parseDouble(token0);
            if (lidarConfidence < 0.0 || lidarConfidence > 100.0) {
                System.err.println(String.format("[VALIDATION ERROR] LiDAR confidence (%.1f) must be between 0 and 100.", lidarConfidence));
                return null;
            }

            int interventions = Integer.parseInt(token1);
            if (interventions < 0) {
                System.err.println(String.format("[VALIDATION ERROR] Manual interventions (%d) must be a non-negative integer.", interventions));
                return null;
            }

            int hardBraking = Integer.parseInt(token2);
            if (hardBraking < 0) {
                System.err.println(String.format("[VALIDATION ERROR] Hard-braking events (%d) must be a non-negative integer.", hardBraking));
                return null;
            }

            return new TelemetryLog(vehicleId, lidarConfidence, interventions, hardBraking);

        } catch (NumberFormatException e) {
            System.err.println("[VALIDATION ERROR] Failed to parse numerical metrics from payload: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves telemetry data to MySQL and displays the standardized summary card.
     */
    private void processAndSaveTelemetry(Vehicle vehicle, double lidarConfidence, int manualInterventions, int hardBraking) {
        TelemetryLog log = new TelemetryLog(vehicle.getVehicleId(), lidarConfidence, manualInterventions, hardBraking);
        log.setVin(vehicle.getVin());

        boolean saved = fleetDAO.insertTelemetryLog(log);
        if (saved) {
            System.out.println("\n" + log.toSummaryString());
        } else {
            System.out.println("\n" + log.toSummaryString());
            System.out.println("[DB NOTICE] Telemetry recorded in simulation state.");
        }
    }

    /**
     * Option 3: View telemetry history for a specific vehicle.
     */
    public void handleViewVehicleTelemetryHistory(Scanner scanner) {
        System.out.println("\n--------------------------------------------------------");
        System.out.println("              VEHICLE TELEMETRY HISTORY                 ");
        System.out.println("--------------------------------------------------------");

        Vehicle vehicle = promptVehicleLookup(scanner);
        if (vehicle == null) return;

        List<TelemetryLog> logs = fleetDAO.getTelemetryLogsByVehicleId(vehicle.getVehicleId());
        if (logs.isEmpty()) {
            System.out.println(String.format("[INFO] No telemetry logs recorded yet for Vehicle AV%03d (%s).", 
                    vehicle.getVehicleId(), vehicle.getVin()));
            return;
        }

        System.out.println(String.format("\nHistorical Telemetry for Vehicle AV%03d (%s) - Total Records: %d", 
                vehicle.getVehicleId(), vehicle.getVin(), logs.size()));
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-16s | %-20s | %-16s | %-20s\n",
                "LOG ID", "LiDAR CONFIDENCE", "DISENGAGEMENTS", "HARD BRAKING", "TIMESTAMP");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (TelemetryLog log : logs) {
            System.out.printf("%-8d | %-16d | %-20d | %-16d | %-20s\n",
                    log.getLogId(),
                    log.getLidarConfidenceScore(),
                    log.getManualInterventions(),
                    log.getHardBrakingEvents(),
                    (log.getLogTimestamp() != null ? log.getLogTimestamp().toString() : "N/A"));
        }
        System.out.println("-----------------------------------------------------------------------------------------");
    }

    /**
     * Option 4: View all fleet telemetry records.
     */
    public void handleViewAllFleetTelemetry() {
        System.out.println("\n-----------------------------------------------------------------------------------------");
        System.out.println("                             FLEET-WIDE TELEMETRY AUDIT STREAM                           ");
        System.out.println("-----------------------------------------------------------------------------------------");

        List<TelemetryLog> logs = fleetDAO.getAllTelemetryLogs();
        if (logs.isEmpty()) {
            System.out.println("[INFO] No telemetry logs currently recorded in the database.");
            return;
        }

        System.out.printf("%-8s | %-10s | %-18s | %-14s | %-15s | %-13s | %-20s\n",
                "LOG ID", "VEHICLE", "VIN", "CONFIDENCE", "DISENGAGEMENTS", "HARD BRAKES", "TIMESTAMP");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (TelemetryLog log : logs) {
            System.out.printf("%-8d | AV%-803d | %-18s | %-14d | %-15d | %-13d | %-20s\n",
                    log.getLogId(),
                    log.getVehicleId(),
                    (log.getVin() != null ? log.getVin() : "N/A"),
                    log.getLidarConfidenceScore(),
                    log.getManualInterventions(),
                    log.getHardBrakingEvents(),
                    (log.getLogTimestamp() != null ? log.getLogTimestamp().toString() : "N/A"));
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println("Total Recorded Telemetry Points: " + logs.size());
    }

    // =========================================================================
    // HELPER INPUT PROMPTS
    // =========================================================================

    private Vehicle promptVehicleLookup(Scanner scanner) {
        System.out.print("Enter Numeric Vehicle ID (e.g., 1 for AV001) or 17-Char VIN: ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            System.err.println("[ERROR] Vehicle identifier cannot be blank.");
            return null;
        }

        // Try numeric ID lookup
        try {
            int id = Integer.parseInt(input);
            Vehicle v = fleetDAO.getVehicleById(id);
            if (v != null) return v;
        } catch (NumberFormatException ignored) {}

        // Try VIN lookup
        Vehicle v = fleetDAO.getVehicleByVin(input.toUpperCase());
        if (v != null) return v;

        System.err.println(String.format("[ERROR] Vehicle '%s' not found. Ensure vehicle is registered first.", input));
        return null;
    }

    private double promptLidarConfidence(Scanner scanner) {
        while (true) {
            System.out.print("Enter LiDAR Point-Cloud Confidence Score (0–100): ");
            String input = scanner.nextLine().trim();
            try {
                double score = Double.parseDouble(input);
                if (score >= 0.0 && score <= 100.0) {
                    return score;
                }
                System.err.println("[VALIDATION ERROR] Confidence score must be between 0 and 100.");
            } catch (NumberFormatException e) {
                System.err.println("[VALIDATION ERROR] Enter a valid numerical value.");
            }
        }
    }

    private int promptNonNegativeInt(Scanner scanner, String metricName) {
        while (true) {
            System.out.print("Enter " + metricName + " (non-negative integer): ");
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= 0) {
                    return value;
                }
                System.err.println("[VALIDATION ERROR] " + metricName + " cannot be negative.");
            } catch (NumberFormatException e) {
                System.err.println("[VALIDATION ERROR] Enter a valid integer number (e.g. 0, 1, 2).");
            }
        }
    }

    // =========================================================================
    // AUTOMATED TEST SUITE FOR IN-TRIP TELEMETRY
    // =========================================================================

    /**
     * Executes automated verification tests for the In-Trip Telemetry module.
     */
    public boolean runAutomatedTelemetryTests() {
        System.out.println("\n========================================================");
        System.out.println("      AUTOMATED IN-TRIP TELEMETRY VERIFICATION TESTS    ");
        System.out.println("========================================================");

        // Test 1: String Processing - trim() and split(",")
        System.out.println("[TEST 1] Testing String Processing with trim() & split(\",\"):");
        String streamPayload = "   95.0 ,   1  ,   2   ";
        TelemetryLog log1 = parseCommaSeparatedTelemetry(1, streamPayload);
        boolean test1Pass = (log1 != null && log1.getLidarConfidenceScore() == 95 
                             && log1.getManualInterventions() == 1 
                             && log1.getHardBrakingEvents() == 2);
        System.out.println("Payload Input: \"" + streamPayload + "\"");
        System.out.println("Parsed Output: LiDAR=" + (log1 != null ? log1.getLidarConfidenceScore() : "null")
                + ", Interventions=" + (log1 != null ? log1.getManualInterventions() : "null")
                + ", HardBraking=" + (log1 != null ? log1.getHardBrakingEvents() : "null"));
        System.out.println("Result: " + (test1Pass ? "PASS" : "FAIL") + "\n");

        // Test 2: Input Validation (Boundary Rejection)
        System.out.println("[TEST 2] Testing Input Validation & Boundary Checks:");
        boolean test2a = (parseCommaSeparatedTelemetry(1, "-5, 1, 2") == null);   // Negative LiDAR
        boolean test2b = (parseCommaSeparatedTelemetry(1, "105, 1, 2") == null);  // Excess LiDAR (>100)
        boolean test2c = (parseCommaSeparatedTelemetry(1, "95, -1, 2") == null);  // Negative interventions
        boolean test2d = (parseCommaSeparatedTelemetry(1, "95, 1, -3") == null);  // Negative hard brakes
        boolean test2Pass = test2a && test2b && test2c && test2d;
        System.out.println("Boundary Rejection Results: " + (test2Pass ? "PASS (All invalid boundaries caught)" : "FAIL") + "\n");

        // Test 3: Standard Output Formatting via String.format()
        System.out.println("[TEST 3] Testing Telemetry Summary Output Formatting:");
        TelemetryLog logSample = new TelemetryLog(1, 95.0, 1, 2);
        String formattedOutput = logSample.toSummaryString();
        System.out.println(formattedOutput);
        boolean test3Pass = formattedOutput.contains("VEHICLE: AV001") 
                         && formattedOutput.contains("LiDAR CONFIDENCE: 95") 
                         && formattedOutput.contains("MANUAL INTERVENTIONS: 1") 
                         && formattedOutput.contains("HARD BRAKING EVENTS: 2")
                         && formattedOutput.contains("STATUS: TELEMETRY RECORDED");
        System.out.println("Format Check: " + (test3Pass ? "PASS" : "FAIL") + "\n");

        System.out.println("========================================================");
        System.out.println("      IN-TRIP TELEMETRY TEST SUITE COMPLETED            ");
        System.out.println("========================================================\n");

        return test1Pass && test2Pass && test3Pass;
    }
}
