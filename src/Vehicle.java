import java.sql.Timestamp;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: Vehicle.java (Entity Model)
 * 
 * Description:
 * Encapsulates the state and properties of an Autonomous Vehicle within the fleet.
 * Maps directly to the 'vehicles' table in the MySQL database.
 * =========================================================================================
 */
public class Vehicle {

    // Valid Vehicle Categories
    public static final String CATEGORY_URBAN_ROBOTAXI = "URBAN_ROBOTAXI";
    public static final String CATEGORY_HIGHWAY_FREIGHT_TRUCK = "HIGHWAY_FREIGHT_TRUCK";
    public static final String CATEGORY_LAST_MILE_DELIVERY_ROVER = "LAST_MILE_DELIVERY_ROVER";

    // Valid Fleet Statuses
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SAFETY_QUARANTINE = "SAFETY_QUARANTINE";
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";
    public static final String STATUS_RETIRED = "RETIRED";

    private int vehicleId;
    private String vin;
    private String vehicleType;
    private String lidarStatus;
    private String radarStatus;
    private String cameraStatus;
    private double batteryLevel;
    private Double safetyScore;
    private String safetyGrade;
    private String fleetStatus;
    private Timestamp createdAt;

    // Default constructor
    public Vehicle() {
        this.lidarStatus = "CALIBRATED";
        this.radarStatus = "CALIBRATED";
        this.cameraStatus = "CALIBRATED";
        this.batteryLevel = 100.00;
        this.fleetStatus = STATUS_ACTIVE;
    }

    // Constructor for registration
    public Vehicle(String vin, String vehicleType, String lidarStatus, String radarStatus,
                   String cameraStatus, double batteryLevel) {
        this.vin = vin;
        this.vehicleType = vehicleType;
        this.lidarStatus = lidarStatus;
        this.radarStatus = radarStatus;
        this.cameraStatus = cameraStatus;
        this.batteryLevel = batteryLevel;
        this.fleetStatus = STATUS_ACTIVE;
    }

    // Constructor with vehicle ID
    public Vehicle(int vehicleId, String vin, String vehicleType, String lidarStatus, String radarStatus,
                   String cameraStatus, double batteryLevel) {
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.vehicleType = vehicleType;
        this.lidarStatus = lidarStatus;
        this.radarStatus = radarStatus;
        this.cameraStatus = cameraStatus;
        this.batteryLevel = batteryLevel;
        this.fleetStatus = STATUS_ACTIVE;
    }

    // Full constructor
    public Vehicle(int vehicleId, String vin, String vehicleType, String lidarStatus,
                   String radarStatus, String cameraStatus, double batteryLevel,
                   Double safetyScore, String safetyGrade, String fleetStatus, Timestamp createdAt) {
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.vehicleType = vehicleType;
        this.lidarStatus = lidarStatus;
        this.radarStatus = radarStatus;
        this.cameraStatus = cameraStatus;
        this.batteryLevel = batteryLevel;
        this.safetyScore = safetyScore;
        this.safetyGrade = safetyGrade;
        this.fleetStatus = fleetStatus;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getLidarStatus() { return lidarStatus; }
    public void setLidarStatus(String lidarStatus) { this.lidarStatus = lidarStatus; }

    public String getRadarStatus() { return radarStatus; }
    public void setRadarStatus(String radarStatus) { this.radarStatus = radarStatus; }

    public String getCameraStatus() { return cameraStatus; }
    public void setCameraStatus(String cameraStatus) { this.cameraStatus = cameraStatus; }

    public double getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(double batteryLevel) { this.batteryLevel = batteryLevel; }

    public Double getSafetyScore() { return safetyScore; }
    public void setSafetyScore(Double safetyScore) { this.safetyScore = safetyScore; }

    public String getSafetyGrade() { return safetyGrade; }
    public void setSafetyGrade(String safetyGrade) { this.safetyGrade = safetyGrade; }

    public String getFleetStatus() { return fleetStatus; }
    public void setFleetStatus(String fleetStatus) { this.fleetStatus = fleetStatus; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /**
     * Formatted string card for vehicle display.
     */
    public String toDisplayCard() {
        return String.format(
            "+-------------------------------------------------------------------------+\n" +
            "| VEHICLE REGISTRATION CARD                                               |\n" +
            "+-------------------------------------------------------------------------+\n" +
            "  Vehicle ID      : %-52d\n" +
            "  VIN             : %-52s\n" +
            "  Category        : %-52s\n" +
            "  Battery/Fuel    : %-51.2f%%\n" +
            "  LiDAR Status    : %-52s\n" +
            "  Radar Status    : %-52s\n" +
            "  Camera Status   : %-52s\n" +
            "  Safety Grade    : %-52s\n" +
            "  Fleet Status    : %-52s\n" +
            "  Registration Date: %-51s\n" +
            "+-------------------------------------------------------------------------+",
            vehicleId,
            vin,
            vehicleType,
            batteryLevel,
            lidarStatus,
            radarStatus,
            cameraStatus,
            (safetyGrade != null ? safetyGrade : "UNGRADED (Awaiting Telemetry)"),
            fleetStatus,
            (createdAt != null ? createdAt.toString() : "N/A")
        );
    }
}
