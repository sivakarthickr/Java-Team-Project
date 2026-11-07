import java.sql.Timestamp;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: TelemetryLog.java (Entity Model)
 * 
 * Description:
 * Encapsulates in-trip telemetry data points collected during autonomous operations:
 * 1. LiDAR Point-Cloud Confidence Score (0–100 or normalized 0.00–1.00)
 * 2. Manual Safety Driver Disengagements / Interventions (non-negative integer)
 * 3. Hard-Braking Events (non-negative integer)
 * 4. Timestamp information
 * =========================================================================================
 */
public class TelemetryLog {

    private int logId;
    private int vehicleId;
    private String vin;
    private double lidarConfidence; // Stored as normalized decimal 0.0000 - 1.0000
    private int manualInterventions;
    private int hardBrakingEvents;
    private Timestamp logTimestamp;

    public TelemetryLog() {}

    public TelemetryLog(int vehicleId, double lidarConfidence, int manualInterventions, int hardBrakingEvents) {
        this.vehicleId = vehicleId;
        // Normalize 0-100 score to 0.0000 - 1.0000 if needed
        this.lidarConfidence = (lidarConfidence > 1.0) ? (lidarConfidence / 100.0) : lidarConfidence;
        this.manualInterventions = manualInterventions;
        this.hardBrakingEvents = hardBrakingEvents;
    }

    public TelemetryLog(int logId, int vehicleId, String vin, double lidarConfidence, 
                        int manualInterventions, int hardBrakingEvents, Timestamp logTimestamp) {
        this.logId = logId;
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.lidarConfidence = (lidarConfidence > 1.0) ? (lidarConfidence / 100.0) : lidarConfidence;
        this.manualInterventions = manualInterventions;
        this.hardBrakingEvents = hardBrakingEvents;
        this.logTimestamp = logTimestamp;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public double getLidarConfidence() { return lidarConfidence; }
    public void setLidarConfidence(double lidarConfidence) { 
        this.lidarConfidence = (lidarConfidence > 1.0) ? (lidarConfidence / 100.0) : lidarConfidence; 
    }

    public int getLidarConfidenceScore() {
        return (int) Math.round(this.lidarConfidence * 100.0);
    }

    public int getManualInterventions() { return manualInterventions; }
    public void setManualInterventions(int manualInterventions) { this.manualInterventions = manualInterventions; }

    public int getHardBrakingEvents() { return hardBrakingEvents; }
    public void setHardBrakingEvents(int hardBrakingEvents) { this.hardBrakingEvents = hardBrakingEvents; }

    public Timestamp getLogTimestamp() { return logTimestamp; }
    public void setLogTimestamp(Timestamp logTimestamp) { this.logTimestamp = logTimestamp; }

    /**
     * Formats the telemetry summary using String.format() matching academic specifications.
     */
    public String toSummaryString() {
        return String.format(
            "========================================================\n" +
            "                 IN-TRIP TELEMETRY SUMMARY              \n" +
            "========================================================\n" +
            "VEHICLE: AV%03d%s\n" +
            "LiDAR CONFIDENCE: %d\n" +
            "MANUAL INTERVENTIONS: %d\n" +
            "HARD BRAKING EVENTS: %d\n" +
            (logTimestamp != null ? "TIMESTAMP: " + logTimestamp.toString() + "\n" : "") +
            "\nSTATUS: TELEMETRY RECORDED\n" +
            "========================================================",
            vehicleId,
            (vin != null && !vin.isEmpty() ? " (" + vin + ")" : ""),
            getLidarConfidenceScore(),
            manualInterventions,
            hardBrakingEvents
        );
    }
}
