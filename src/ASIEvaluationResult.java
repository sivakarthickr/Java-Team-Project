/**
 * =========================================================================================
 * AUTONOMOUS VEHICLE (AV) FLEET SAFETY & TELEMETRY AUDIT ENGINE
 * Module: ASIEvaluationResult.java (Evaluation Value Object)
 * 
 * Description:
 * Holds the comprehensive results of an Autonomous Safety Index (ASI) audit:
 * - Vehicle identification and category
 * - Input component scores and weights
 * - Calculated disengagement rate and category threshold
 * - Computed ASI score and preliminary band
 * - Safety quarantine override status and final assigned safety band
 * =========================================================================================
 */
public class ASIEvaluationResult {

    private int vehicleId;
    private String vin;
    private String category;
    private double disengagementRate;
    private double categoryThreshold;
    private boolean quarantineTriggered;
    private String quarantineReason;

    private double score1; // e.g. Sensor / Obstacle Detection
    private double score2; // e.g. Disengagement / Lane Discipline / Pedestrian Avoidance
    private double score3; // e.g. Rider Safety / Braking Response / Speed Adherence

    private double weight1;
    private double weight2;
    private double weight3;

    private double rawAsiScore;
    private String assignedGrade;
    private String finalFleetStatus;

    public ASIEvaluationResult() {}

    // Getters and Setters
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getDisengagementRate() { return disengagementRate; }
    public void setDisengagementRate(double disengagementRate) { this.disengagementRate = disengagementRate; }

    public double getCategoryThreshold() { return categoryThreshold; }
    public void setCategoryThreshold(double categoryThreshold) { this.categoryThreshold = categoryThreshold; }

    public boolean isQuarantineTriggered() { return quarantineTriggered; }
    public void setQuarantineTriggered(boolean quarantineTriggered) { this.quarantineTriggered = quarantineTriggered; }

    public String getQuarantineReason() { return quarantineReason; }
    public void setQuarantineReason(String quarantineReason) { this.quarantineReason = quarantineReason; }

    public double getScore1() { return score1; }
    public void setScore1(double score1) { this.score1 = score1; }

    public double getScore2() { return score2; }
    public void setScore2(double score2) { this.score2 = score2; }

    public double getScore3() { return score3; }
    public void setScore3(double score3) { this.score3 = score3; }

    public double getWeight1() { return weight1; }
    public void setWeight1(double weight1) { this.weight1 = weight1; }

    public double getWeight2() { return weight2; }
    public void setWeight2(double weight2) { this.weight2 = weight2; }

    public double getWeight3() { return weight3; }
    public void setWeight3(double weight3) { this.weight3 = weight3; }

    public double getRawAsiScore() { return rawAsiScore; }
    public void setRawAsiScore(double rawAsiScore) { this.rawAsiScore = rawAsiScore; }

    public String getAssignedGrade() { return assignedGrade; }
    public void setAssignedGrade(String assignedGrade) { this.assignedGrade = assignedGrade; }

    public String getFinalFleetStatus() { return finalFleetStatus; }
    public void setFinalFleetStatus(String finalFleetStatus) { this.finalFleetStatus = finalFleetStatus; }

    /**
     * Formats detailed audit card for terminal display.
     */
    public String toEvaluationReportString() {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("                 AUTONOMOUS SAFETY INDEX (ASI) EVALUATION REPORT                \n");
        sb.append("================================================================================\n");
        sb.append(String.format("Vehicle: AV%03d (%s) | Category: %s\n", vehicleId, vin, category));
        sb.append(String.format("Disengagement Rate : %.2f per 100 km (Category Threshold: %.2f per 100 km)\n", 
                disengagementRate, categoryThreshold));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("COMPONENT METRICS & WEIGHTS:\n");
        sb.append(String.format("  Component 1: Score = %6.2f | Weight = %2.0f%% | Contribution = %6.2f\n",
                score1, weight1 * 100, score1 * weight1));
        sb.append(String.format("  Component 2: Score = %6.2f | Weight = %2.0f%% | Contribution = %6.2f\n",
                score2, weight2 * 100, score2 * weight2));
        sb.append(String.format("  Component 3: Score = %6.2f | Weight = %2.0f%% | Contribution = %6.2f\n",
                score3, weight3 * 100, score3 * weight3));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("RAW ASI CALCULATED SCORE : %.2f / 100.00\n", rawAsiScore));
        sb.append(String.format("DISENGAGEMENT FILTER     : %s\n", 
                (quarantineTriggered ? "EXCEEDED THRESHOLD -> SAFETY_QUARANTINE ENFORCED" : "PASSED (Within Operational Limit)")));
        if (quarantineTriggered) {
            sb.append(String.format("QUARANTINE REASON        : %s\n", quarantineReason));
            sb.append("QUARANTINE OVERRIDE      : Normal grading overridden -> Assigned GROUNDED\n");
        }
        sb.append(String.format("FINAL SAFETY GRADE       : %s\n", assignedGrade));
        sb.append(String.format("FLEET STATUS             : %s\n", finalFleetStatus));
        sb.append("================================================================================");
        return sb.toString();
    }
}
