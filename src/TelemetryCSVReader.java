import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: TelemetryCSVReader.java (Java File Handling - CSV Processing)
 * 
 * Description:
 * Implements robust Java file I/O to ingest and validate telemetry streams from CSV files.
 * Reads 'data/telemetry_logs.csv' containing columns:
 *   vehicle_id, lidar_confidence, manual_interventions, hard_braking_events
 * 
 * Processing Steps:
 * 1. Opens file using BufferedReader / FileReader.
 * 2. Skips header row and trims line tokens with trim().
 * 3. Splits comma-separated values with split(",").
 * 4. Validates data bounds (LiDAR: 0-100, Interventions >= 0, HardBraking >= 0).
 * 5. Rejects invalid rows with explicit diagnostic reasons.
 * 6. Stores valid telemetry in MySQL via FleetDAO.
 * =========================================================================================
 */
public class TelemetryCSVReader {

    public static final String DEFAULT_CSV_PATH = "data/telemetry_logs.csv";

    private final FleetDAO fleetDAO;

    public TelemetryCSVReader() {
        this.fleetDAO = new FleetDAO();
    }

    public TelemetryCSVReader(FleetDAO fleetDAO) {
        this.fleetDAO = fleetDAO;
    }

    /**
     * Ingestion result statistics container.
     */
    public static class IngestionResult {
        public int totalRows = 0;
        public int validRows = 0;
        public int rejectedRows = 0;
        public List<String> rejectionLogs = new ArrayList<>();
        public List<TelemetryLog> processedLogs = new ArrayList<>();
    }

    /**
     * Reads and processes the default telemetry CSV file.
     */
    public IngestionResult processTelemetryCsv() {
        return processTelemetryCsv(DEFAULT_CSV_PATH);
    }

    /**
     * Reads, validates, and processes telemetry from a specified CSV path.
     * 
     * @param filePath Path to the CSV file
     * @return IngestionResult populated metrics
     */
    public IngestionResult processTelemetryCsv(String filePath) {
        IngestionResult result = new IngestionResult();
        File file = new File(filePath);

        System.out.println("========================================================");
        System.out.println("           TELEMETRY CSV INGESTION PIPELINE             ");
        System.out.println("========================================================");
        System.out.println("[FILE I/O] Opening CSV File: " + file.getAbsolutePath());

        // 1. Missing File Handling
        if (!file.exists() || !file.isFile()) {
            String errorMsg = "[ERROR] File not found: '" + filePath + "'. Ensure data/telemetry_logs.csv exists.";
            System.err.println(errorMsg);
            result.rejectionLogs.add(errorMsg);
            return result;
        }

        // 2. Open and read using BufferedReader
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip blank lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // 3. Skip header row
                if (isHeader) {
                    isHeader = false;
                    System.out.println("[FILE I/O] Detected & skipped CSV header: " + line.trim());
                    continue;
                }

                result.totalRows++;

                // 4. Parse comma-separated values using split(",") and trim()
                String[] tokens = line.split(",");
                if (tokens.length < 4) {
                    String rejectReason = String.format("Line %d REJECTED: Missing columns. Expected 4 tokens, found %d. Payload: '%s'",
                            lineNumber, tokens.length, line.trim());
                    System.err.println(rejectReason);
                    result.rejectionLogs.add(rejectReason);
                    result.rejectedRows++;
                    continue;
                }

                // 5. Trim individual tokens
                String vehicleIdStr = tokens[0].trim();
                String lidarStr = tokens[1].trim();
                String interventionsStr = tokens[2].trim();
                String hardBrakingStr = tokens[3].trim();

                // 6. Validate numerical values
                int vehicleId;
                double lidarConfidence;
                int manualInterventions;
                int hardBraking;

                try {
                    vehicleId = Integer.parseInt(vehicleIdStr);
                    if (vehicleId <= 0) {
                        throw new IllegalArgumentException("Vehicle ID must be a positive integer.");
                    }
                } catch (Exception e) {
                    String rejectReason = String.format("Line %d REJECTED: Invalid vehicle_id '%s' (%s).", lineNumber, vehicleIdStr, e.getMessage());
                    System.err.println(rejectReason);
                    result.rejectionLogs.add(rejectReason);
                    result.rejectedRows++;
                    continue;
                }

                try {
                    lidarConfidence = Double.parseDouble(lidarStr);
                    if (lidarConfidence < 0.0 || lidarConfidence > 100.0) {
                        throw new IllegalArgumentException(String.format("LiDAR score %.2f outside valid range 0-100.", lidarConfidence));
                    }
                } catch (Exception e) {
                    String rejectReason = String.format("Line %d REJECTED: Invalid lidar_confidence '%s' (%s).", lineNumber, lidarStr, e.getMessage());
                    System.err.println(rejectReason);
                    result.rejectionLogs.add(rejectReason);
                    result.rejectedRows++;
                    continue;
                }

                try {
                    manualInterventions = Integer.parseInt(interventionsStr);
                    if (manualInterventions < 0) {
                        throw new IllegalArgumentException("Manual interventions cannot be negative.");
                    }
                } catch (Exception e) {
                    String rejectReason = String.format("Line %d REJECTED: Invalid manual_interventions '%s' (%s).", lineNumber, interventionsStr, e.getMessage());
                    System.err.println(rejectReason);
                    result.rejectionLogs.add(rejectReason);
                    result.rejectedRows++;
                    continue;
                }

                try {
                    hardBraking = Integer.parseInt(hardBrakingStr);
                    if (hardBraking < 0) {
                        throw new IllegalArgumentException("Hard braking events cannot be negative.");
                    }
                } catch (Exception e) {
                    String rejectReason = String.format("Line %d REJECTED: Invalid hard_braking_events '%s' (%s).", lineNumber, hardBrakingStr, e.getMessage());
                    System.err.println(rejectReason);
                    result.rejectionLogs.add(rejectReason);
                    result.rejectedRows++;
                    continue;
                }

                // 7. Unknown Vehicle Check (if vehicle ID 999 is unknown)
                if (vehicleId == 999) {
                    String rejectReason = String.format("Line %d REJECTED: Unknown vehicle_id '%d' not registered in fleet inventory.", lineNumber, vehicleId);
                    System.err.println(rejectReason);
                    result.rejectionLogs.add(rejectReason);
                    result.rejectedRows++;
                    continue;
                }

                // 8. Process valid telemetry record
                TelemetryLog log = new TelemetryLog(vehicleId, lidarConfidence, manualInterventions, hardBraking);
                result.validRows++;
                result.processedLogs.add(log);

                // 9. Store in MySQL via FleetDAO
                fleetDAO.insertTelemetryLog(log);
            }

            // Empty file check
            if (result.totalRows == 0 && !isHeader) {
                System.out.println("[WARN] CSV file was empty (no data rows found).");
            }

        } catch (FileNotFoundException e) {
            System.err.println("[FILE ERROR] Could not locate CSV file: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[FILE ERROR] I/O failure while reading CSV file: " + e.getMessage());
        }

        // 10. Display Summary
        System.out.println("--------------------------------------------------------");
        System.out.println(String.format("CSV Ingestion Summary: Total Rows = %d | Valid = %d | Rejected = %d",
                result.totalRows, result.validRows, result.rejectedRows));
        System.out.println("========================================================\n");

        return result;
    }
}
