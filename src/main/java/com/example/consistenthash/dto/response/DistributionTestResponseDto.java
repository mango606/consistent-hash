package com.example.consistenthash.dto.response;

import java.util.Map;

/**
 * 분산 테스트 결과 응답 DTO
 */
public class DistributionTestResponseDto {

    private int totalKeyCount;
    private Map<String, Integer> nodeDistribution;
    private Map<String, Double> distributionPercentages;
    private double uniformityScore;
    private String uniformityGrade;
    private TestStatistics statistics;

    // 기본 생성자
    public DistributionTestResponseDto() {}

    // 전체 생성자
    public DistributionTestResponseDto(int totalKeyCount, Map<String, Integer> nodeDistribution) {
        this.totalKeyCount = totalKeyCount;
        this.nodeDistribution = nodeDistribution;

        // 계산된 필드들 초기화
        calculateDerivedFields();
    }

    private void calculateDerivedFields() {
        if (nodeDistribution != null && !nodeDistribution.isEmpty()) {
            // 백분율 계산
            this.distributionPercentages = new java.util.HashMap<>();
            nodeDistribution.forEach((nodeId, count) -> {
                double percentage = (count / (double) totalKeyCount) * 100;
                this.distributionPercentages.put(nodeId, percentage);
            });

            // 균등성 점수 계산
            this.uniformityScore = calculateUniformityScore();
            this.uniformityGrade = getUniformityGrade(uniformityScore);

            // 통계 정보 계산
            this.statistics = new TestStatistics();
        }
    }

    private double calculateUniformityScore() {
        int nodeCount = nodeDistribution.size();
        double expectedPerNode = (double) totalKeyCount / nodeCount;

        double variance = nodeDistribution.values().stream()
                .mapToDouble(count -> Math.pow(count - expectedPerNode, 2))
                .average()
                .orElse(0.0);

        double coefficientOfVariation = Math.sqrt(variance) / expectedPerNode;
        return Math.max(0, 100 - (coefficientOfVariation * 100));
    }

    private String getUniformityGrade(double score) {
        if (score >= 95) return "A+ (매우 우수)";
        if (score >= 90) return "A (우수)";
        if (score >= 80) return "B (양호)";
        if (score >= 70) return "C (보통)";
        if (score >= 60) return "D (미흡)";
        return "F (불량)";
    }

    // Getters and Setters
    public int getTotalKeyCount() {
        return totalKeyCount;
    }

    public void setTotalKeyCount(int totalKeyCount) {
        this.totalKeyCount = totalKeyCount;
    }

    public Map<String, Integer> getNodeDistribution() {
        return nodeDistribution;
    }

    public void setNodeDistribution(Map<String, Integer> nodeDistribution) {
        this.nodeDistribution = nodeDistribution;
        calculateDerivedFields(); // 계산된 필드들 재계산
    }

    public Map<String, Double> getDistributionPercentages() {
        return distributionPercentages;
    }

    public void setDistributionPercentages(Map<String, Double> distributionPercentages) {
        this.distributionPercentages = distributionPercentages;
    }

    public double getUniformityScore() {
        return uniformityScore;
    }

    public void setUniformityScore(double uniformityScore) {
        this.uniformityScore = uniformityScore;
    }

    public String getUniformityGrade() {
        return uniformityGrade;
    }

    public void setUniformityGrade(String uniformityGrade) {
        this.uniformityGrade = uniformityGrade;
    }

    public TestStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(TestStatistics statistics) {
        this.statistics = statistics;
    }

    /**
     * 테스트 통계 정보를 담는 내부 클래스
     */
    public class TestStatistics {
        private double expectedKeysPerNode;
        private int minKeysPerNode;
        private int maxKeysPerNode;
        private double standardDeviation;
        private double imbalanceRatio;

        public TestStatistics() {
            if (nodeDistribution != null && !nodeDistribution.isEmpty()) {
                this.expectedKeysPerNode = (double) totalKeyCount / nodeDistribution.size();
                this.minKeysPerNode = nodeDistribution.values().stream().mapToInt(Integer::intValue).min().orElse(0);
                this.maxKeysPerNode = nodeDistribution.values().stream().mapToInt(Integer::intValue).max().orElse(0);

                double variance = nodeDistribution.values().stream()
                        .mapToDouble(count -> Math.pow(count - expectedKeysPerNode, 2))
                        .average()
                        .orElse(0.0);
                this.standardDeviation = Math.sqrt(variance);

                // 불균형 비율 = 최대값 / 최소값
                this.imbalanceRatio = minKeysPerNode > 0 ? (double) maxKeysPerNode / minKeysPerNode : 0.0;
            }
        }

        // Getters
        public double getExpectedKeysPerNode() { return expectedKeysPerNode; }
        public int getMinKeysPerNode() { return minKeysPerNode; }
        public int getMaxKeysPerNode() { return maxKeysPerNode; }
        public double getStandardDeviation() { return standardDeviation; }
        public double getImbalanceRatio() { return imbalanceRatio; }
    }

    @Override
    public String toString() {
        return String.format("DistributionTestResponseDto{totalKeys=%d, nodes=%d, uniformity=%.2f, grade='%s'}",
                totalKeyCount, nodeDistribution.size(), uniformityScore, uniformityGrade);
    }
}