package com.example.consistenthash.stability;

import com.example.consistenthash.model.Node;
import com.example.consistenthash.service.ConsistentHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.*;

public class StabilityVerificationTest {

    @Test
    @DisplayName("🛡️ 시스템 안정성 종합 검증")
    void comprehensiveStabilityTest() {
        ConsistentHash hash = new ConsistentHash();

        System.out.println("=".repeat(60));
        System.out.println("           시스템 안정성 종합 검증");
        System.out.println("=".repeat(60));

        // 1. 기본 클러스터 구성
        System.out.println("\n🔹 1단계: 초기 클러스터 구성 (5개 노드)");
        for (int i = 1; i <= 5; i++) {
            hash.addNode(new Node("production-server-" + i));
        }

        // 2. 안정성 메트릭 측정
        StabilityMetrics metrics = new StabilityMetrics();
        measureInitialState(hash, metrics);

        // 3. 장애 시나리오 테스트
        System.out.println("\n🔹 2단계: 장애 내성 테스트");
        testFailureScenarios(hash, metrics);

        // 4. 확장성 테스트
        System.out.println("\n🔹 3단계: 확장성 테스트");
        testScalabilityStability(hash, metrics);

        // 5. 동시성 안정성 테스트
        System.out.println("\n🔹 4단계: 동시성 안정성 테스트");
        testConcurrencyStability(hash, metrics);

        // 6. 최종 안정성 점수 계산
        System.out.println("\n🔹 5단계: 안정성 점수 산출");
        metrics.calculateFinalScore();
        metrics.printReport();

        System.out.println("\n✅ 종합 안정성 검증 완료!");
    }

    private void measureInitialState(ConsistentHash hash, StabilityMetrics metrics) {
        // 10,000개 키로 초기 분산도 측정
        Map<String, Integer> distribution = new HashMap<>();

        for (int i = 0; i < 10000; i++) {
            String key = "user_data_" + i;
            Node node = hash.getNode(key);
            distribution.merge(node.getId(), 1, Integer::sum);
        }

        // 분산 균등성 점수 계산
        double evenness = calculateDistributionEvenness(distribution, 10000);
        metrics.setInitialDistributionScore(evenness);

        System.out.printf("초기 분산 균등성: %.2f/100%n", evenness);
        distribution.forEach((nodeId, count) ->
                System.out.printf("  %s: %d개 (%.1f%%)%n",
                        nodeId, count, (count/10000.0)*100));
    }

    private void testFailureScenarios(ConsistentHash hash, StabilityMetrics metrics) {
        // 시나리오 1: 단일 노드 장애
        System.out.println("\n  📍 시나리오 1: 단일 노드 장애");

        Map<String, String> beforeFailure = captureKeyMapping(hash, 1000);
        hash.removeNode("production-server-1");
        Map<String, String> afterFailure = captureKeyMapping(hash, 1000);

        double dataStability = calculateDataStability(beforeFailure, afterFailure);
        metrics.addFailureRecoveryScore(dataStability);

        System.out.printf("    데이터 안정성: %.2f%% (높을수록 좋음)%n", dataStability);

        // 시나리오 2: 연쇄 장애
        System.out.println("\n  📍 시나리오 2: 연쇄 장애 (2개 노드 추가 실패)");

        Map<String, String> beforeCascade = captureKeyMapping(hash, 1000);
        hash.removeNode("production-server-2");
        hash.removeNode("production-server-3");
        Map<String, String> afterCascade = captureKeyMapping(hash, 1000);

        // 60% 노드 장애에도 시스템 동작 확인
        boolean systemStillWorks = hash.getNode("test_key") != null;
        metrics.setCascadeFailureResistance(systemStillWorks);

        System.out.printf("    연쇄 장애 후 시스템 동작: %s%n",
                systemStillWorks ? "✅ 정상" : "❌ 실패");
    }

    private void testScalabilityStability(ConsistentHash hash, StabilityMetrics metrics) {
        // 현재 상태 기록
        Map<String, String> beforeScale = captureKeyMapping(hash, 1000);

        // 클러스터 확장 (3배 증가)
        for (int i = 6; i <= 12; i++) {
            hash.addNode(new Node("scale-server-" + i));
        }

        Map<String, String> afterScale = captureKeyMapping(hash, 1000);

        // 확장 시 데이터 이동 최소성 확인
        int unchangedKeys = 0;
        for (String key : beforeScale.keySet()) {
            if (Objects.equals(beforeScale.get(key), afterScale.get(key))) {
                unchangedKeys++;
            }
        }

        double scalabilityStability = (unchangedKeys / 1000.0) * 100;
        metrics.setScalabilityStability(scalabilityStability);

        System.out.printf("확장 시 안정성: %.2f%% 키가 기존 위치 유지%n", scalabilityStability);

        // 성능 안정성 측정
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            hash.getNode("perf_test_" + i);
        }
        long endTime = System.currentTimeMillis();

        double avgLatency = (endTime - startTime) / 10000.0;
        metrics.setPerformanceStability(avgLatency);

        System.out.printf("확장 후 평균 조회 시간: %.3fms%n", avgLatency);
    }

    private void testConcurrencyStability(ConsistentHash hash, StabilityMetrics metrics) {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(1000);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        // 1000개 동시 작업 실행
        for (int i = 0; i < 1000; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // 다양한 동시 작업 수행
                    switch (taskId % 4) {
                        case 0 -> hash.addNode(new Node("concurrent_" + taskId));
                        case 1 -> hash.removeNode("concurrent_" + (taskId - 100));
                        case 2 -> hash.getNode("concurrent_lookup_" + taskId);
                        case 3 -> hash.getRingInfo();
                    }
                } catch (Throwable e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            double concurrencyStability = completed && exceptions.isEmpty() ? 100.0 : 0.0;
            metrics.setConcurrencyStability(concurrencyStability);

            System.out.printf("동시성 안정성: %s (예외 %d개)%n",
                    completed ? "✅ 안전" : "❌ 타임아웃",
                    exceptions.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
    }

    private Map<String, String> captureKeyMapping(ConsistentHash hash, int keyCount) {
        Map<String, String> mapping = new HashMap<>();
        for (int i = 0; i < keyCount; i++) {
            String key = "test_key_" + i;
            Node node = hash.getNode(key);
            if (node != null) {
                mapping.put(key, node.getId());
            }
        }
        return mapping;
    }

    private double calculateDataStability(Map<String, String> before, Map<String, String> after) {
        int stableKeys = 0;
        int totalKeys = before.size();

        for (String key : before.keySet()) {
            String beforeNode = before.get(key);
            String afterNode = after.get(key);

            // 노드가 제거되지 않은 키들 중에서 안정성 측정
            if (afterNode != null && beforeNode.equals(afterNode)) {
                stableKeys++;
            }
        }

        return (stableKeys / (double) totalKeys) * 100;
    }

    private double calculateDistributionEvenness(Map<String, Integer> distribution, int totalKeys) {
        if (distribution.isEmpty()) return 0.0;

        double expectedPerNode = (double) totalKeys / distribution.size();
        double variance = distribution.values().stream()
                .mapToDouble(count -> Math.pow(count - expectedPerNode, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / expectedPerNode;

        // 균등성 점수: CV가 낮을수록 높은 점수
        return Math.max(0, 100 - (coefficientOfVariation * 100));
    }

    /**
     * 안정성 메트릭을 종합 관리하는 클래스
     */
    static class StabilityMetrics {
        private double initialDistributionScore = 0;
        private List<Double> failureRecoveryScores = new ArrayList<>();
        private boolean cascadeFailureResistance = false;
        private double scalabilityStability = 0;
        private double performanceStability = 0;
        private double concurrencyStability = 0;
        private double finalScore = 0;

        public void setInitialDistributionScore(double score) {
            this.initialDistributionScore = score;
        }

        public void addFailureRecoveryScore(double score) {
            this.failureRecoveryScores.add(score);
        }

        public void setCascadeFailureResistance(boolean resistant) {
            this.cascadeFailureResistance = resistant;
        }

        public void setScalabilityStability(double stability) {
            this.scalabilityStability = stability;
        }

        public void setPerformanceStability(double latency) {
            // 0.1ms 이하면 100점, 1ms 이상이면 0점
            this.performanceStability = Math.max(0, 100 - (latency * 100));
        }

        public void setConcurrencyStability(double stability) {
            this.concurrencyStability = stability;
        }

        public void calculateFinalScore() {
            double avgFailureRecovery = failureRecoveryScores.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            this.finalScore = (
                    initialDistributionScore * 0.2 +      // 20%
                            avgFailureRecovery * 0.25 +           // 25%
                            (cascadeFailureResistance ? 100 : 0) * 0.15 + // 15%
                            scalabilityStability * 0.2 +          // 20%
                            performanceStability * 0.1 +          // 10%
                            concurrencyStability * 0.1             // 10%
            );
        }

        public void printReport() {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("           🏆 안정성 종합 평가 리포트");
            System.out.println("=".repeat(50));

            System.out.printf("📊 분산 균등성      : %.1f/100점%n", initialDistributionScore);
            System.out.printf("🛡️ 장애 복구능력    : %.1f/100점%n",
                    failureRecoveryScores.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            System.out.printf("⚡ 연쇄 장애 내성    : %s%n", cascadeFailureResistance ? "✅ 통과" : "❌ 실패");
            System.out.printf("📈 확장성 안정성    : %.1f/100점%n", scalabilityStability);
            System.out.printf("🚀 성능 안정성      : %.1f/100점%n", performanceStability);
            System.out.printf("🔒 동시성 안정성    : %.1f/100점%n", concurrencyStability);

            System.out.println("-".repeat(50));
            System.out.printf("🎯 **종합 안정성 점수: %.1f/100점**%n", finalScore);

            String grade = getStabilityGrade(finalScore);
            System.out.printf("🏅 **안정성 등급: %s**%n", grade);

            printRecommendations(finalScore);
        }

        private String getStabilityGrade(double score) {
            if (score >= 90) return "A+ (매우 안정함)";
            if (score >= 80) return "A (안정함)";
            if (score >= 70) return "B (양호함)";
            if (score >= 60) return "C (보통)";
            return "D (개선 필요)";
        }

        private void printRecommendations(double score) {
            System.out.println("\n💡 안정성 개선 권장사항:");

            if (initialDistributionScore < 80) {
                System.out.println("  • 가상 노드 수 증가로 분산 개선");
            }
            if (cascadeFailureResistance == false) {
                System.out.println("  • 복제 전략 구현으로 장애 내성 강화");
            }
            if (performanceStability < 80) {
                System.out.println("  • 해시 함수 최적화로 성능 개선");
            }
            if (score >= 90) {
                System.out.println("  ✨ 프로덕션 환경에 적합한 안정성을 보유하고 있습니다!");
            }
        }
    }
}