package com.example.consistenthash.dto.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 해시 링 상태 정보 응답 DTO
 */
public class RingInfoResponseDto {

    private int physicalNodeCount;
    private int virtualNodeCount;
    private List<String> nodeList;
    private Map<String, Integer> dataDistribution;
    private double averageKeysPerNode;
    private double distributionUniformity;

    // 기본 생성자
    public RingInfoResponseDto() {
        this.nodeList = new ArrayList<>();
        this.dataDistribution = new HashMap<>();
    }

    // 전체 생성자 - 방어적 복사 적용
    public RingInfoResponseDto(int physicalNodeCount,
                               int virtualNodeCount,
                               List<String> nodeList,
                               Map<String, Integer> dataDistribution) {
        this.physicalNodeCount = physicalNodeCount;
        this.virtualNodeCount = virtualNodeCount;

        // 방어적 복사 1단계: 생성자에서 깊은 복사
        this.nodeList = createDefensiveCopyOfList(nodeList);
        this.dataDistribution = createDefensiveCopyOfMap(dataDistribution);

        // 계산된 필드들
        calculateDerivedFields();
    }

    /**
     * List의 방어적 복사를 생성하는 유틸리티 메서드
     */
    private List<String> createDefensiveCopyOfList(List<String> original) {
        if (original == null) {
            return new ArrayList<>();
        }

        // 완전히 새로운 ArrayList를 생성하고 모든 요소를 복사
        List<String> copy = new ArrayList<>(original.size());
        for (String item : original) {
            copy.add(item); // String은 immutable이므로 참조 복사 안전
        }
        return copy;
    }

    /**
     * Map의 방어적 복사를 생성하는 유틸리티 메서드
     */
    private Map<String, Integer> createDefensiveCopyOfMap(Map<String, Integer> original) {
        if (original == null) {
            return new HashMap<>();
        }

        // 완전히 새로운 HashMap을 생성하고 모든 엔트리를 복사
        Map<String, Integer> copy = new HashMap<>(original.size());
        for (Map.Entry<String, Integer> entry : original.entrySet()) {
            copy.put(entry.getKey(), entry.getValue()); // String과 Integer는 immutable
        }
        return copy;
    }

    private void calculateDerivedFields() {
        if (dataDistribution != null && !dataDistribution.isEmpty()) {
            // 평균 키 개수 계산
            int totalKeys = dataDistribution.values().stream().mapToInt(Integer::intValue).sum();
            this.averageKeysPerNode = physicalNodeCount > 0 ? (double) totalKeys / physicalNodeCount : 0.0;

            // 분산 균등성 계산 (표준편차 기반)
            if (physicalNodeCount > 1) {
                double variance = dataDistribution.values().stream()
                        .mapToDouble(count -> Math.pow(count - averageKeysPerNode, 2))
                        .average()
                        .orElse(0.0);

                double stdDev = Math.sqrt(variance);
                double coefficientOfVariation = averageKeysPerNode > 0 ? stdDev / averageKeysPerNode : 0.0;
                this.distributionUniformity = Math.max(0, 100 - (coefficientOfVariation * 100));
            } else {
                this.distributionUniformity = 100.0; // 단일 노드는 완벽한 균등성
            }
        }
    }

    // Getters and Setters
    public int getPhysicalNodeCount() {
        return physicalNodeCount;
    }

    public void setPhysicalNodeCount(int physicalNodeCount) {
        this.physicalNodeCount = physicalNodeCount;
    }

    public int getVirtualNodeCount() {
        return virtualNodeCount;
    }

    public void setVirtualNodeCount(int virtualNodeCount) {
        this.virtualNodeCount = virtualNodeCount;
    }

    public List<String> getNodeList() {
        // 방어적 복사 2단계: Getter에서도 새로운 복사본 반환
        return createDefensiveCopyOfList(this.nodeList);
    }

    public void setNodeList(List<String> nodeList) {
        // Setter에서도 방어적 복사
        this.nodeList = createDefensiveCopyOfList(nodeList);
    }

    public Map<String, Integer> getDataDistribution() {
        // 방어적 복사 2단계: Getter에서도 새로운 복사본 반환
        return createDefensiveCopyOfMap(this.dataDistribution);
    }

    public void setDataDistribution(Map<String, Integer> dataDistribution) {
        // Setter에서도 방어적 복사
        this.dataDistribution = createDefensiveCopyOfMap(dataDistribution);
        calculateDerivedFields(); // 분산 관련 필드 재계산
    }

    public double getAverageKeysPerNode() {
        return averageKeysPerNode;
    }

    public void setAverageKeysPerNode(double averageKeysPerNode) {
        this.averageKeysPerNode = averageKeysPerNode;
    }

    public double getDistributionUniformity() {
        return distributionUniformity;
    }

    public void setDistributionUniformity(double distributionUniformity) {
        this.distributionUniformity = distributionUniformity;
    }

    @Override
    public String toString() {
        return String.format("RingInfoResponseDto{physicalNodes=%d, virtualNodes=%d, uniformity=%.2f%%, nodeListSize=%d, distributionSize=%d}",
                physicalNodeCount, virtualNodeCount, distributionUniformity,
                nodeList != null ? nodeList.size() : 0,
                dataDistribution != null ? dataDistribution.size() : 0);
    }
}