import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: FleetDAO.java (Data Access Object)
 * 
 * Description:
 * Encapsulates all database CRUD operations, maintaining a strict separation of concerns
 * between MySQL persistence and the core business logic.
 * =========================================================================================
 */
public class FleetDAO {

    /**
     * Checks if a vehicle with the specified VIN is already registered in the database.
     * 
     * @param vin 17-character VIN
     * @return boolean true if VIN exists, false otherwise
     */
    public boolean isVinRegistered(String vin) {
        String sql = "SELECT 1 FROM vehicles WHERE vin = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vin);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] isVinRegistered check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserts a new vehicle into the 'vehicles' table and sets the generated vehicle_id.
     * 
     * @param vehicle Vehicle model populated with attributes
     * @return boolean true if inserted successfully, false otherwise
     * @throws IllegalArgumentException if duplicate VIN is detected
     */
    public boolean insertVehicle(Vehicle vehicle) throws IllegalArgumentException {
        if (isVinRegistered(vehicle.getVin())) {
            throw new IllegalArgumentException("Registration Rejected: VIN '" + vehicle.getVin() + "' is already registered in the database.");
        }

        String sql = "INSERT INTO vehicles (vin, vehicle_type, lidar_status, radar_status, camera_status, battery_level, fleet_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, vehicle.getVin());
            ps.setString(2, vehicle.getVehicleType());
            ps.setString(3, vehicle.getLidarStatus());
            ps.setString(4, vehicle.getRadarStatus());
            ps.setString(5, vehicle.getCameraStatus());
            ps.setDouble(6, vehicle.getBatteryLevel());
            ps.setString(7, vehicle.getFleetStatus() != null ? vehicle.getFleetStatus() : Vehicle.STATUS_ACTIVE);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        vehicle.setVehicleId(generatedId);
                    }
                }
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("Duplicate Entry: A vehicle with VIN '" + vehicle.getVin() + "' already exists.");
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] insertVehicle failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves a vehicle record by its unique database vehicle_id.
     * 
     * @param vehicleId Primary key vehicle_id
     * @return Vehicle object or null if not found
     */
    public Vehicle getVehicleById(int vehicleId) {
        String sql = "SELECT * FROM vehicles WHERE vehicle_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVehicle(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getVehicleById failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a vehicle record by its unique 17-character VIN.
     * 
     * @param vin 17-character VIN
     * @return Vehicle object or null if not found
     */
    public Vehicle getVehicleByVin(String vin) {
        String sql = "SELECT * FROM vehicles WHERE vin = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVehicle(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getVehicleByVin failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all registered vehicles from the database.
     * 
     * @return List of Vehicle objects
     */
    public List<Vehicle> getAllVehicles() {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT * FROM vehicles ORDER BY vehicle_id ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToVehicle(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getAllVehicles failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Maps a current ResultSet row to a Vehicle domain object.
     */
    private Vehicle mapResultSetToVehicle(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setVehicleId(rs.getInt("vehicle_id"));
        v.setVin(rs.getString("vin"));
        v.setVehicleType(rs.getString("vehicle_type"));
        v.setLidarStatus(rs.getString("lidar_status"));
        v.setRadarStatus(rs.getString("radar_status"));
        v.setCameraStatus(rs.getString("camera_status"));
        v.setBatteryLevel(rs.getDouble("battery_level"));

        double score = rs.getDouble("safety_score");
        if (!rs.wasNull()) {
            v.setSafetyScore(score);
        } else {
            v.setSafetyScore(null);
        }

        v.setSafetyGrade(rs.getString("safety_grade"));
        v.setFleetStatus(rs.getString("fleet_status"));
        v.setCreatedAt(rs.getTimestamp("created_at"));
        return v;
    }

    /**
     * Inserts a completed trip audit record and sets the auto-generated trip_id on the object.
     * 
     * @param trip Trip domain object
     * @return boolean true if inserted successfully
     */
    public boolean insertTrip(Trip trip) {
        String sql = "INSERT INTO trips (vehicle_id, trip_distance, speeding_violations, arrival_condition, disengagements, disengagement_rate) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, trip.getVehicleId());
            ps.setDouble(2, trip.getTripDistance());
            ps.setInt(3, trip.getSpeedingViolations());
            ps.setDouble(4, trip.getArrivalCondition());
            ps.setInt(5, trip.getDisengagements());
            ps.setDouble(6, trip.getDisengagementRate());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        trip.setTripId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] insertTrip failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Records a completed trip audit.
     */
    public int recordTrip(int vehicleId, double tripDistance, int speedingViolations, 
                          double arrivalCondition, int disengagements, double disengagementRate) {
        Trip trip = new Trip(vehicleId, tripDistance, speedingViolations, arrivalCondition, disengagements, disengagementRate);
        if (insertTrip(trip)) {
            return trip.getTripId();
        }
        return -1;
    }

    /**
     * Aggregates the total manual safety driver interventions recorded across telemetry logs for a vehicle.
     * 
     * @param vehicleId Vehicle ID
     * @return sum of manual_interventions
     */
    public int getTotalManualInterventions(int vehicleId) {
        String sql = "SELECT COALESCE(SUM(manual_interventions), 0) AS total_interventions FROM telemetry_logs WHERE vehicle_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_interventions");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getTotalManualInterventions failed: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Retrieves the most recent pre-trip inspection for a vehicle.
     * 
     * @param vehicleId Vehicle ID
     * @return PreTripInspection object or null if none on record
     */
    public PreTripInspection getLatestPreTripInspection(int vehicleId) {
        String sql = "SELECT * FROM pre_trip_inspections WHERE vehicle_id = ? ORDER BY inspection_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PreTripInspection insp = new PreTripInspection();
                    insp.setInspectionId(rs.getInt("inspection_id"));
                    insp.setVehicleId(rs.getInt("vehicle_id"));
                    insp.setVin(rs.getString("vin"));
                    insp.setLidarStatus(rs.getString("lidar_status"));
                    insp.setRadarStatus(rs.getString("radar_status"));
                    insp.setCameraStatus(rs.getString("camera_status"));
                    insp.setBatteryLevel(rs.getDouble("battery_level"));
                    insp.setInspectionStatus(rs.getString("inspection_status"));
                    insp.setFailureReason(rs.getString("failure_reason"));
                    insp.setInspectionDate(rs.getTimestamp("inspection_date"));
                    return insp;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getLatestPreTripInspection failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all historical trips for a specific vehicle.
     * 
     * @param vehicleId Vehicle ID
     * @return List of Trip objects
     */
    public List<Trip> getTripsByVehicleId(int vehicleId) {
        List<Trip> list = new ArrayList<>();
        String sql = "SELECT t.*, v.vin, v.vehicle_type FROM trips t " +
                     "JOIN vehicles v ON t.vehicle_id = v.vehicle_id " +
                     "WHERE t.vehicle_id = ? ORDER BY t.trip_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Trip(
                        rs.getInt("trip_id"),
                        rs.getInt("vehicle_id"),
                        rs.getString("vin"),
                        rs.getString("vehicle_type"),
                        rs.getDouble("trip_distance"),
                        rs.getInt("speeding_violations"),
                        rs.getDouble("arrival_condition"),
                        rs.getInt("disengagements"),
                        rs.getDouble("disengagement_rate"),
                        rs.getTimestamp("trip_date")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getTripsByVehicleId failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all trips across the entire fleet.
     * 
     * @return List of Trip objects
     */
    public List<Trip> getAllTrips() {
        List<Trip> list = new ArrayList<>();
        String sql = "SELECT t.*, v.vin, v.vehicle_type FROM trips t " +
                     "JOIN vehicles v ON t.vehicle_id = v.vehicle_id " +
                     "ORDER BY t.trip_date DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Trip(
                    rs.getInt("trip_id"),
                    rs.getInt("vehicle_id"),
                    rs.getString("vin"),
                    rs.getString("vehicle_type"),
                    rs.getDouble("trip_distance"),
                    rs.getInt("speeding_violations"),
                    rs.getDouble("arrival_condition"),
                    rs.getInt("disengagements"),
                    rs.getDouble("disengagement_rate"),
                    rs.getTimestamp("trip_date")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getAllTrips failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Records high-frequency telemetry metrics into telemetry_logs using a TelemetryLog object.
     * Sets the auto-generated log_id on the object.
     * 
     * @param log TelemetryLog domain entity
     * @return boolean true if inserted successfully
     */
    public boolean insertTelemetryLog(TelemetryLog log) {
        String sql = "INSERT INTO telemetry_logs (vehicle_id, lidar_confidence, manual_interventions, hard_braking_events) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getVehicleId());
            ps.setDouble(2, log.getLidarConfidence());
            ps.setInt(3, log.getManualInterventions());
            ps.setInt(4, log.getHardBrakingEvents());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        log.setLogId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] insertTelemetryLog failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Records high-frequency telemetry metrics into telemetry_logs.
     */
    public boolean logTelemetry(int vehicleId, double lidarConfidence, int manualInterventions, int hardBraking) {
        TelemetryLog log = new TelemetryLog(vehicleId, lidarConfidence, manualInterventions, hardBraking);
        return insertTelemetryLog(log);
    }

    /**
     * Retrieves all telemetry logs recorded for a specific vehicle.
     * 
     * @param vehicleId Vehicle ID
     * @return List of TelemetryLog objects
     */
    public List<TelemetryLog> getTelemetryLogsByVehicleId(int vehicleId) {
        List<TelemetryLog> logs = new ArrayList<>();
        String sql = "SELECT tl.*, v.vin FROM telemetry_logs tl " +
                     "JOIN vehicles v ON tl.vehicle_id = v.vehicle_id " +
                     "WHERE tl.vehicle_id = ? ORDER BY tl.log_timestamp DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new TelemetryLog(
                        rs.getInt("log_id"),
                        rs.getInt("vehicle_id"),
                        rs.getString("vin"),
                        rs.getDouble("lidar_confidence"),
                        rs.getInt("manual_interventions"),
                        rs.getInt("hard_braking_events"),
                        rs.getTimestamp("log_timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getTelemetryLogsByVehicleId failed: " + e.getMessage());
        }
        return logs;
    }

    /**
     * Retrieves all telemetry logs across the entire fleet.
     * 
     * @return List of TelemetryLog objects
     */
    public List<TelemetryLog> getAllTelemetryLogs() {
        List<TelemetryLog> logs = new ArrayList<>();
        String sql = "SELECT tl.*, v.vin FROM telemetry_logs tl " +
                     "JOIN vehicles v ON tl.vehicle_id = v.vehicle_id " +
                     "ORDER BY tl.log_timestamp DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new TelemetryLog(
                    rs.getInt("log_id"),
                    rs.getInt("vehicle_id"),
                    rs.getString("vin"),
                    rs.getDouble("lidar_confidence"),
                    rs.getInt("manual_interventions"),
                    rs.getInt("hard_braking_events"),
                    rs.getTimestamp("log_timestamp")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getAllTelemetryLogs failed: " + e.getMessage());
        }
        return logs;
    }

    /**
     * Checks if a vehicle already has an active quarantine record.
     */
    public boolean hasActiveQuarantine(int vehicleId) {
        String sql = "SELECT 1 FROM quarantine_records WHERE vehicle_id = ? AND status = 'ACTIVE_QUARANTINE' LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] hasActiveQuarantine failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Flags a vehicle into SAFETY_QUARANTINE and logs an entry in quarantine_records,
     * preventing duplicate active records for the same vehicle.
     */
    public boolean flagQuarantine(int vehicleId, String reason) {
        String updateVehicleSql = "UPDATE vehicles SET fleet_status = 'SAFETY_QUARANTINE', safety_grade = 'GROUNDED' WHERE vehicle_id = ?";
        String insertQuarantineSql = "INSERT INTO quarantine_records (vehicle_id, reason, status) VALUES (?, ?, 'ACTIVE_QUARANTINE')";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update vehicle state
                try (PreparedStatement psUpdate = conn.prepareStatement(updateVehicleSql)) {
                    psUpdate.setInt(1, vehicleId);
                    psUpdate.executeUpdate();
                }

                // Prevent duplicate active quarantine records
                if (!hasActiveQuarantine(vehicleId)) {
                    try (PreparedStatement psInsert = conn.prepareStatement(insertQuarantineSql)) {
                        psInsert.setInt(1, vehicleId);
                        psInsert.setString(2, reason);
                        psInsert.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] flagQuarantine failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the active quarantine record for a specific vehicle.
     */
    public QuarantineRecord getActiveQuarantineRecord(int vehicleId) {
        String sql = "SELECT qr.*, v.vin, v.vehicle_type FROM quarantine_records qr " +
                     "JOIN vehicles v ON qr.vehicle_id = v.vehicle_id " +
                     "WHERE qr.vehicle_id = ? AND qr.status = 'ACTIVE_QUARANTINE' " +
                     "ORDER BY qr.quarantine_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToQuarantine(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getActiveQuarantineRecord failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all currently active quarantined vehicles.
     */
    public List<QuarantineRecord> getAllQuarantinedVehicles() {
        List<QuarantineRecord> list = new ArrayList<>();
        String sql = "SELECT qr.*, v.vin, v.vehicle_type FROM quarantine_records qr " +
                     "JOIN vehicles v ON qr.vehicle_id = v.vehicle_id " +
                     "WHERE qr.status = 'ACTIVE_QUARANTINE' " +
                     "ORDER BY qr.quarantine_date DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToQuarantine(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getAllQuarantinedVehicles failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves the complete quarantine audit history (both active and cleared).
     */
    public List<QuarantineRecord> getAllQuarantineHistory() {
        List<QuarantineRecord> list = new ArrayList<>();
        String sql = "SELECT qr.*, v.vin, v.vehicle_type FROM quarantine_records qr " +
                     "JOIN vehicles v ON qr.vehicle_id = v.vehicle_id " +
                     "ORDER BY qr.quarantine_date DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToQuarantine(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] getAllQuarantineHistory failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Resolves an active quarantine record after maintenance or recalibration without deleting history.
     */
    public boolean resolveQuarantine(int quarantineId, int vehicleId, String resolutionNotes) {
        String updateQuarantineSql = "UPDATE quarantine_records SET status = 'CLEARED_AFTER_MAINTENANCE' WHERE quarantine_id = ?";
        String updateVehicleSql = "UPDATE vehicles SET fleet_status = 'ACTIVE', safety_grade = 'RECALIBRATION_REQUIRED' WHERE vehicle_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = conn.prepareStatement(updateQuarantineSql)) {
                    ps1.setInt(1, quarantineId);
                    ps1.executeUpdate();
                }

                try (PreparedStatement ps2 = conn.prepareStatement(updateVehicleSql)) {
                    ps2.setInt(1, vehicleId);
                    ps2.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] resolveQuarantine failed: " + e.getMessage());
            return false;
        }
    }

    private QuarantineRecord mapResultSetToQuarantine(ResultSet rs) throws SQLException {
        QuarantineRecord qr = new QuarantineRecord();
        qr.setQuarantineId(rs.getInt("quarantine_id"));
        qr.setVehicleId(rs.getInt("vehicle_id"));
        qr.setVin(rs.getString("vin"));
        qr.setCategory(rs.getString("vehicle_type"));
        qr.setReason(rs.getString("reason"));
        qr.setQuarantineDate(rs.getTimestamp("quarantine_date"));
        qr.setStatus(rs.getString("status"));
        qr.setCategoryThreshold(ASIEngine.getCategoryThreshold(qr.getCategory()));
        return qr;
    }

    /**
     * Updates the calculated Autonomous Safety Index (ASI) and assigned safety band.
     */
    public boolean updateSafetyGrade(int vehicleId, double safetyScore, String safetyGrade) {
        String sql = "UPDATE vehicles SET safety_score = ?, safety_grade = ? WHERE vehicle_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, safetyScore);
            ps.setString(2, safetyGrade);
            ps.setInt(3, vehicleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] updateSafetyGrade failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves a pre-trip inspection record into pre_trip_inspections table.
     * 
     * @param inspection PreTripInspection domain object
     * @return boolean true if saved successfully
     */
    public boolean savePreTripInspection(PreTripInspection inspection) {
        String sql = "INSERT INTO pre_trip_inspections (vehicle_id, vin, lidar_status, radar_status, camera_status, battery_level, inspection_status, failure_reason) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, inspection.getVehicleId());
            ps.setString(2, inspection.getVin());
            ps.setString(3, inspection.getLidarStatus());
            ps.setString(4, inspection.getRadarStatus());
            ps.setString(5, inspection.getCameraStatus());
            ps.setDouble(6, inspection.getBatteryLevel());
            ps.setString(7, inspection.getInspectionStatus());
            ps.setString(8, inspection.getFailureReason());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        inspection.setInspectionId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] savePreTripInspection failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Updates the vehicle's live sensor calibration status and battery level in the vehicles table.
     */
    public boolean updateVehicleSensorAndBattery(int vehicleId, String lidar, String radar, String camera, double battery) {
        String sql = "UPDATE vehicles SET lidar_status = ?, radar_status = ?, camera_status = ?, battery_level = ? WHERE vehicle_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lidar);
            ps.setString(2, radar);
            ps.setString(3, camera);
            ps.setDouble(4, battery);
            ps.setInt(5, vehicleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] updateVehicleSensorAndBattery failed: " + e.getMessage());
            return false;
        }
    }
}
