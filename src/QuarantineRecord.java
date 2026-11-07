import java.sql.Timestamp;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: QuarantineRecord.java (Entity Model)
 * 
 * Description:
 * Encapsulates formal safety compliance quarantine actions for autonomous vehicles
 * barred from fleet deployment due to safety infractions or hardware faults.
 * Maps directly to the 'quarantine_records' table in MySQL.
 * =========================================================================================
 */
public class QuarantineRecord {

    public static final String STATUS_ACTIVE = "ACTIVE_QUARANTINE";
    public static final String STATUS_CLEARED = "CLEARED_AFTER_MAINTENANCE";

    private int quarantineId;
    private int vehicleId;
    private String vin;
    private String category;
    private String reason;
    private double disengagementRate;
    private double categoryThreshold;
    private Timestamp quarantineDate;
    private String status;

    public QuarantineRecord() {
        this.status = STATUS_ACTIVE;
    }

    public QuarantineRecord(int vehicleId, String vin, String category, String reason,
                            double disengagementRate, double categoryThreshold) {
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.category = category;
        this.reason = reason;
        this.disengagementRate = disengagementRate;
        this.categoryThreshold = categoryThreshold;
        this.status = STATUS_ACTIVE;
    }

    public QuarantineRecord(int quarantineId, int vehicleId, String vin, String category,
                            String reason, double disengagementRate, double categoryThreshold,
                            Timestamp quarantineDate, String status) {
        this.quarantineId = quarantineId;
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.category = category;
        this.reason = reason;
        this.disengagementRate = disengagementRate;
        this.categoryThreshold = categoryThreshold;
        this.quarantineDate = quarantineDate;
        this.status = status;
    }

    // Getters and Setters
    public int getQuarantineId() { return quarantineId; }
    public void setQuarantineId(int quarantineId) { this.quarantineId = quarantineId; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public double getDisengagementRate() { return disengagementRate; }
    public void setDisengagementRate(double disengagementRate) { this.disengagementRate = disengagementRate; }

    public double getCategoryThreshold() { return categoryThreshold; }
    public void setCategoryThreshold(double categoryThreshold) { this.categoryThreshold = categoryThreshold; }

    public Timestamp getQuarantineDate() { return quarantineDate; }
    public void setQuarantineDate(Timestamp quarantineDate) { this.quarantineDate = quarantineDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /**
     * Formats official Vehicle Safety Quarantine Notice.
     */
    public String toQuarantineNoticeString() {
        return String.format(
            "================================================================================\n" +
            "                  OFFICIAL NOTICE OF VEHICLE SAFETY QUARANTINE                  \n" +
            "             AUTONOMOUS VEHICLE DISPATCH BARRED - LICENSE SUSPENDED             \n" +
            "================================================================================\n" +
            "RECORD ID           : QR-%05d\n" +
            "VEHICLE IDENTIFIER  : AV%03d\n" +
            "VIN                 : %s\n" +
            "OPERATING CATEGORY  : %s\n" +
            "TIMESTAMP OF ACTION : %s\n" +
            "CURRENT STATUS      : %s\n" +
            "--------------------------------------------------------------------------------\n" +
            "VIOLATION SUMMARY:\n" +
            "  Trigger Reason     : %s\n" +
            (disengagementRate > 0 ? String.format("  Disengagement Rate : %.2f per 100 km (Category Limit: %.2f per 100 km)\n", disengagementRate, categoryThreshold) : "") +
            "--------------------------------------------------------------------------------\n" +
            "MANDATORY AUDIT ACTION:\n" +
            "  1. Vehicle is IMMEDIATELY REVOKED from commercial autonomous dispatch.\n" +
            "  2. Physical and algorithmic sensor recalibration required.\n" +
            "  3. Recalibration and maintenance must be certified by an authorized safety auditor.\n" +
            "================================================================================",
            quarantineId,
            vehicleId,
            (vin != null ? vin : "N/A"),
            (category != null ? category : "N/A"),
            (quarantineDate != null ? quarantineDate.toString() : "Current Simulation Session"),
            status,
            reason
        );
    }
}
