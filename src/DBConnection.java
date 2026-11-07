import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: DBConnection.java (JDBC Connection Manager)
 * 
 * Description:
 * Manages database connectivity to the MySQL database 'av_fleet_db' using JDBC.
 * Sourced securely from environment variables to prevent hardcoding sensitive credentials.
 * Compatible with local development and Oracle Cloud Infrastructure (OCI) Linux VM hosts.
 * =========================================================================================
 */
public class DBConnection {

    // Default connection parameters (used if environment variables are not set)
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB_NAME = "av_fleet_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    // JDBC Driver Class Name
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    /**
     * Resolves configuration parameters from environment variables with safe defaults.
     */
    public static String getHost() {
        return System.getenv().getOrDefault("DB_HOST", DEFAULT_HOST);
    }

    public static String getPort() {
        return System.getenv().getOrDefault("DB_PORT", DEFAULT_PORT);
    }

    public static String getDbName() {
        return System.getenv().getOrDefault("DB_NAME", DEFAULT_DB_NAME);
    }

    public static String getUser() {
        return System.getenv().getOrDefault("DB_USER", DEFAULT_USER);
    }

    public static String getPassword() {
        return System.getenv().getOrDefault("DB_PASS", DEFAULT_PASSWORD);
    }

    /**
     * Constructs the standard MySQL JDBC connection URL.
     * Includes timezone, SSL parameters, and UTF-8 encoding.
     */
    public static String getJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
            getHost(), getPort(), getDbName()
        );
    }

    /**
     * Establishes and returns an active JDBC Connection to the MySQL database.
     * 
     * @return Connection an active SQL connection
     * @throws SQLException if connection fails or database is unreachable
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load and register the MySQL JDBC Driver class
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            String errorMsg = "[ERROR] MySQL JDBC Driver not found in classpath. " +
                              "Ensure 'mysql-connector-j-8.3.0.jar' is present in the 'lib/' directory.";
            System.err.println(errorMsg);
            throw new SQLException(errorMsg, e);
        }

        return DriverManager.getConnection(getJdbcUrl(), getUser(), getPassword());
    }

    /**
     * Safely closes a database Connection resource without throwing uncaught exceptions.
     * 
     * @param conn the Connection to close
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("[WARN] Failed to close database connection: " + e.getMessage());
            }
        }
    }

    /**
     * Tests database connectivity and outputs the standardized status banner.
     * 
     * @return boolean true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        System.out.println("================================");
        System.out.println("AV FLEET DATABASE CONNECTION");
        System.out.println("================================");
        System.out.println("Connecting to MySQL...");
        System.out.println("Database: " + getDbName());

        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Connection Status: SUCCESS");
                System.out.println("JDBC Connection Established!");
                System.out.println("================================");
                return true;
            } else {
                System.out.println("Connection Status: FAILED");
                System.out.println("Error: Connection returned null or closed.");
                System.out.println("================================");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Connection Status: FAILED");
            System.out.println("Error: " + e.getMessage());
            System.out.println("================================");
            return false;
        }
    }

    /**
     * Standalone main method to execute the database connection test directly.
     * 
     * Usage:
     *   java -cp "bin;lib/*" DBConnection (Windows)
     *   java -cp "bin:lib/*" DBConnection (Linux/macOS)
     */
    public static void main(String[] args) {
        testConnection();
    }
}
