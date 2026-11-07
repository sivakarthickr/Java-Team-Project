import java.sql.Timestamp;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: PreTripInspection.java (Entity Model)
 * 
 * Description:
 * Encapsulates the results of a Phase 1 Pre-Trip Safety & Calibration Inspection.
 * Records verification of LiDAR, Radar, Camera, and Battery level before autonomous dispatch.
 * =========================================================================================
 */
public class PreTripInspection {

    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_FAILED = "FAILED";

    private int inspectionId;
    private int vehicleId;
    private String vin;
    private String lidarStatus;
    private String radarStatus;
    private String cameraStatus;
    private double batteryLevel;
    private String inspectionStatus;
    private String failureReason;
    private Timestamp inspectionDate;

    public PreTripInspection() {}

    public PreTripInspection(int vehicleId, String vin, String lidarStatus, String radarStatus,
                             String cameraStatus, double batteryLevel, String inspectionStatus, String failureReason) {
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.lidarStatus = lidarStatus;
        this.radarStatus = radarStatus;
        this.cameraStatus = cameraStatus;
        this.batteryLevel = batteryLevel;
        this.inspectionStatus = inspectionStatus;
        this.failureReason = failureReason;
    }

    public int getInspectionId() { return inspectionId; }
    public void setInspectionId(int inspectionId) { this.inspectionId = inspectionId; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getLidarStatus() { return lidarStatus; }
    public void setLidarStatus(String lidarStatus) { this.lidarStatus = lidarStatus; }

    public String getRadarStatus() { return radarStatus; }
    public void setRadarStatus(String radarStatus) { this.radarStatus = radarStatus; }

    public String getCameraStatus() { return cameraStatus; }
    public void setCameraStatus(String cameraStatus) { this.cameraStatus = cameraStatus; }

    public double getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(double batteryLevel) { this.batteryLevel = batteryLevel; }

    public String getInspectionStatus() { return inspectionStatus; }
    public void setInspectionStatus(String inspectionStatus) { this.inspectionStatus = inspectionStatus; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Timestamp getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(Timestamp inspectionDate) { this.inspectionDate = inspectionDate; }

    /**
     * Formats the inspection result matching the academic specification.
     */
    public String toReportString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================\n");
        sb.append("              PRE-TRIP INSPECTION REPORT                \n");
        sb.append("========================================================\n");
        sb.append(String.format("Vehicle ID: AV%03d\n", vehicleId));
        sb.append(String.format("VIN: %s\n", vin));
        sb.append(String.format("LiDAR: %s\n", lidarStatus));
        sb.append(String.format("Radar: %s\n", radarStatus));
        sb.append(String.format("Camera: %s\n", cameraStatus));
        sb.append(String.format("Battery: %.1f%%\n\n", batteryLevel));
        sb.append(String.format("Inspection Status: %s\n", inspectionStatus));
        if (STATUS_FAILED.equalsIgnoreCase(inspectionStatus) && failureReason != null && !failureReason.isEmpty()) {
            sb.append(String.format("Failure Reason: %s\n", failureReason));
        }
        sb.append("========================================================");
        return sb.toString();
    }
}
