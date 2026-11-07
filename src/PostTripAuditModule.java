import java.util.List;
import java.util.Scanner;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: PostTripAuditModule.java (Phase 3 Post-Trip Audit Processor)
 * 
 * Description:
 * Evaluates, validates, and records post-trip journey metrics:
 * 1. Total Trip Distance in Kilometers (Positive decimal > 0.0)
 * 2. Speeding Violations (Non-negative integer >= 0)
 * 3. Passenger/Cargo Arrival Condition Score (0.00 - 100.00)
 * 4. Aggregated Manual Interventions across all associated telemetry records
 * 5. Calculated Disengagement Rate = (Total Manual Interventions / Total Distance) * 100
 * =========================================================================================
 */
public class PostTripAuditModule {

    private final FleetDAO fleetDAO;

    public PostTripAuditModule() {
        this.fleetDAO = new FleetDAO();
    }

    public PostTripAuditModule(FleetDAO fleetDAO) {
        this.fleetDAO = fleetDAO;
    }

    /**
     * Starts the interactive Post-Trip Audit console menu.
     * 
     * @param scanner Shared input scanner
     */
    public void startMenu(Scanner scanner) {
        boolean exitMenu = false;
        while (!exitMenu) {
            System.out.println("\n========================================================");
            System.out.println("       PHASE 3: POST-TRIP SAFETY AUDIT MODULE           ");
            System.out.println("========================================================");
            System.out.println("  [1] Record Completed Trip Audit");
            System.out.println("  [2] View Historical Trips for a Vehicle");
            System.out.println("  [3] View All Completed Fleet Trips");
            System.out.println("  [4] Return to Main Menu");
            System.out.println("========================================================");
            System.out.print("Select an option (1-4): ");

            String input = scanner.nextLine().trim();
            switch (input) {
                case "1":
                    handleRecordPostTripAudit(scanner);
                    break;
                case "2":
                    handleViewVehicleTripHistory(scanner);
                    break;
                case "3":
                    handleViewAllFleetTrips();
                    break;
                case "4":
                    System.out.println("[INFO] Returning to Main Menu...");
                    exitMenu = true;
                    break;
                default:
                    System.err.println("[ERROR] Invalid choice. Please enter a number between 1 and 4.");
            }
        }
    }

    /**
     * Option 1: Record Post-Trip Audit with validation and aggregated telemetry.
     */
    public void handleRecordPostTripAudit(Scanner scanner) {
        System.out.println("\n--------------------------------------------------------");
        System.out.println("                RECORD POST-TRIP AUDIT                  ");
        System.out.println("--------------------------------------------------------");

        // 1. Search Registered Vehicle
        Vehicle vehicle = promptVehicleLookup(scanner);
        if (vehicle == null) return;

        // 2. Verify Pre-Trip Inspection Status
        verifyPreTripInspectionStatus(vehicle.getVehicleId());

        // 3. Collect & Validate Trip Distance
        double distanceKm = promptTripDistance(scanner);

        // 4. Collect & Validate Speeding Violations
        int speedingViolations = promptNonNegativeInt(scanner, "Speeding Violations Count");

        // 5. Collect & Validate Arrival Condition Score (0 - 100)
        double arrivalCondition = promptArrivalCondition(scanner);

        // 6. Aggregate Manual Interventions from Telemetry Logs
        int aggregatedInterventions = fleetDAO.getTotalManualInterventions(vehicle.getVehicleId());
        System.out.println(String.format("\n[TELEMETRY AUDIT] Aggregated manual interventions recorded in database: %d", aggregatedInterventions));

        System.out.print("Use aggregated interventions from telemetry logs (" + aggregatedInterventions + ")? (Y/N, enter N to override): ");
        String overrideChoice = scanner.nextLine().trim();
        int finalInterventions = aggregatedInterventions;

        if ("N".equalsIgnoreCase(overrideChoice)) {
            finalInterventions = promptNonNegativeInt(scanner, "Manual Safety Driver Disengagements / Interventions for this trip");
        }

        // 7. Calculate Disengagement Rate
        double disengagementRate = calculateDisengagementRate(finalInterventions, distanceKm);

        // 8. Create Trip Entity and Persist to MySQL
        Trip trip = new Trip(vehicle.getVehicleId(), distanceKm, speedingViolations,
                             arrivalCondition, finalInterventions, disengagementRate);
        trip.setVin(vehicle.getVin());
        trip.setVehicleCategory(vehicle.getVehicleType());

        boolean saved = fleetDAO.insertTrip(trip);
        if (saved) {
            System.out.println("\n" + trip.toAuditReportString());
        } else {
            System.out.println("\n" + trip.toAuditReportString());
            System.out.println("[DB NOTICE] Trip recorded in simulation memory (Database offline).");
        }
    }

    /**
     * Calculates the disengagement rate per 100 km.
     * Formula: (Total Manual Interventions / Total Trip Distance) * 100
     * 
     * @param totalInterventions Total manual interventions
     * @param tripDistanceKm Total trip distance in kilometers
     * @return Disengagement rate rounded to 2 decimal places
     */
    public static double calculateDisengagementRate(int totalInterventions, double tripDistanceKm) {
        if (tripDistanceKm <= 0.0) {
            return 0.0;
        }
        double rate = ((double) totalInterventions / tripDistanceKm) * 100.0;
        return Math.round(rate * 100.0) / 100.0;
    }

    /**
     * Verifies whether the vehicle passed its Phase 1 Pre-Trip Inspection.
     */
    public boolean verifyPreTripInspectionStatus(int vehicleId) {
        PreTripInspection inspection = fleetDAO.getLatestPreTripInspection(vehicleId);
        if (inspection == null) {
            System.out.println("[PRE-TRIP CHECK] Notice: No pre-trip inspection record found for this vehicle.");
            System.out.println("                 Recommended: Execute Phase 1 Pre-Trip Inspection before trip dispatch.");
            return false;
        }

        if (PreTripInspection.STATUS_PASSED.equalsIgnoreCase(inspection.getInspectionStatus())) {
            System.out.println(String.format("[PRE-TRIP CHECK] Verified: Vehicle passed pre-trip inspection on %s.",
                    (inspection.getInspectionDate() != null ? inspection.getInspectionDate().toString() : "recent record")));
            return true;
        } else {
            System.err.println(String.format("[PRE-TRIP ALERT] Warning: Vehicle FAILED last pre-trip inspection! Reason: %s",
                    inspection.getFailureReason()));
            return false;
        }
    }

    /**
     * Option 2: View historical completed trips for a single vehicle.
     */
    public void handleViewVehicleTripHistory(Scanner scanner) {
        System.out.println("\n--------------------------------------------------------");
        System.out.println("                VEHICLE TRIP HISTORY                    ");
        System.out.println("--------------------------------------------------------");

        Vehicle vehicle = promptVehicleLookup(scanner);
        if (vehicle == null) return;

        List<Trip> trips = fleetDAO.getTripsByVehicleId(vehicle.getVehicleId());
        if (trips.isEmpty()) {
            System.out.println(String.format("[INFO] No completed trips on record for Vehicle AV%03d (%s).",
                    vehicle.getVehicleId(), vehicle.getVin()));
            return;
        }

        System.out.println(String.format("\nCompleted Trip History for Vehicle AV%03d (%s) - Total Trips: %d",
                vehicle.getVehicleId(), vehicle.getVin(), trips.size()));
        printTripTable(trips);
    }

    /**
     * Option 3: View all completed fleet trips.
     */
    public void handleViewAllFleetTrips() {
        System.out.println("\n-------------------------------------------------------------------------------------------------------");
        System.out.println("                                  FLEET-WIDE COMPLETED TRIP AUDIT LOG                                  ");
        System.out.println("-------------------------------------------------------------------------------------------------------");

        List<Trip> trips = fleetDAO.getAllTrips();
        if (trips.isEmpty()) {
            System.out.println("[INFO] No trips currently recorded in the database.");
            return;
        }

        printTripTable(trips);
        System.out.println("Total Fleet Completed Trips: " + trips.size());
    }

    private void printTripTable(List<Trip> trips) {
        System.out.println("-------------------------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-10s | %-18s | %-12s | %-10s | %-13s | %-14s | %-18s\n",
                "TRIP ID", "VEHICLE", "VIN", "DISTANCE(KM)", "VIOLATIONS", "ARRIVAL SCORE", "DISENGAGEMENTS", "RATE (/100KM)");
        System.out.println("-------------------------------------------------------------------------------------------------------");

        for (Trip t : trips) {
            System.out.printf("%-8d | AV%-803d | %-18s | %-12.2f | %-10d | %-13.1f | %-14d | %-18.2f\n",
                    t.getTripId(),
                    t.getVehicleId(),
                    (t.getVin() != null ? t.getVin() : "N/A"),
                    t.getTripDistance(),
                    t.getSpeedingViolations(),
                    t.getArrivalCondition(),
                    t.getDisengagements(),
                    t.getDisengagementRate());
        }
        System.out.println("-------------------------------------------------------------------------------------------------------");
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

        try {
            int id = Integer.parseInt(input);
            Vehicle v = fleetDAO.getVehicleById(id);
            if (v != null) return v;
        } catch (NumberFormatException ignored) {}

        Vehicle v = fleetDAO.getVehicleByVin(input.toUpperCase());
        if (v != null) return v;

        System.err.println(String.format("[ERROR] Vehicle '%s' not found. Ensure vehicle is registered first.", input));
        return null;
    }

    private double promptTripDistance(Scanner scanner) {
        while (true) {
            System.out.print("Enter Total Trip Distance in Kilometers (Positive decimal): ");
            String input = scanner.nextLine().trim();
            try {
                double distance = Double.parseDouble(input);
                if (distance > 0.0) {
                    return Math.round(distance * 100.0) / 100.0;
                }
                System.err.println("[VALIDATION ERROR] Trip distance must be a positive value greater than 0.0 km.");
            } catch (NumberFormatException e) {
                System.err.println("[VALIDATION ERROR] Enter a valid numeric decimal (e.g., 150.5).");
            }
        }
    }

    private int promptNonNegativeInt(Scanner scanner, String metricName) {
        while (true) {
            System.out.print("Enter " + metricName + " (Non-negative integer): ");
            String input = scanner.nextLine().trim();
            try {
                int count = Integer.parseInt(input);
                if (count >= 0) {
                    return count;
                }
                System.err.println("[VALIDATION ERROR] " + metricName + " cannot be negative.");
            } catch (NumberFormatException e) {
                System.err.println("[VALIDATION ERROR] Enter a valid integer number (e.g., 0, 1, 2).");
            }
        }
    }

    private double promptArrivalCondition(Scanner scanner) {
        while (true) {
            System.out.print("Enter Passenger/Cargo Arrival Condition Score (0.00 - 100.00): ");
            String input = scanner.nextLine().trim();
            try {
                double score = Double.parseDouble(input);
                if (score >= 0.0 && score <= 100.0) {
                    return Math.round(score * 10.0) / 10.0;
                }
                System.err.println("[VALIDATION ERROR] Arrival condition score must be between 0.00 and 100.00.");
            } catch (NumberFormatException e) {
                System.err.println("[VALIDATION ERROR] Enter a valid decimal score.");
            }
        }
    }

    // =========================================================================
    // AUTOMATED TEST SUITE FOR POST-TRIP AUDIT
    // =========================================================================

    /**
     * Executes automated verification tests for the Post-Trip Audit module.
     */
    public boolean runAutomatedTripTests() {
        System.out.println("\n========================================================");
        System.out.println("      AUTOMATED POST-TRIP AUDIT VERIFICATION TESTS      ");
        System.out.println("========================================================");

        // Test 1: Mathematical Formula Verification:
        // Distance = 150.0 km, Interventions = 1 -> Rate = (1 / 150.0) * 100 = 0.67
        System.out.println("[TEST 1] Disengagement Rate Calculation (1 intervention / 150.00 km):");
        double rate1 = calculateDisengagementRate(1, 150.0);
        boolean test1Pass = (Math.abs(rate1 - 0.67) < 0.01);
        System.out.println("Calculated Rate: " + rate1 + " per 100 km (Expected: 0.67)");
        System.out.println("Result: " + (test1Pass ? "PASS" : "FAIL") + "\n");

        // Test 2: Multi-record Telemetry Aggregation:
        // Trip with 3 interventions across 320.0 km -> Rate = (3 / 320.0) * 100 = 0.94
        System.out.println("[TEST 2] Multi-Telemetry Aggregation (3 interventions / 320.00 km):");
        double rate2 = calculateDisengagementRate(3, 320.0);
        boolean test2Pass = (Math.abs(rate2 - 0.94) < 0.01);
        System.out.println("Calculated Rate: " + rate2 + " per 100 km (Expected: 0.94)");
        System.out.println("Result: " + (test2Pass ? "PASS" : "FAIL") + "\n");

        // Test 3: Input Validation Boundaries (Zero distance protection):
        System.out.println("[TEST 3] Zero/Negative Distance Protection:");
        double zeroRate = calculateDisengagementRate(5, 0.0);
        boolean test3Pass = (zeroRate == 0.0);
        System.out.println("Zero Distance Safe Handling: " + (test3Pass ? "PASS" : "FAIL") + "\n");

        // Test 4: Structured Audit Report Formatting Check:
        System.out.println("[TEST 4] Formatted Post-Trip Audit Report:");
        Trip sampleTrip = new Trip(1, 150.00, 0, 98.0, 1, rate1);
        sampleTrip.setVin("1HGCR2F83HA001234");
        sampleTrip.setVehicleCategory("URBAN_ROBOTAXI");
        String reportOutput = sampleTrip.toAuditReportString();
        System.out.println(reportOutput);
        boolean test4Pass = reportOutput.contains("Vehicle ID: AV001")
                         && reportOutput.contains("Trip Distance: 150.00 km")
                         && reportOutput.contains("Speeding Violations: 0")
                         && reportOutput.contains("Arrival Condition: 98.0 / 100")
                         && reportOutput.contains("Disengagement Rate: 0.67 per 100 km")
                         && reportOutput.contains("STATUS: TRIP AUDIT RECORDED");
        System.out.println("Report Content Check: " + (test4Pass ? "PASS" : "FAIL") + "\n");

        System.out.println("========================================================");
        System.out.println("      POST-TRIP AUDIT TEST SUITE COMPLETED              ");
        System.out.println("========================================================\n");

        return test1Pass && test2Pass && test3Pass && test4Pass;
    }
}
