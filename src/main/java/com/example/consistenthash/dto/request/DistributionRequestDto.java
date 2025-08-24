package com.example.consistenthash.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 분산 테스트 요청을 위한 DTO
 */
public class DistributionRequestDto {

    @Min(value = 1, message = "키 개수는 최소 1개 이상이어야 합니다")
    @Max(value = 1000000, message = "키 개수는 최대 1,000,000개까지 가능합니다")
    private int keyCount = 1000;

    @NotBlank(message = "키 접두사는 필수입니다")
    @Size(min = 1, max = 20, message = "키 접두사는 1-20자 사이여야 합니다")
    private String keyPrefix = "testkey";

    // 기본 생성자
    public DistributionRequestDto() {}

    // 전체 생성자
    public DistributionRequestDto(int keyCount, String keyPrefix) {
        this.keyCount = keyCount;
        this.keyPrefix = keyPrefix;
    }

    // Getters and Setters
    public int getKeyCount() {
        return keyCount;
    }

    public void setKeyCount(int keyCount) {
        this.keyCount = keyCount;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    @Override
    public String toString() {
        return String.format("DistributionRequestDto{keyCount=%d, keyPrefix='%s'}", keyCount, keyPrefix);
    }
}