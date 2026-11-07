import java.util.concurrent.atomic.AtomicInteger;

/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: SafetyEvaluationAggregator.java (Thread-Safe Statistics Collector)
 * 
 * Description:
 * Utilizes atomic integers to safely aggregate multithreaded evaluation metrics across
 * 100+ concurrent worker threads without shared mutable state race conditions.
 * =========================================================================================
 */
public class SafetyEvaluationAggregator {

    private final AtomicInteger totalVehicles = new AtomicInteger(0);
    private final AtomicInteger processedVehicles = new AtomicInteger(0);
    private final AtomicInteger platinumCount = new AtomicInteger(0);
    private final AtomicInteger goldCount = new AtomicInteger(0);
    private final AtomicInteger recalibrationCount = new AtomicInteger(0);
    private final AtomicInteger groundedCount = new AtomicInteger(0);
    private final AtomicInteger quarantinedCount = new AtomicInteger(0);

    public void setTotalVehicles(int total) {
        totalVehicles.set(total);
    }

    public void recordEvaluation(String safetyGrade, boolean isQuarantined) {
        processedVehicles.incrementAndGet();

        if (isQuarantined) {
            quarantinedCount.incrementAndGet();
        }

        if (ASIEngine.GRADE_PLATINUM.equalsIgnoreCase(safetyGrade)) {
            platinumCount.incrementAndGet();
        } else if (ASIEngine.GRADE_GOLD.equalsIgnoreCase(safetyGrade)) {
            goldCount.incrementAndGet();
        } else if (ASIEngine.GRADE_RECALIBRATION.equalsIgnoreCase(safetyGrade)) {
            recalibrationCount.incrementAndGet();
        } else {
            groundedCount.incrementAndGet();
        }
    }

    public int getTotalVehicles() { return totalVehicles.get(); }
    public int getProcessedVehicles() { return processedVehicles.get(); }
    public int getPlatinumCount() { return platinumCount.get(); }
    public int getGoldCount() { return goldCount.get(); }
    public int getRecalibrationCount() { return recalibrationCount.get(); }
    public int getGroundedCount() { return groundedCount.get(); }
    public int getQuarantinedCount() { return quarantinedCount.get(); }

    /**
     * Formats the standardized multithreaded evaluation output banner.
     */
    public String toDisplaySummary() {
        return "=====================================\n" +
               "MULTITHREADED FLEET SAFETY EVALUATION\n" +
               "=====================================\n\n" +
               String.format("Total Vehicles: %d\n", totalVehicles.get()) +
               String.format("Processed: %d\n", processedVehicles.get()) +
               String.format("Platinum: %d\n", platinumCount.get()) +
               String.format("Gold: %d\n", goldCount.get()) +
               String.format("Recalibration: %d\n", recalibrationCount.get()) +
               String.format("Grounded: %d\n", groundedCount.get()) +
               String.format("Quarantined: %d\n\n", quarantinedCount.get()) +
               "Evaluation Status: COMPLETED\n" +
               "=====================================";
    }
}
