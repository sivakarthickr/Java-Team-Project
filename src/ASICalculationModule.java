import java.util.List;
import java.util.Scanner;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: ASICalculationModule.java (ASI CLI Interaction & Verification)
 * 
 * Description:
 * Manages the CLI interface for the Autonomous Safety Index calculation engine.
 * Allows on-demand evaluation of vehicles, fleet batch evaluation, and simulation testing.
 * =========================================================================================
 */
public class ASICalculationModule {

    private final ASIEngine asiEngine;
    private final FleetDAO fleetDAO;

    public ASICalculationModule() {
        this.fleetDAO = new FleetDAO();
        this.asiEngine = new ASIEngine(fleetDAO);
    }

    public ASICalculationModule(FleetDAO fleetDAO) {
        this.fleetDAO = fleetDAO;
        this.asiEngine = new ASIEngine(fleetDAO);
    }

    /**
     * Starts the interactive ASI module menu.
     * 
     * @param scanner Shared input scanner
     */
    public void startMenu(Scanner scanner) {
        boolean exitMenu = false;
        while (!exitMenu) {
            System.out.println("\n========================================================");
            System.out.println("  AUTONOMOUS SAFETY INDEX (ASI) CALCULATION ENGINE      ");
            System.out.println("========================================================");
            System.out.println("  [1] Evaluate Safety for a Vehicle (Live Database)");
            System.out.println("  [2] Batch Evaluate All Registered Vehicles");
            System.out.println("  [3] Category Formula Simulation Calculator");
            System.out.println("  [4] Return to Main Menu");
            System.out.println("========================================================");
            System.out.print("Select an option (1-4): ");

            String input = scanner.nextLine().trim();
            switch (input) {
                case "1":
                    handleEvaluateSingleVehicle(scanner);
                    break;
                case "2":
                    handleBatchEvaluateFleet();
                    break;
                case "3":
                    handleFormulaSimulation(scanner);
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
     * Option 1: Evaluate a single vehicle using live MySQL telemetry and trip metrics.
     */
    public void handleEvaluateSingleVehicle(Scanner scanner) {
        System.out.println("\n--------------------------------------------------------");
        System.out.println("            EVALUATE VEHICLE SAFETY INDEX               ");
        System.out.println("--------------------------------------------------------");
        System.out.print("Enter Numeric Vehicle ID (e.g. 1) or 17-Character VIN: ");
        String input = scanner.nextLine().trim();

        int vehicleId = -1;
        try {
            vehicleId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Vehicle v = fleetDAO.getVehicleByVin(input.toUpperCase());
            if (v != null) {
                vehicleId = v.getVehicleId();
            }
        }

        if (vehicleId <= 0) {
            System.err.println("[ERROR] Vehicle not found. Ensure vehicle is registered in the database.");
            return;
        }

        ASIEvaluationResult result = asiEngine.evaluateVehicle(vehicleId);
        if (result != null) {
            System.out.println("\n" + result.toEvaluationReportString());
        } else {
            System.err.println("[ERROR] Failed to retrieve or calculate evaluation for vehicle ID " + vehicleId);
        }
    }

    /**
     * Option 2: Evaluates every vehicle registered in the fleet.
     */
    public void handleBatchEvaluateFleet() {
        System.out.println("\n-------------------------------------------------------------------------------------------------------------");
        System.out.println("                                 FLEET-WIDE AUTONOMOUS SAFETY AUDIT SUMMARY                                  ");
        System.out.println("-------------------------------------------------------------------------------------------------------------");

        List<Vehicle> vehicles = fleetDAO.getAllVehicles();
        if (vehicles.isEmpty()) {
            System.out.println("[INFO] No vehicles currently registered in the database.");
            return;
        }

        System.out.printf("%-5s | %-18s | %-24s | %-10s | %-10s | %-12s | %-18s\n",
                "ID", "VIN", "CATEGORY", "RATE/100K", "THRESHOLD", "RAW ASI", "ASSIGNED GRADE");
        System.out.println("-------------------------------------------------------------------------------------------------------------");

        int quarantinedCount = 0;
        for (Vehicle v : vehicles) {
            ASIEvaluationResult res = asiEngine.evaluateVehicle(v.getVehicleId());
            if (res != null) {
                if (res.isQuarantineTriggered()) quarantinedCount++;
                System.out.printf("%-5d | %-18s | %-24s | %-10.2f | %-10.2f | %-12.2f | %-18s\n",
                        res.getVehicleId(),
                        res.getVin(),
                        res.getCategory(),
                        res.getDisengagementRate(),
                        res.getCategoryThreshold(),
                        res.getRawAsiScore(),
                        res.getAssignedGrade());
            }
        }
        System.out.println("-------------------------------------------------------------------------------------------------------------");
        System.out.println(String.format("Audit Completed for %d vehicle(s) | Active Safety Quarantines: %d", 
                vehicles.size(), quarantinedCount));
    }

    /**
     * Option 3: Configurable Simulation Calculator testing hypothetical inputs.
     */
    public void handleFormulaSimulation(Scanner scanner) {
        System.out.println("\n--------------------------------------------------------");
        System.out.println("             CATEGORY ASI FORMULA SIMULATOR             ");
        System.out.println("--------------------------------------------------------");
        System.out.println("Select Category to Simulate:");
        System.out.println("  [1] URBAN ROBOTAXI (Weights: 50% / 30% / 20% | Threshold: 2/100km)");
        System.out.println("  [2] HIGHWAY FREIGHT TRUCK (Weights: 40% / 40% / 20% | Threshold: 1/100km)");
        System.out.println("  [3] LAST-MILE DELIVERY ROVER (Weights: 60% / 30% / 10% | Threshold: 4/100km)");
        System.out.print("Choice (1-3): ");

        String catChoice = scanner.nextLine().trim();
        System.out.print("Enter Trip Distance (km): ");
        double dist = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Enter Total Disengagements: ");
        int disengagements = Integer.parseInt(scanner.nextLine().trim());

        double disengagementRate = (disengagements / dist) * 100.0;
        double disengagementScore = ASIEngine.calculateDisengagementScore(disengagementRate);

        double asiScore = 0.0;
        double threshold = 2.0;
        String catName = "URBAN_ROBOTAXI";

        switch (catChoice) {
            case "2":
                catName = "HIGHWAY_FREIGHT_TRUCK";
                threshold = ASIEngine.THRESHOLD_HIGHWAY_FREIGHT;
                System.out.print("Enter Sensor Score (0-100): ");
                double sFreight = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("Enter Braking Response Score (0-100): ");
                double bFreight = Double.parseDouble(scanner.nextLine().trim());
                asiScore = ASIEngine.calculateHighwayFreightASI(sFreight, disengagementScore, bFreight);
                break;
            case "3":
                catName = "LAST_MILE_DELIVERY_ROVER";
                threshold = ASIEngine.THRESHOLD_DELIVERY_ROVER;
                System.out.print("Enter Obstacle Detection Score (0-100): ");
                double sRover = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("Enter Speed Adherence Score (0-100): ");
                double spRover = Double.parseDouble(scanner.nextLine().trim());
                asiScore = ASIEngine.calculateLastMileRoverASI(sRover, disengagementScore, spRover);
                break;
            case "1":
            default:
                catName = "URBAN_ROBOTAXI";
                threshold = ASIEngine.THRESHOLD_URBAN_ROBOTAXI;
                System.out.print("Enter Sensor Score (0-100): ");
                double sTaxi = Double.parseDouble(scanner.nextLine().trim());
                System.out.print("Enter Rider Safety Rating (0-100): ");
                double rTaxi = Double.parseDouble(scanner.nextLine().trim());
                asiScore = ASIEngine.calculateUrbanRobotaxiASI(sTaxi, disengagementScore, rTaxi);
                break;
        }

        boolean isQuarantined = (disengagementRate > threshold);
        String finalGrade = ASIEngine.determineSafetyGrade(asiScore, isQuarantined);

        System.out.println("\n--- SIMULATION RESULTS ---");
        System.out.println(String.format("Category            : %s", catName));
        System.out.println(String.format("Disengagement Rate  : %.2f per 100 km (Threshold: %.2f)", disengagementRate, threshold));
        System.out.println(String.format("Raw ASI Score       : %.2f / 100.00", asiScore));
        System.out.println(String.format("Quarantine Status   : %s", (isQuarantined ? "SAFETY_QUARANTINE ENFORCED" : "CLEAR")));
        System.out.println(String.format("Assigned Grade      : %s", finalGrade));
        System.out.println("--------------------------");
    }

    // =========================================================================
    // AUTOMATED TEST SUITE FOR ASI CALCULATION
    // =========================================================================

    /**
     * Executes automated verification tests for the ASI Engine.
     */
    public boolean runAutomatedASITests() {
        System.out.println("\n========================================================");
        System.out.println("      AUTOMATED ASI CALCULATION ENGINE TESTS            ");
        System.out.println("========================================================");

        // Test 1: Category 1 (Urban Robotaxi) Formula
        System.out.println("[TEST 1] Urban Robotaxi Formula Check (Sensor=95, DisengagementScore=99.33, Rider=98):");
        double asi1 = ASIEngine.calculateUrbanRobotaxiASI(95.0, 99.33, 98.0);
        // (95 * 0.5) + (99.33 * 0.3) + (98 * 0.2) = 47.5 + 29.80 + 19.6 = 96.90
        boolean test1Pass = (Math.abs(asi1 - 96.90) <= 0.05);
        String grade1 = ASIEngine.determineSafetyGrade(asi1, false);
        System.out.println("Calculated ASI: " + asi1 + " | Assigned Grade: " + grade1);
        System.out.println("Result: " + (test1Pass && ASIEngine.GRADE_PLATINUM.equals(grade1) ? "PASS" : "FAIL") + "\n");

        // Test 2: Category 2 (Highway Freight Truck) Formula
        System.out.println("[TEST 2] Highway Freight Truck Formula Check (Sensor=90, LaneDiscipline=99.5, Braking=85):");
        double asi2 = ASIEngine.calculateHighwayFreightASI(90.0, 99.50, 85.0);
        // (90 * 0.4) + (99.5 * 0.4) + (85 * 0.2) = 36.0 + 39.8 + 17.0 = 92.80
        boolean test2Pass = (Math.abs(asi2 - 92.80) <= 0.05);
        String grade2 = ASIEngine.determineSafetyGrade(asi2, false);
        System.out.println("Calculated ASI: " + asi2 + " | Assigned Grade: " + grade2);
        System.out.println("Result: " + (test2Pass && ASIEngine.GRADE_PLATINUM.equals(grade2) ? "PASS" : "FAIL") + "\n");

        // Test 3: Category 3 (Last-Mile Delivery Rover) Formula
        System.out.println("[TEST 3] Delivery Rover Formula Check (Obstacle=80, Pedestrian=70, Speed=90):");
        double asi3 = ASIEngine.calculateLastMileRoverASI(80.0, 70.0, 90.0);
        // (80 * 0.6) + (70 * 0.3) + (90 * 0.1) = 48.0 + 21.0 + 9.0 = 78.00
        boolean test3Pass = (Math.abs(asi3 - 78.00) <= 0.05);
        String grade3 = ASIEngine.determineSafetyGrade(asi3, false);
        System.out.println("Calculated ASI: " + asi3 + " | Assigned Grade: " + grade3);
        System.out.println("Result: " + (test3Pass && ASIEngine.GRADE_GOLD.equals(grade3) ? "PASS" : "FAIL") + "\n");

        // Test 4: Disengagement Filtering & Quarantine Override Rule
        System.out.println("[TEST 4] Quarantine Override Rule (High raw ASI = 95.0, but Disengagement Rate = 2.50 > Threshold 2.00):");
        double disengagementRate = 2.50; // Exceeds threshold of 2.00 for Robotaxi
        boolean isQuarantined = (disengagementRate > ASIEngine.THRESHOLD_URBAN_ROBOTAXI);
        String gradeWithQuarantine = ASIEngine.determineSafetyGrade(95.0, isQuarantined);
        boolean test4Pass = isQuarantined && ASIEngine.GRADE_GROUNDED.equals(gradeWithQuarantine);
        System.out.println("Quarantine Triggered: " + isQuarantined);
        System.out.println("Normal Grade without Quarantine: " + ASIEngine.GRADE_PLATINUM);
        System.out.println("Effective Grade with Quarantine Override: " + gradeWithQuarantine);
        System.out.println("Result: " + (test4Pass ? "PASS (Quarantine successfully overrode PLATINUM to GROUNDED)" : "FAIL") + "\n");

        System.out.println("========================================================");
        System.out.println("      ASI ENGINE TEST SUITE COMPLETED                   ");
        System.out.println("========================================================\n");

        return test1Pass && test2Pass && test3Pass && test4Pass;
    }
}
