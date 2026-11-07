import java.util.Scanner;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: MainApp.java (Execution Orchestrator & CLI Master Menu)
 * 
 * Description:
 * Master CLI launcher coordinating:
 * - Vehicle Registration
 * - Phase 1: Pre-Trip Inspection
 * - Phase 2: In-Trip Telemetry
 * - Phase 3: Post-Trip Audit
 * - Category-Specific Autonomous Safety Index (ASI) Calculation Engine
 * - Safety Quarantine & Dispatch Revocation System
 * - High-Concurrency Multithreaded Fleet Safety Evaluator (100+ AVs)
 * =========================================================================================
 */
public class MainApp {

    // Regex pattern for standard 17-character VIN (excludes I, O, Q to avoid confusion with digits)
    public static final String VIN_REGEX = "^[A-HJ-NPR-Z0-9]{17}$";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE (SIMULATION)    ");
        System.out.println("================================================================================");
        System.out.println("[INFO] Initializing system modules and environment...");

        FleetDAO fleetDAO = new FleetDAO();
        ReportExporter reportExporter = new ReportExporter();
        VehicleRegistrationModule registrationModule = new VehicleRegistrationModule(fleetDAO);
        PreTripInspectionModule inspectionModule = new PreTripInspectionModule(fleetDAO);
        InTripTelemetryModule telemetryModule = new InTripTelemetryModule(fleetDAO);
        PostTripAuditModule tripModule = new PostTripAuditModule(fleetDAO);
        ASICalculationModule asiModule = new ASICalculationModule(fleetDAO);
        SafetyQuarantineModule quarantineModule = new SafetyQuarantineModule(fleetDAO, reportExporter);
        MultithreadedFleetAuditEngine multithreadedEngine = new MultithreadedFleetAuditEngine(fleetDAO);

        TelemetryCSVReader csvReader = new TelemetryCSVReader(fleetDAO);

        // Check if headless test argument was passed
        if (args.length > 0) {
            if ("--test-registration".equalsIgnoreCase(args[0])) {
                registrationModule.runAutomatedTests();
                return;
            } else if ("--test-inspection".equalsIgnoreCase(args[0])) {
                inspectionModule.runAutomatedInspectionTests();
                return;
            } else if ("--test-telemetry".equalsIgnoreCase(args[0])) {
                telemetryModule.runAutomatedTelemetryTests();
                return;
            } else if ("--test-trip".equalsIgnoreCase(args[0])) {
                tripModule.runAutomatedTripTests();
                return;
            } else if ("--test-asi".equalsIgnoreCase(args[0])) {
                asiModule.runAutomatedASITests();
                return;
            } else if ("--test-quarantine".equalsIgnoreCase(args[0])) {
                quarantineModule.runAutomatedQuarantineTests();
                return;
            } else if ("--test-multithreading".equalsIgnoreCase(args[0])) {
                multithreadedEngine.executeMultithreadedAudit(100);
                return;
            } else if ("--test-reports".equalsIgnoreCase(args[0]) || "--test-file-io".equalsIgnoreCase(args[0])) {
                runAutomatedFileHandlingTests(csvReader, reportExporter, fleetDAO);
                return;
            } else if ("--test-all".equalsIgnoreCase(args[0])) {
                registrationModule.runAutomatedTests();
                inspectionModule.runAutomatedInspectionTests();
                telemetryModule.runAutomatedTelemetryTests();
                tripModule.runAutomatedTripTests();
                asiModule.runAutomatedASITests();
                quarantineModule.runAutomatedQuarantineTests();
                multithreadedEngine.executeMultithreadedAudit(100);
                runAutomatedFileHandlingTests(csvReader, reportExporter, fleetDAO);
                return;
            }
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n================================================================================");
            System.out.println("                             MAIN APPLICATION MENU                              ");
            System.out.println("================================================================================");
            System.out.println("  [1] Vehicle Registration & Fleet Management");
            System.out.println("  [2] Pre-Trip Safety Inspection Module (Phase 1)");
            System.out.println("  [3] In-Trip Telemetry Collection Module (Phase 2)");
            System.out.println("  [4] Post-Trip Safety Audit Module (Phase 3)");
            System.out.println("  [5] Autonomous Safety Index (ASI) Calculation Engine");
            System.out.println("  [6] Vehicle Safety Quarantine & Compliance Module");
            System.out.println("  [7] Multithreaded Fleet Safety Evaluation (100+ Concurrent AVs)");
            System.out.println("  [8] Ingest Telemetry CSV File (data/telemetry_logs.csv)");
            System.out.println("  [9] Generate Fleet Safety & Quarantine Text Reports (reports/)");
            System.out.println("  [10] Generate Vehicle Safety Audit Certificate");
            System.out.println("  [11] Run Automated System Tests (All Modules + File I/O)");
            System.out.println("  [12] Test MySQL Database Connectivity");
            System.out.println("  [13] Exit Application");
            System.out.println("================================================================================");
            System.out.print("Select an option (1-13): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    registrationModule.startMenu(scanner);
                    break;
                case "2":
                    inspectionModule.startMenu(scanner);
                    break;
                case "3":
                    telemetryModule.startMenu(scanner);
                    break;
                case "4":
                    tripModule.startMenu(scanner);
                    break;
                case "5":
                    asiModule.startMenu(scanner);
                    break;
                case "6":
                    quarantineModule.startMenu(scanner);
                    break;
                case "7":
                    System.out.print("Enter fleet size to evaluate (minimum 100, press Enter for 100): ");
                    String sizeInput = scanner.nextLine().trim();
                    int size = 100;
                    try {
                        if (!sizeInput.isEmpty()) size = Math.max(100, Integer.parseInt(sizeInput));
                    } catch (NumberFormatException ignored) {}
                    multithreadedEngine.executeMultithreadedAudit(size);
                    break;
                case "8":
                    System.out.println("\n[ACTION] Ingesting CSV File: data/telemetry_logs.csv...");
                    csvReader.processTelemetryCsv();
                    break;
                case "9":
                    System.out.println("\n[ACTION] Generating Fleet Safety and Quarantine Reports in reports/...");
                    var vehicles = fleetDAO.getAllVehicles();
                    var qRecords = fleetDAO.getAllQuarantineHistory();
                    reportExporter.generateFleetSafetyReport(vehicles, fleetDAO);
                    reportExporter.generateQuarantineReport(qRecords, fleetDAO);
                    break;
                case "10":
                    System.out.print("Enter Vehicle ID to generate certificate (e.g., 1): ");
                    String idStr = scanner.nextLine().trim();
                    try {
                        int vId = Integer.parseInt(idStr);
                        Vehicle v = fleetDAO.getVehicleById(vId);
                        if (v != null) {
                            String cert = reportExporter.generateSafetyCertificate(v);
                            System.out.println("\n" + cert);
                        } else {
                            System.err.println("[ERROR] Vehicle ID " + vId + " not found.");
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("[ERROR] Invalid numeric Vehicle ID.");
                    }
                    break;
                case "11":
                    System.out.println("\n[INFO] Running complete automated verification suite...");
                    registrationModule.runAutomatedTests();
                    inspectionModule.runAutomatedInspectionTests();
                    telemetryModule.runAutomatedTelemetryTests();
                    tripModule.runAutomatedTripTests();
                    asiModule.runAutomatedASITests();
                    quarantineModule.runAutomatedQuarantineTests();
                    multithreadedEngine.executeMultithreadedAudit(100);
                    runAutomatedFileHandlingTests(csvReader, reportExporter, fleetDAO);
                    break;
                case "12":
                    DBConnection.testConnection();
                    break;
                case "13":
                    System.out.println("[INFO] Exiting AV Fleet Safety & Telemetry Audit Engine. Goodbye!");
                    running = false;
                    break;
                default:
                    System.err.println("[ERROR] Invalid choice. Please enter a number between 1 and 13.");
            }
        }
        scanner.close();
    }

    /**
     * Automated test runner for CSV file reading and TXT report generation.
     */
    private static void runAutomatedFileHandlingTests(TelemetryCSVReader csvReader, ReportExporter reportExporter, FleetDAO fleetDAO) {
        System.out.println("\n================================================================================");
        System.out.println("          RUNNING AUTOMATED TEST SUITE: FILE HANDLING & REPORTS                 ");
        System.out.println("================================================================================");

        // Test Part A: Telemetry CSV Ingestion
        System.out.println("[TEST 1] Ingesting Telemetry Logs from data/telemetry_logs.csv...");
        TelemetryCSVReader.IngestionResult result = csvReader.processTelemetryCsv();
        System.out.println(String.format(">> Telemetry Ingestion Results: Total=%d, Valid=%d, Rejected=%d",
                result.totalRows, result.validRows, result.rejectedRows));

        // Test Part B: Report Exporter
        System.out.println("\n[TEST 2] Generating Fleet Safety Report (reports/fleet_safety_report.txt)...");
        var vehicles = fleetDAO.getAllVehicles();
        if (vehicles.isEmpty()) {
            // Provide synthetic sample vehicles for headless export testing
            vehicles.add(new Vehicle(1, "1HGCR2F83HA000001", "URBAN_ROBOTAXI", "CALIBRATED", "CALIBRATED", "CALIBRATED", 94.0));
            vehicles.add(new Vehicle(2, "1FTFW1ED4MFA00002", "HIGHWAY_FREIGHT_TRUCK", "CALIBRATED", "CALIBRATED", "CALIBRATED", 88.0));
            vehicles.add(new Vehicle(3, "1FTFW1ED4MFA00003", "LAST_MILE_DELIVERY_ROVER", "CALIBRATED", "CALIBRATED", "CALIBRATED", 90.0));
        }
        boolean fleetRepSuccess = reportExporter.generateFleetSafetyReport(vehicles, fleetDAO);
        System.out.println(">> Fleet Safety Report Exported: " + (fleetRepSuccess ? "SUCCESS" : "FAILED"));

        System.out.println("\n[TEST 3] Generating Quarantine Registry Report (reports/quarantine_report.txt)...");
        var qRecords = fleetDAO.getAllQuarantineHistory();
        if (qRecords.isEmpty()) {
            qRecords.add(new QuarantineRecord(4, "1HGCR2F83HA000004", "URBAN_ROBOTAXI", "Disengagement rate 2.45 exceeds 2.00 limit", 2.45, 2.00));
        }
        boolean qRepSuccess = reportExporter.generateQuarantineReport(qRecords, fleetDAO);
        System.out.println(">> Quarantine Report Exported: " + (qRepSuccess ? "SUCCESS" : "FAILED"));

        // Test Part C: Safety Certificate
        System.out.println("\n[TEST 4] Generating Autonomous Vehicle Safety Audit Certificate...");
        Vehicle sample = vehicles.get(0);
        sample.setSafetyScore(93.50);
        sample.setSafetyGrade("PLATINUM_SAFETY");
        sample.setFleetStatus("ACTIVE");
        String cert = reportExporter.generateSafetyCertificate(sample);
        System.out.println(">> Generated Certificate Output:\n" + cert);
        System.out.println("================================================================================");
        System.out.println("       AUTOMATED TEST SUITE: FILE HANDLING & REPORTS COMPLETED                  ");
        System.out.println("================================================================================\n");
    }
}
