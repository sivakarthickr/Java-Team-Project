import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: ASIEngine.java (Autonomous Safety Index Calculation Engine)
 * 
 * Description:
 * Computes category-specific Autonomous Safety Index (ASI) scores, enforces safety
 * quarantine thresholds, and assigns fleet compliance bands.
 * 
 * Category Weights & Thresholds:
 * 1. URBAN ROBOTAXI:
 *    - Sensor Weight: 50% | Disengagement Weight: 30% | Rider Safety Rating: 20%
 *    - Max Disengagement Threshold: 2.0 per 100 km
 * 
 * 2. HIGHWAY FREIGHT TRUCK:
 *    - Sensor Weight: 40% | Lane Discipline: 40% | Braking Response: 20%
 *    - Max Disengagement Threshold: 1.0 per 100 km
 * 
 * 3. LAST-MILE DELIVERY ROVER:
 *    - Obstacle Detection: 60% | Pedestrian Avoidance: 30% | Speed Adherence: 10%
 *    - Max Disengagement Threshold: 4.0 per 100 km
 * 
 * Safety Bands:
 * - 90 - 100 : PLATINUM_SAFETY
 * - 75 - 89  : GOLD_FLEET
 * - 60 - 74  : RECALIBRATION_REQUIRED
 * - Below 60 : GROUNDED
 * 
 * Disengagement Filtering Rule:
 * If Disengagement Rate > Category Threshold:
 *   -> Enforce Fleet Status = SAFETY_QUARANTINE
 *   -> Quarantine OVERRIDES normal grade, forcing assigned grade = GROUNDED
 * =========================================================================================
 */
public class ASIEngine {

    // Grading Band Constants
    public static final String GRADE_PLATINUM = "PLATINUM_SAFETY";
    public static final String GRADE_GOLD = "GOLD_FLEET";
    public static final String GRADE_RECALIBRATION = "RECALIBRATION_REQUIRED";
    public static final String GRADE_GROUNDED = "GROUNDED";

    // Category Threshold Constants
    public static final double THRESHOLD_URBAN_ROBOTAXI = 2.00;
    public static final double THRESHOLD_HIGHWAY_FREIGHT = 1.00;
    public static final double THRESHOLD_DELIVERY_ROVER = 4.00;

    private final FleetDAO fleetDAO;

    public ASIEngine() {
        this.fleetDAO = new FleetDAO();
    }

    public ASIEngine(FleetDAO fleetDAO) {
        this.fleetDAO = fleetDAO;
    }

    /**
     * Returns the maximum allowable disengagements per 100 km for a vehicle category.
     */
    public static double getCategoryThreshold(String category) {
        if (category == null) return THRESHOLD_URBAN_ROBOTAXI;
        switch (category.toUpperCase()) {
            case "HIGHWAY_FREIGHT_TRUCK":
                return THRESHOLD_HIGHWAY_FREIGHT;
            case "LAST_MILE_DELIVERY_ROVER":
                return THRESHOLD_DELIVERY_ROVER;
            case "URBAN_ROBOTAXI":
            default:
                return THRESHOLD_URBAN_ROBOTAXI;
        }
    }

    /**
     * Calculates the Disengagement Score (0 - 100) based on disengagement penalty formula:
     * Disengagement Score = max(0, 100 - DisengagementRate)
     * Documented Formula from Assignment Specification:
     * ASI contribution = (100 - (Disengagements / Distance) * 100) * W_d
     */
    public static double calculateDisengagementScore(double disengagementRate) {
        double score = 100.0 - disengagementRate;
        if (score < 0.0) return 0.0;
        if (score > 100.0) return 100.0;
        return score;
    }

    // =========================================================================
    // SEPARATE CONFIGURABLE ASI FORMULA METHODS
    // =========================================================================

    /**
     * Category 1: Urban Robotaxi ASI Formula
     * ASI = (SensorScore * 0.50) + (DisengagementScore * 0.30) + (RiderRating * 0.20)
     */
    public static double calculateUrbanRobotaxiASI(double sensorScore, double disengagementScore, double riderSafetyRating) {
        double asi = (sensorScore * 0.50) + (disengagementScore * 0.30) + (riderSafetyRating * 0.20);
        return Math.round(asi * 100.0) / 100.0;
    }

    /**
     * Category 2: Highway Freight Truck ASI Formula
     * ASI = (SensorScore * 0.40) + (LaneDisciplineScore * 0.40) + (BrakingResponseScore * 0.20)
     * 
     * NOTE ON ASSUMED METRICS:
     * In simulation, Lane Discipline maps to Disengagement Compliance Score,
     * and Braking Response maps to Arrival Cargo Condition / Hard-Braking penalty.
     */
    public static double calculateHighwayFreightASI(double sensorScore, double laneDisciplineScore, double brakingResponseScore) {
        double asi = (sensorScore * 0.40) + (laneDisciplineScore * 0.40) + (brakingResponseScore * 0.20);
        return Math.round(asi * 100.0) / 100.0;
    }

    /**
     * Category 3: Last-Mile Delivery Rover ASI Formula
     * ASI = (ObstacleDetectionScore * 0.60) + (PedestrianAvoidanceScore * 0.30) + (SpeedAdherenceScore * 0.10)
     * 
     * NOTE ON ASSUMED METRICS:
     * In simulation, Obstacle Detection maps to LiDAR Sensor confidence,
     * Pedestrian Avoidance maps to Disengagement Compliance Score,
     * and Speed Adherence maps to Speeding Infraction penalty.
     */
    public static double calculateLastMileRoverASI(double obstacleScore, double pedestrianAvoidanceScore, double speedAdherenceScore) {
        double asi = (obstacleScore * 0.60) + (pedestrianAvoidanceScore * 0.30) + (speedAdherenceScore * 0.10);
        return Math.round(asi * 100.0) / 100.0;
    }

    /**
     * Determines the assigned safety band based on raw score and quarantine override.
     * 
     * @param asiScore Computed ASI Score
     * @param isQuarantined true if vehicle disengagement rate exceeded threshold
     * @return Safety band name
     */
    public static String determineSafetyGrade(double asiScore, boolean isQuarantined) {
        // Quarantine override rule: Exceeding disengagement threshold forces GROUNDED
        if (isQuarantined) {
            return GRADE_GROUNDED;
        }

        if (asiScore >= 90.0) {
            return GRADE_PLATINUM;
        } else if (asiScore >= 75.0) {
            return GRADE_GOLD;
        } else if (asiScore >= 60.0) {
            return GRADE_RECALIBRATION;
        } else {
            return GRADE_GROUNDED;
        }
    }

    /**
     * Evaluates a registered vehicle by retrieving live trip and telemetry data from MySQL.
     * 
     * @param vehicleId Target Vehicle ID
     * @return ASIEvaluationResult populated object or null if vehicle not found
     */
    public ASIEvaluationResult evaluateVehicle(int vehicleId) {
        Vehicle vehicle = fleetDAO.getVehicleById(vehicleId);
        if (vehicle == null) return null;

        String category = vehicle.getVehicleType() != null ? vehicle.getVehicleType() : Vehicle.CATEGORY_URBAN_ROBOTAXI;
        double threshold = getCategoryThreshold(category);

        // 1. Gather Telemetry Sensor Score (LiDAR confidence)
        List<TelemetryLog> telemetryLogs = fleetDAO.getTelemetryLogsByVehicleId(vehicleId);
        double sensorScore = 95.0; // Default nominal
        int totalHardBrakes = 0;
        int totalInterventions = 0;

        if (!telemetryLogs.isEmpty()) {
            double totalConf = 0.0;
            for (TelemetryLog log : telemetryLogs) {
                totalConf += log.getLidarConfidenceScore();
                totalHardBrakes += log.getHardBrakingEvents();
                totalInterventions += log.getManualInterventions();
            }
            sensorScore = totalConf / telemetryLogs.size();
        }

        // 2. Gather Trip Metrics (Distance, Speeding, Arrival Condition)
        List<Trip> trips = fleetDAO.getTripsByVehicleId(vehicleId);
        double totalDistance = 0.0;
        int totalSpeeding = 0;
        double totalArrival = 0.0;
        int tripInterventions = 0;

        for (Trip t : trips) {
            totalDistance += t.getTripDistance();
            totalSpeeding += t.getSpeedingViolations();
            totalArrival += t.getArrivalCondition();
            tripInterventions += t.getDisengagements();
        }

        int finalInterventions = Math.max(totalInterventions, tripInterventions);
        double avgArrival = trips.isEmpty() ? 95.0 : (totalArrival / trips.size());
        double disengagementRate = totalDistance > 0 ? ((finalInterventions / totalDistance) * 100.0) : 0.0;
        disengagementRate = Math.round(disengagementRate * 100.0) / 100.0;

        // Disengagement penalty score
        double disengagementScore = calculateDisengagementScore(disengagementRate);

        // 3. Category Specific Metric Calculations
        ASIEvaluationResult result = new ASIEvaluationResult();
        result.setVehicleId(vehicleId);
        result.setVin(vehicle.getVin());
        result.setCategory(category);
        result.setDisengagementRate(disengagementRate);
        result.setCategoryThreshold(threshold);

        double rawAsi;
        if (Vehicle.CATEGORY_HIGHWAY_FREIGHT_TRUCK.equalsIgnoreCase(category)) {
            result.setWeight1(0.40);
            result.setWeight2(0.40);
            result.setWeight3(0.20);

            result.setScore1(sensorScore);
            result.setScore2(disengagementScore); // Lane discipline compliance
            double brakingScore = Math.max(0.0, avgArrival - (totalHardBrakes * 2.0));
            result.setScore3(brakingScore);

            rawAsi = calculateHighwayFreightASI(result.getScore1(), result.getScore2(), result.getScore3());

        } else if (Vehicle.CATEGORY_LAST_MILE_DELIVERY_ROVER.equalsIgnoreCase(category)) {
            result.setWeight1(0.60);
            result.setWeight2(0.30);
            result.setWeight3(0.10);

            result.setScore1(sensorScore); // Obstacle Detection
            result.setScore2(disengagementScore); // Pedestrian Avoidance
            double speedAdherence = Math.max(0.0, 100.0 - (totalSpeeding * 10.0));
            result.setScore3(speedAdherence);

            rawAsi = calculateLastMileRoverASI(result.getScore1(), result.getScore2(), result.getScore3());

        } else { // URBAN_ROBOTAXI
            result.setWeight1(0.50);
            result.setWeight2(0.30);
            result.setWeight3(0.20);

            result.setScore1(sensorScore);
            result.setScore2(disengagementScore);
            result.setScore3(avgArrival); // Rider Safety / Arrival Rating

            rawAsi = calculateUrbanRobotaxiASI(result.getScore1(), result.getScore2(), result.getScore3());
        }

        result.setRawAsiScore(rawAsi);

        // 4. Disengagement Threshold & Quarantine Enforcement
        boolean quarantine = (disengagementRate > threshold);
        result.setQuarantineTriggered(quarantine);

        if (quarantine) {
            String reason = String.format("Exceeded %s disengagement threshold: %.2f > %.2f per 100km",
                    category, disengagementRate, threshold);
            result.setQuarantineReason(reason);
            result.setFinalFleetStatus(Vehicle.STATUS_SAFETY_QUARANTINE);
            result.setAssignedGrade(GRADE_GROUNDED); // Quarantine override!
        } else {
            result.setFinalFleetStatus(Vehicle.STATUS_ACTIVE);
            result.setAssignedGrade(determineSafetyGrade(rawAsi, false));
        }

        // 5. Update Vehicle Record in MySQL
        persistEvaluationToDatabase(result);

        return result;
    }

    /**
     * Persists safety evaluation and quarantine records in MySQL.
     */
    private void persistEvaluationToDatabase(ASIEvaluationResult result) {
        String updateSql = "UPDATE vehicles SET safety_score = ?, safety_grade = ?, fleet_status = ? WHERE vehicle_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setDouble(1, result.getRawAsiScore());
            ps.setString(2, result.getAssignedGrade());
            ps.setString(3, result.getFinalFleetStatus());
            ps.setInt(4, result.getVehicleId());
            ps.executeUpdate();

            // If quarantined, log in quarantine_records
            if (result.isQuarantineTriggered()) {
                String quarantineSql = "INSERT INTO quarantine_records (vehicle_id, reason, status) " +
                                       "VALUES (?, ?, 'ACTIVE_QUARANTINE')";
                try (PreparedStatement psQuarantine = conn.prepareStatement(quarantineSql)) {
                    psQuarantine.setInt(1, result.getVehicleId());
                    psQuarantine.setString(2, result.getQuarantineReason());
                    psQuarantine.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("[ASIEngine DB NOTICE] Failed to persist safety evaluation to MySQL: " + e.getMessage());
        }
    }
}
