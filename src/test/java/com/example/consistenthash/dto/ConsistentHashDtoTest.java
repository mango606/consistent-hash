package com.example.consistenthash.dto;

import com.example.consistenthash.dto.request.NodeRequestDto;
import com.example.consistenthash.dto.request.DistributionRequestDto;
import com.example.consistenthash.dto.response.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsistentHashDtoTest {

    private Validator validator;

    @BeforeAll
    void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("NodeRequestDto 테스트")
    class NodeRequestDtoTest {

        @Test
        @DisplayName("정상적인 노드 요청 DTO 생성")
        void createValidNodeRequestDto() {
            // Given & When
            NodeRequestDto dto = new NodeRequestDto("server1", "192.168.1.1", 8080);

            // Then
            assertThat(dto.getId()).isEqualTo("server1");
            assertThat(dto.getHost()).isEqualTo("192.168.1.1");
            assertThat(dto.getPort()).isEqualTo(8080);

            // 검증 통과 확인
            Set<ConstraintViolation<NodeRequestDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("기본값 생성자 테스트")
        void createNodeRequestDtoWithDefaults() {
            // Given & When
            NodeRequestDto dto = new NodeRequestDto("server1");

            // Then
            assertThat(dto.getId()).isEqualTo("server1");
            assertThat(dto.getHost()).isEqualTo("localhost");
            assertThat(dto.getPort()).isEqualTo(8080);
        }

        @Test
        @DisplayName("노드 ID 검증 실패 테스트")
        void validateNodeIdConstraints() {
            // Given: 빈 ID - @NotBlank와 @Size 모두 위반
            NodeRequestDto emptyId = new NodeRequestDto("", "localhost", 8080);

            // When
            Set<ConstraintViolation<NodeRequestDto>> violations = validator.validate(emptyId);

            // Then: 2개의 검증 오류 (NotBlank + Size)
            assertThat(violations).hasSize(2);

            // 개별 검증 오류 메시지 확인
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toList());
            assertThat(errorMessages).contains("노드 ID는 필수입니다");
            assertThat(errorMessages).contains("노드 ID는 1-50자 사이여야 합니다");

            // Given: null ID - NotBlank만 위반
            NodeRequestDto nullIdDto = new NodeRequestDto();
            nullIdDto.setId(null);

            // When
            violations = validator.validate(nullIdDto);

            // Then: 1개의 검증 오류 (NotBlank만)
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("노드 ID는 필수입니다");

            // Given: 너무 긴 ID - Size만 위반
            String longId = "a".repeat(51);
            NodeRequestDto longIdDto = new NodeRequestDto(longId, "localhost", 8080);

            // When
            violations = validator.validate(longIdDto);

            // Then: 1개의 검증 오류 (Size만)
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("노드 ID는 1-50자 사이여야 합니다");
        }

        @Test
        @DisplayName("포트 검증 실패 테스트")
        void validatePortConstraints() {
            // Given: 음수 포트
            NodeRequestDto negativePort = new NodeRequestDto("server1", "localhost", -1);

            // When
            Set<ConstraintViolation<NodeRequestDto>> violations = validator.validate(negativePort);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("포트는 양수여야 합니다");
        }
    }

    @Nested
    @DisplayName("DistributionRequestDto 테스트")
    class DistributionRequestDtoTest {

        @Test
        @DisplayName("정상적인 분산 테스트 요청 DTO 생성")
        void createValidDistributionRequestDto() {
            // Given & When
            DistributionRequestDto dto = new DistributionRequestDto(5000, "mykey");

            // Then
            assertThat(dto.getKeyCount()).isEqualTo(5000);
            assertThat(dto.getKeyPrefix()).isEqualTo("mykey");

            // 검증 통과 확인
            Set<ConstraintViolation<DistributionRequestDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("키 개수 제한 테스트")
        void validateKeyCountConstraints() {
            // Given: 너무 적은 키
            DistributionRequestDto tooFew = new DistributionRequestDto(0, "test");

            // When
            Set<ConstraintViolation<DistributionRequestDto>> violations = validator.validate(tooFew);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("키 개수는 최소 1개 이상이어야 합니다");

            // Given: 너무 많은 키
            DistributionRequestDto tooMany = new DistributionRequestDto(2000000, "test");

            // When
            violations = validator.validate(tooMany);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("키 개수는 최대 1,000,000개까지 가능합니다");
        }

        @Test
        @DisplayName("키 접두사 검증 테스트")
        void validateKeyPrefixConstraints() {
            // Given: 빈 접두사 - @NotBlank와 @Size 모두 위반
            DistributionRequestDto emptyPrefix = new DistributionRequestDto(1000, "");

            // When
            Set<ConstraintViolation<DistributionRequestDto>> violations = validator.validate(emptyPrefix);

            // Then: 2개의 검증 오류 (NotBlank + Size)
            assertThat(violations).hasSize(2);

            // 개별 검증 오류 메시지 확인
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toList());
            assertThat(errorMessages).contains("키 접두사는 필수입니다");
            assertThat(errorMessages).contains("키 접두사는 1-20자 사이여야 합니다");

            // Given: null 접두사 - NotBlank만 위반
            DistributionRequestDto nullPrefix = new DistributionRequestDto(1000, null);

            // When
            violations = validator.validate(nullPrefix);

            // Then: 1개의 검증 오류 (NotBlank만)
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("키 접두사는 필수입니다");

            // Given: 너무 긴 접두사 - Size만 위반
            String longPrefix = "a".repeat(21);
            DistributionRequestDto longPrefixDto = new DistributionRequestDto(1000, longPrefix);

            // When
            violations = validator.validate(longPrefixDto);

            // Then: 1개의 검증 오류 (Size만)
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("키 접두사는 1-20자 사이여야 합니다");
        }
    }

    @Nested
    @DisplayName("응답 DTO 테스트")
    class ResponseDtoTest {

        @Test
        @DisplayName("ApiResponseDto 성공 응답 테스트")
        void testApiResponseDtoSuccess() {
            // Given
            String testData = "test data";
            String message = "성공적으로 처리되었습니다";

            // When
            ApiResponseDto<String> response = ApiResponseDto.success(testData, message);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getMessage()).isEqualTo(message);
            assertThat(response.getTimestamp()).isNotNull();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("ApiResponseDto 오류 응답 테스트")
        void testApiResponseDtoError() {
            // Given
            String errorMessage = "처리 중 오류가 발생했습니다";

            // When
            ApiResponseDto<String> response = ApiResponseDto.error(errorMessage);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getData()).isNull();
            assertThat(response.getError()).isEqualTo(errorMessage);
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("NodeLookupResponseDto 테스트")
        void testNodeLookupResponseDto() {
            // Given & When: 찾음
            NodeLookupResponseDto found = NodeLookupResponseDto.found("user123", "server1", "192.168.1.1:8080");

            // Then
            assertThat(found.getKey()).isEqualTo("user123");
            assertThat(found.getNodeId()).isEqualTo("server1");
            assertThat(found.getNodeAddress()).isEqualTo("192.168.1.1:8080");
            assertThat(found.isFound()).isTrue();

            // Given & When: 못찾음
            NodeLookupResponseDto notFound = NodeLookupResponseDto.notFound("user456");

            // Then
            assertThat(notFound.getKey()).isEqualTo("user456");
            assertThat(notFound.getNodeId()).isNull();
            assertThat(notFound.isFound()).isFalse();
        }

        @Test
        @DisplayName("RingInfoResponseDto 계산 필드 테스트")
        void testRingInfoResponseDtoCalculations() {
            // Given
            List<String> nodeList = Arrays.asList("server1", "server2", "server3");
            Map<String, Integer> distribution = new HashMap<>();
            distribution.put("server1", 330);
            distribution.put("server2", 340);
            distribution.put("server3", 330);

            // When
            RingInfoResponseDto response = new RingInfoResponseDto(3, 450, nodeList, distribution);

            // Then
            assertThat(response.getPhysicalNodeCount()).isEqualTo(3);
            assertThat(response.getVirtualNodeCount()).isEqualTo(450);
            assertThat(response.getNodeList()).hasSize(3);
            assertThat(response.getAverageKeysPerNode()).isCloseTo(333.33, within(0.01));
            assertThat(response.getDistributionUniformity()).isGreaterThan(90.0); // 높은 균등성
        }

        @Test
        @DisplayName("DistributionTestResponseDto 통계 계산 테스트")
        void testDistributionTestResponseDtoStatistics() {
            // Given
            Map<String, Integer> distribution = new HashMap<>();
            distribution.put("server1", 2500);
            distribution.put("server2", 2450);
            distribution.put("server3", 2550);
            distribution.put("server4", 2500);

            // When
            DistributionTestResponseDto response = new DistributionTestResponseDto(10000, distribution);

            // Then
            assertThat(response.getTotalKeyCount()).isEqualTo(10000);
            assertThat(response.getNodeDistribution()).hasSize(4);

            // 백분율 확인
            Map<String, Double> percentages = response.getDistributionPercentages();
            assertThat(percentages.get("server1")).isCloseTo(25.0, within(0.1));
            assertThat(percentages.get("server2")).isCloseTo(24.5, within(0.1));

            // 균등성 점수 확인
            assertThat(response.getUniformityScore()).isGreaterThan(90.0);
            assertThat(response.getUniformityGrade()).contains("A");

            // 통계 확인
            DistributionTestResponseDto.TestStatistics stats = response.getStatistics();
            assertThat(stats.getExpectedKeysPerNode()).isCloseTo(2500.0, within(0.1));
            assertThat(stats.getMinKeysPerNode()).isEqualTo(2450);
            assertThat(stats.getMaxKeysPerNode()).isEqualTo(2550);
            assertThat(stats.getImbalanceRatio()).isCloseTo(1.04, within(0.01));
        }
    }

    @Test
    @DisplayName("DTO 직렬화/역직렬화 테스트")
    @Execution(ExecutionMode.SAME_THREAD)
    void testDtoSerialization() {
        // 실제 Spring Boot 환경에서는 Jackson이 자동으로 처리
        // 여기서는 toString 메서드 테스트로 대체

        NodeRequestDto nodeRequest = new NodeRequestDto("server1", "localhost", 8080);
        assertThat(nodeRequest.toString()).contains("server1", "localhost", "8080");

        DistributionRequestDto distRequest = new DistributionRequestDto(1000, "test");
        assertThat(distRequest.toString()).contains("1000", "test");

        NodeLookupResponseDto lookupResponse = NodeLookupResponseDto.found("key1", "server1", "localhost:8080");
        assertThat(lookupResponse.toString()).contains("key1", "server1");

        System.out.println("✅ DTO 문자열 변환 테스트 통과");
    }

    @Test
    @DisplayName("DTO 불변성 및 방어적 복사 테스트")
    void testDtoImmutability() {
        // Given: 원본 데이터 생성 (mutable 컬렉션)
        List<String> originalNodeList = new ArrayList<>();
        originalNodeList.add("server1");
        originalNodeList.add("server2");

        Map<String, Integer> originalDistribution = new HashMap<>();
        originalDistribution.put("server1", 500);
        originalDistribution.put("server2", 500);

        // When: DTO 생성 (이 시점에서 방어적 복사가 일어나야 함)
        RingInfoResponseDto response = new RingInfoResponseDto(2, 300, originalNodeList, originalDistribution);

        // 초기 상태 확인 - DTO가 정상적으로 생성되었는지 확인
        assertThat(response.getNodeList()).hasSize(2);
        assertThat(response.getDataDistribution()).hasSize(2);
        assertThat(response.getNodeList()).containsExactlyInAnyOrder("server1", "server2");

        // Then: 원본 데이터 수정 후 DTO가 영향받지 않는지 테스트 (방어적 복사 검증)
        System.out.println("=== 방어적 복사 테스트 시작 ===");
        System.out.println("수정 전 DTO nodeList 크기: " + response.getNodeList().size());
        System.out.println("수정 전 DTO nodeList 내용: " + response.getNodeList());

        // 원본 컬렉션 수정 (DTO에 영향을 주면 안됨)
        originalNodeList.add("server3");
        originalDistribution.put("server3", 500);

        System.out.println("원본 수정 후 original nodeList 크기: " + originalNodeList.size());
        System.out.println("원본 수정 후 original nodeList 내용: " + originalNodeList);

        List<String> dtoNodeList = response.getNodeList();
        System.out.println("원본 수정 후 DTO nodeList 크기: " + dtoNodeList.size());
        System.out.println("원본 수정 후 DTO nodeList 내용: " + dtoNodeList);

        // 핵심 테스트: DTO는 원본 수정의 영향을 받지 않아야 함
        assertThat(response.getNodeList()).hasSize(2); // 여전히 2개여야 함
        assertThat(response.getDataDistribution()).hasSize(2); // 여전히 2개여야 함
        assertThat(response.getNodeList()).containsExactlyInAnyOrder("server1", "server2"); // 내용도 그대로
        assertThat(response.getNodeList()).doesNotContain("server3"); // 새로 추가된 요소는 없어야 함

        System.out.println("=== Getter 반환값 수정 테스트 시작 ===");

        // 추가 검증: DTO에서 반환된 컬렉션을 수정해도 DTO 내부에 영향 없어야 함
        List<String> returnedList = response.getNodeList();
        Map<String, Integer> returnedMap = response.getDataDistribution();

        System.out.println("Getter 호출로 받은 list 크기: " + returnedList.size());

        // 반환된 컬렉션 수정 시도 (DTO 내부에 영향을 주면 안됨)
        returnedList.add("hacker_node");
        returnedMap.put("hacker_node", 999);

        System.out.println("반환된 컬렉션 수정 후 크기: " + returnedList.size());
        System.out.println("반환된 컬렉션 수정 후 DTO 다시 호출한 크기: " + response.getNodeList().size());

        // DTO 내부는 여전히 안전해야 함 (getter에서도 방어적 복사)
        assertThat(response.getNodeList()).hasSize(2);
        assertThat(response.getDataDistribution()).hasSize(2);
        assertThat(response.getNodeList()).doesNotContain("hacker_node");
        assertThat(response.getDataDistribution()).doesNotContainKey("hacker_node");
        assertThat(response.getNodeList()).containsExactlyInAnyOrder("server1", "server2");

        System.out.println("=== 방어적 복사 테스트 완료 ===");
        System.out.println("✅ DTO 불변성 테스트 통과 - 방어적 복사가 정상 작동");
    }

    @Test
    @DisplayName("DTO 성능 테스트")
    void testDtoPerformance() {
        // Given
        int iterations = 10000;

        // When & Then: 대량 DTO 생성 성능 테스트
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            NodeRequestDto dto = new NodeRequestDto("server" + i, "host" + i, 8080 + i);
            validator.validate(dto); // 검증 포함
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.printf("DTO 성능 테스트: %d개 생성/검증 - %dms (평균 %.3fms/개)%n",
                iterations, duration, duration / (double) iterations);

        // 성능 기준: 평균 0.1ms 미만
        assertThat(duration / (double) iterations).isLessThan(0.1);

        System.out.println("✅ DTO 성능 테스트 통과");
    }
}