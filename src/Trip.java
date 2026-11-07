import java.sql.Timestamp;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: Trip.java (Entity Model)
 * 
 * Description:
 * Encapsulates the results of a Phase 3 Post-Trip Audit:
 * 1. Trip Distance (km)
 * 2. Speeding Violations count
 * 3. Passenger/Cargo Arrival Condition Score (0 - 100)
 * 4. Aggregated Manual Safety Driver Disengagements
 * 5. Calculated Disengagement Rate per 100 km
 * 6. Audit Timestamp
 * =========================================================================================
 */
public class Trip {

    private int tripId;
    private int vehicleId;
    private String vin;
    private String vehicleCategory;
    private double tripDistance;
    private int speedingViolations;
    private double arrivalCondition;
    private int disengagements;
    private double disengagementRate;
    private Timestamp tripDate;

    public Trip() {}

    public Trip(int vehicleId, double tripDistance, int speedingViolations,
                double arrivalCondition, int disengagements, double disengagementRate) {
        this.vehicleId = vehicleId;
        this.tripDistance = tripDistance;
        this.speedingViolations = speedingViolations;
        this.arrivalCondition = arrivalCondition;
        this.disengagements = disengagements;
        this.disengagementRate = disengagementRate;
    }

    public Trip(int tripId, int vehicleId, String vin, String vehicleCategory, double tripDistance,
                int speedingViolations, double arrivalCondition, int disengagements,
                double disengagementRate, Timestamp tripDate) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.vehicleCategory = vehicleCategory;
        this.tripDistance = tripDistance;
        this.speedingViolations = speedingViolations;
        this.arrivalCondition = arrivalCondition;
        this.disengagements = disengagements;
        this.disengagementRate = disengagementRate;
        this.tripDate = tripDate;
    }

    // Getters and Setters
    public int getTripId() { return tripId; }
    public void setTripId(int tripId) { this.tripId = tripId; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public double getTripDistance() { return tripDistance; }
    public void setTripDistance(double tripDistance) { this.tripDistance = tripDistance; }

    public int getSpeedingViolations() { return speedingViolations; }
    public void setSpeedingViolations(int speedingViolations) { this.speedingViolations = speedingViolations; }

    public double getArrivalCondition() { return arrivalCondition; }
    public void setArrivalCondition(double arrivalCondition) { this.arrivalCondition = arrivalCondition; }

    public int getDisengagements() { return disengagements; }
    public void setDisengagements(int disengagements) { this.disengagements = disengagements; }

    public double getDisengagementRate() { return disengagementRate; }
    public void setDisengagementRate(double disengagementRate) { this.disengagementRate = disengagementRate; }

    public Timestamp getTripDate() { return tripDate; }
    public void setTripDate(Timestamp tripDate) { this.tripDate = tripDate; }

    /**
     * Formats the post-trip audit report matching the academic specifications.
     */
    public String toAuditReportString() {
        return String.format(
            "========================================================\n" +
            "                 POST-TRIP AUDIT REPORT                 \n" +
            "========================================================\n" +
            "Vehicle ID: AV%03d%s\n" +
            "Vehicle Category: %s\n" +
            "Trip Distance: %.2f km\n" +
            "Speeding Violations: %d\n" +
            "Arrival Condition: %.1f / 100\n" +
            "Manual Interventions: %d\n" +
            "Disengagement Rate: %.2f per 100 km\n" +
            (tripDate != null ? "Trip Date: " + tripDate.toString() + "\n" : "") +
            "\nSTATUS: TRIP AUDIT RECORDED\n" +
            "========================================================",
            vehicleId,
            (vin != null && !vin.isEmpty() ? " (" + vin + ")" : ""),
            (vehicleCategory != null ? vehicleCategory : "N/A"),
            tripDistance,
            speedingViolations,
            arrivalCondition,
            disengagements,
            disengagementRate
        );
    }
}
