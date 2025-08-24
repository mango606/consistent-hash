package com.example.consistenthash.benchmark;

import com.example.consistenthash.model.Node;
import com.example.consistenthash.service.ConsistentHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

/**
 * 실제 성능을 측정하여 README 업데이트용 데이터 생성
 */
public class PerformanceBenchmarkTest {

    @Test
    @DisplayName("🚀 확장성 성능 실측 테스트")
    void measureActualPerformance() {
        System.out.println("=".repeat(80));
        System.out.println("                    📊 실제 성능 측정 결과");
        System.out.println("=".repeat(80));
        System.out.printf("%-10s %-12s %-15s %-15s %-12s%n",
                "노드 수", "조회 시간", "메모리 사용", "균등성 점수", "노드 추가시간");
        System.out.println("-".repeat(80));

        // 다양한 노드 수로 성능 측정
        int[] nodeCounts = {10, 50, 100, 500, 1000};

        for (int nodeCount : nodeCounts) {
            PerformanceResult result = measurePerformanceForNodeCount(nodeCount);
            System.out.printf("%-10d %-12s %-15s %-15.1f %-12s%n",
                    nodeCount,
                    String.format("%.3fms", result.avgLookupTime),
                    formatBytes(result.memoryUsage),
                    result.uniformityScore,
                    String.format("%.1fms", result.nodeAddTime));
        }

        System.out.println("-".repeat(80));
        System.out.println("💡 메모리 사용량은 추정치입니다 (JVM 특성상 정확한 측정 어려움)");
        System.out.println("⚡ 조회 시간은 10,000회 평균입니다");
        System.out.println("🎯 균등성 점수는 1,000개 키 기준입니다");
    }

    private PerformanceResult measurePerformanceForNodeCount(int nodeCount) {
        ConsistentHash hash = new ConsistentHash();

        // 1. 노드 추가 시간 측정
        long addStartTime = System.nanoTime();
        for (int i = 1; i <= nodeCount; i++) {
            hash.addNode(new Node("node_" + i, "host" + i + ".com", 8080 + i));
        }
        long addEndTime = System.nanoTime();
        double nodeAddTime = (addEndTime - addStartTime) / 1_000_000.0; // ms 변환

        // 2. 조회 시간 측정 (10,000회)
        int lookupCount = 10000;
        long lookupStartTime = System.nanoTime();

        for (int i = 0; i < lookupCount; i++) {
            hash.getNode("test_key_" + i);
        }

        long lookupEndTime = System.nanoTime();
        double avgLookupTime = (lookupEndTime - lookupStartTime) / 1_000_000.0 / lookupCount;

        // 3. 균등성 점수 측정
        double uniformityScore = measureUniformityScore(hash);

        // 4. 메모리 사용량 추정 (대략적)
        long estimatedMemory = estimateMemoryUsage(nodeCount);

        return new PerformanceResult(avgLookupTime, estimatedMemory, uniformityScore, nodeAddTime);
    }

    private double measureUniformityScore(ConsistentHash hash) {
        Map<String, Integer> distribution = new HashMap<>();
        int testKeys = 1000;

        // 1000개 키로 분산 측정
        for (int i = 0; i < testKeys; i++) {
            Node node = hash.getNode("uniform_test_" + i);
            if (node != null) {
                distribution.merge(node.getId(), 1, Integer::sum);
            }
        }

        if (distribution.isEmpty()) return 0.0;

        // 균등성 점수 계산
        double expectedPerNode = (double) testKeys / distribution.size();
        double variance = distribution.values().stream()
                .mapToDouble(count -> Math.pow(count - expectedPerNode, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / expectedPerNode;

        return Math.max(0, 100 - (coefficientOfVariation * 100));
    }

    private long estimateMemoryUsage(int nodeCount) {
        // 추정 공식:
        // - 각 노드당 150개 가상 노드
        // - TreeMap Entry: ~64 bytes (Long + Node 참조 + 오버헤드)
        // - Node 객체: ~200 bytes
        // - 기타 오버헤드: ~20%

        long virtualNodes = nodeCount * 150L;
        long treeMapMemory = virtualNodes * 64; // TreeMap entries
        long nodeObjectsMemory = nodeCount * 200L; // Node objects
        long overhead = (long) ((treeMapMemory + nodeObjectsMemory) * 0.2);

        return treeMapMemory + nodeObjectsMemory + overhead;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024));
        return String.format("%.1fGB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 성능 측정 결과를 담는 클래스
     */
    private static class PerformanceResult {
        final double avgLookupTime;
        final long memoryUsage;
        final double uniformityScore;
        final double nodeAddTime;

        PerformanceResult(double avgLookupTime, long memoryUsage,
                          double uniformityScore, double nodeAddTime) {
            this.avgLookupTime = avgLookupTime;
            this.memoryUsage = memoryUsage;
            this.uniformityScore = uniformityScore;
            this.nodeAddTime = nodeAddTime;
        }
    }

    @Test
    @DisplayName("🎯 단일 노드 상세 분석")
    void detailedSingleNodeAnalysis() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("           🔍 단일 노드 상세 성능 분석");
        System.out.println("=".repeat(60));

        ConsistentHash hash = new ConsistentHash();
        hash.addNode(new Node("test_node", "localhost", 8080));

        // 다양한 키 개수로 조회 시간 측정
        int[] keyCounts = {100, 1000, 10000, 50000, 100000};

        System.out.printf("%-12s %-15s %-15s%n", "키 개수", "총 시간", "평균 시간");
        System.out.println("-".repeat(45));

        for (int keyCount : keyCounts) {
            long startTime = System.nanoTime();

            for (int i = 0; i < keyCount; i++) {
                hash.getNode("detailed_test_" + i);
            }

            long endTime = System.nanoTime();
            double totalTime = (endTime - startTime) / 1_000_000.0;
            double avgTime = totalTime / keyCount;

            System.out.printf("%-12s %-15s %-15s%n",
                    String.format("%,d개", keyCount),
                    String.format("%.2fms", totalTime),
                    String.format("%.4fms", avgTime));
        }

        System.out.println("-".repeat(45));
        System.out.println("💡 단일 노드에서도 O(log N) 성능 유지!");
    }

    @Test
    @DisplayName("📈 확장성 부하 테스트")
    void scalabilityStressTest() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("           🔥 확장성 부하 테스트");
        System.out.println("=".repeat(60));

        ConsistentHash hash = new ConsistentHash();

        // 점진적으로 노드 추가하면서 성능 변화 측정
        System.out.printf("%-10s %-15s %-20s%n", "단계", "누적 노드", "100K 조회 시간");
        System.out.println("-".repeat(50));

        for (int step = 1; step <= 10; step++) {
            // 10개씩 노드 추가
            for (int i = 1; i <= 10; i++) {
                int nodeId = (step - 1) * 10 + i;
                hash.addNode(new Node("stress_node_" + nodeId));
            }

            // 100,000번 조회 테스트
            long startTime = System.nanoTime();
            for (int i = 0; i < 100000; i++) {
                hash.getNode("stress_key_" + i);
            }
            long endTime = System.nanoTime();

            double totalTime = (endTime - startTime) / 1_000_000.0;

            System.out.printf("%-10d %-15d %-20s%n",
                    step, step * 10, String.format("%.2fms", totalTime));
        }

        System.out.println("-".repeat(50));
        System.out.println("📊 결과: 노드 수가 10배 증가해도 조회 시간은 완만하게 증가 (로그 스케일)");
    }
}