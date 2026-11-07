import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: MultithreadedFleetAuditEngine.java (Parallel Fleet Evaluation Orchestrator)
 * 
 * Description:
 * Manages parallel background worker threads to evaluate real-time telemetry streams
 * and safety records across 100+ active autonomous vehicles simultaneously.
 * 
 * Architecture Highlights:
 * - Uses bounded thread pool via Executors.newFixedThreadPool(THREAD_POOL_SIZE).
 * - Avoids thread overhead by reusing pooled worker threads.
 * - Thread-safe statistics collection via SafetyEvaluationAggregator (AtomicInteger counters).
 * - Barrier synchronization via executor.awaitTermination() ensuring all 100+ tasks
 *   finish before displaying the final audit summary.
 * =========================================================================================
 */
public class MultithreadedFleetAuditEngine {

    // Configurable bounded thread pool size
    public static final int THREAD_POOL_SIZE = 12;
    public static final int DEFAULT_SIMULATION_FLEET_SIZE = 100;

    private final FleetDAO fleetDAO;
    private final ASIEngine asiEngine;

    public MultithreadedFleetAuditEngine() {
        this.fleetDAO = new FleetDAO();
        this.asiEngine = new ASIEngine(fleetDAO);
    }

    public MultithreadedFleetAuditEngine(FleetDAO fleetDAO) {
        this.fleetDAO = fleetDAO;
        this.asiEngine = new ASIEngine(fleetDAO);
    }

    /**
     * Executes parallel multithreaded evaluation across 100+ active AVs.
     * 
     * @param fleetSize Minimum number of vehicles to evaluate (e.g. 100)
     * @return SafetyEvaluationAggregator populated with final counts
     */
    public SafetyEvaluationAggregator executeMultithreadedAudit(int fleetSize) {
        System.out.println("[MULTITHREADING] Initializing Fixed Thread Pool (Size: " + THREAD_POOL_SIZE + " concurrent workers)...");
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        SafetyEvaluationAggregator aggregator = new SafetyEvaluationAggregator();

        // 1. Gather live vehicles from database
        List<Vehicle> liveVehicles = fleetDAO.getAllVehicles();
        List<SafetyWorker> workerTasks = new ArrayList<>();

        int totalTarget = Math.max(fleetSize, liveVehicles.size());
        aggregator.setTotalVehicles(totalTarget);

        System.out.println("[MULTITHREADING] Preparing evaluation tasks for " + totalTarget + " autonomous vehicles...");

        // First add live DB vehicles
        for (Vehicle v : liveVehicles) {
            workerTasks.add(new SafetyWorker(v, fleetDAO, asiEngine, aggregator));
        }

        // Fill remaining up to 100+ with clearly identified test fixture vehicles
        int neededFixtures = totalTarget - liveVehicles.size();
        if (neededFixtures > 0) {
            List<SafetyWorker> fixtures = generateSimulatedTestFixtures(liveVehicles.size() + 1, neededFixtures, aggregator);
            workerTasks.addAll(fixtures);
        }

        // 2. Dispatch tasks to the Fixed Thread Pool
        long startTime = System.currentTimeMillis();
        System.out.println("[MULTITHREADING] Dispatching " + workerTasks.size() + " worker threads to ExecutorService...");

        for (SafetyWorker task : workerTasks) {
            executor.execute(task);
        }

        // 3. Graceful Shutdown & Barrier Synchronization
        executor.shutdown();
        try {
            boolean completedInTime = executor.awaitTermination(60, TimeUnit.SECONDS);
            long elapsedMs = System.currentTimeMillis() - startTime;

            if (!completedInTime) {
                System.err.println("[WARN] Thread pool did not terminate within timeout; forcing shutdown.");
                executor.shutdownNow();
            } else {
                System.out.println(String.format("[MULTITHREADING] All %d tasks successfully completed in %d ms.",
                        workerTasks.size(), elapsedMs));
            }
        } catch (InterruptedException e) {
            System.err.println("[ERROR] Thread pool interrupted during execution: " + e.getMessage());
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 4. Output the Standardized Evaluation Banner
        System.out.println("\n" + aggregator.toDisplaySummary() + "\n");
        return aggregator;
    }

    /**
     * Generates clearly identified test fixtures representing a diverse 100+ vehicle fleet:
     * - Distributed across all 3 categories (Urban Robotaxi, Highway Freight Truck, Delivery Rover)
     * - Calibrated and uncalibrated sensor states
     * - Nominal and threshold-exceeding disengagement rates
     * - Covers all safety bands: Platinum, Gold, Recalibration, Grounded
     */
    public List<SafetyWorker> generateSimulatedTestFixtures(int startId, int count, SafetyEvaluationAggregator aggregator) {
        List<SafetyWorker> fixtures = new ArrayList<>();
        Random rng = new Random(42); // Fixed seed for reproducible benchmarks

        String[] categories = {
            Vehicle.CATEGORY_URBAN_ROBOTAXI,
            Vehicle.CATEGORY_HIGHWAY_FREIGHT_TRUCK,
            Vehicle.CATEGORY_LAST_MILE_DELIVERY_ROVER
        };

        for (int i = 0; i < count; i++) {
            int fixtureIndex = startId + i;
            // Use negative ID for test fixtures to distinguish from persistent MySQL rows
            int simulatedId = -fixtureIndex;
            String cat = categories[i % categories.length];
            String vin = String.format("1HGCR2F83HA%06d", (100000 + fixtureIndex));

            double dist = 100.0 + (i % 10) * 15.0; // 100 - 235 km
            int disengagements;
            double sensorScore;
            double tertiaryScore;
            String lidarStatus = "CALIBRATED";

            // Calibrate tiers across the 100 vehicles
            if (i < 48) {
                // Tier 1: Platinum Safety (ASI >= 90, Disengagements = 0 or 1, Calibrated sensors)
                disengagements = (i % 3 == 0) ? 1 : 0;
                sensorScore = 94.0 + (i % 6); // 94 - 99
                tertiaryScore = 92.0 + (i % 7); // 92 - 98
            } else if (i < 78) {
                // Tier 2: Gold Fleet (ASI 75 - 89, Disengagements = 1 or 2)
                disengagements = 1 + (i % 2);
                sensorScore = 80.0 + (i % 8); // 80 - 87
                tertiaryScore = 78.0 + (i % 10); // 78 - 87
            } else if (i < 90) {
                // Tier 3: Recalibration Required (ASI 60 - 74)
                disengagements = 1 + (i % 2);
                sensorScore = 65.0 + (i % 8); // 65 - 72
                tertiaryScore = 62.0 + (i % 10); // 62 - 71
            } else {
                // Tier 4: Grounded / Quarantined (Exceeds category threshold or sensor degraded)
                disengagements = 6 + (i % 5); // Exceeds all thresholds
                sensorScore = 52.0 + (i % 10);
                tertiaryScore = 55.0;
                lidarStatus = (i % 2 == 0) ? "DEGRADED" : "CALIBRATED";
            }

            Vehicle v = new Vehicle(simulatedId, vin, cat, lidarStatus, "CALIBRATED", "CALIBRATED", 90.0);
            fixtures.add(new SafetyWorker(v, fleetDAO, asiEngine, aggregator, dist, disengagements, sensorScore, tertiaryScore));
        }

        return fixtures;
    }
}
