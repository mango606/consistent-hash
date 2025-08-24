package com.example.consistenthash.service;

import com.example.consistenthash.model.Node;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsistentHashTest {

    private ConsistentHash consistentHash;

    @BeforeEach
    void setUp() {
        consistentHash = new ConsistentHash();
    }

    @AfterEach
    void tearDown() {
        consistentHash.clear();
    }

    @Test
    @DisplayName("기본 노드 추가 및 조회 테스트")
    void testBasicNodeOperations() {
        // Given
        Node node1 = new Node("server1", "192.168.1.1", 8080);
        Node node2 = new Node("server2", "192.168.1.2", 8080);

        // When
        consistentHash.addNode(node1);
        consistentHash.addNode(node2);

        // Then
        assertThat(consistentHash.getNodeCount()).isEqualTo(2);

        // 키들이 적절히 분산되는지 확인
        Node nodeForKey1 = consistentHash.getNode("user123");
        Node nodeForKey2 = consistentHash.getNode("product456");

        assertThat(nodeForKey1).isNotNull();
        assertThat(nodeForKey2).isNotNull();
        assertThat(nodeForKey1.getId()).isIn("server1", "server2");
        assertThat(nodeForKey2.getId()).isIn("server1", "server2");

        System.out.println("✅ 기본 노드 작업 테스트 통과");
    }

    @Test
    @DisplayName("노드 제거 테스트")
    void testNodeRemoval() {
        // Given
        Node node1 = new Node("server1");
        Node node2 = new Node("server2");
        Node node3 = new Node("server3");

        consistentHash.addNode(node1);
        consistentHash.addNode(node2);
        consistentHash.addNode(node3);

        // When
        consistentHash.removeNode("server2");

        // Then
        assertThat(consistentHash.getNodeCount()).isEqualTo(2);

        // 제거된 노드는 더 이상 반환되지 않아야 함
        for (int i = 0; i < 100; i++) {
            Node node = consistentHash.getNode("testkey" + i);
            assertThat(node.getId()).isNotEqualTo("server2");
        }

        System.out.println("✅ 노드 제거 테스트 통과");
    }

    @Test
    @DisplayName("데이터 분산 균등성 테스트")
    void testDataDistribution() {
        // Given: 4개 노드 추가
        for (int i = 1; i <= 4; i++) {
            consistentHash.addNode(new Node("server" + i));
        }

        // When: 10000개의 키 분산 테스트
        Map<String, Integer> distribution = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            String key = "key_" + i;
            Node node = consistentHash.getNode(key);
            distribution.merge(node.getId(), 1, Integer::sum);
        }

        // Then: 각 노드가 20% ~ 30% 범위의 데이터를 가져야 함 (이론적으로는 25%)
        System.out.println("데이터 분산 결과:");
        distribution.forEach((nodeId, count) -> {
            double percentage = (count / 10000.0) * 100;
            System.out.printf("  %s: %d개 (%.2f%%)%n", nodeId, count, percentage);
            assertThat(percentage).isBetween(20.0, 30.0);
        });

        // 표준편차 계산으로 균등성 확인
        double average = 2500.0; // 10000 / 4
        double variance = distribution.values().stream()
                .mapToDouble(count -> Math.pow(count - average, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        System.out.printf("표준편차: %.2f (낮을수록 균등함)%n", stdDev);
        assertThat(stdDev).isLessThan(500); // 표준편차가 500 미만이어야 함

        System.out.println("✅ 데이터 분산 균등성 테스트 통과");
    }

    @Test
    @DisplayName("노드 추가 시 최소 데이터 이동 테스트")
    void testMinimalRehashingOnNodeAddition() {
        // Given: 3개 노드로 시작
        for (int i = 1; i <= 3; i++) {
            consistentHash.addNode(new Node("server" + i));
        }

        // 1000개 키의 초기 배치 기록
        Map<String, String> initialMapping = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String key = "key_" + i;
            Node node = consistentHash.getNode(key);
            initialMapping.put(key, node.getId());
        }

        // When: 새 노드 추가
        consistentHash.addNode(new Node("server4"));

        // Then: 새로운 배치 확인
        int movedKeys = 0;
        for (int i = 0; i < 1000; i++) {
            String key = "key_" + i;
            Node newNode = consistentHash.getNode(key);
            if (!newNode.getId().equals(initialMapping.get(key))) {
                movedKeys++;
            }
        }

        double movePercentage = (movedKeys / 1000.0) * 100;
        System.out.printf("노드 추가 후 이동된 키: %d개 (%.2f%%)%n", movedKeys, movePercentage);

        // 이론적으로는 25% (1/4) 정도만 이동되어야 함
        assertThat(movePercentage).isLessThan(35.0); // 여유있게 35% 미만
        assertThat(movePercentage).isGreaterThan(15.0); // 너무 적으면 이상함

        System.out.println("✅ 최소 데이터 이동 테스트 통과");
    }

    @Test
    @DisplayName("노드 제거 시 최소 데이터 이동 테스트")
    void testMinimalRehashingOnNodeRemoval() {
        // Given: 4개 노드로 시작
        for (int i = 1; i <= 4; i++) {
            consistentHash.addNode(new Node("server" + i));
        }

        // 1000개 키의 초기 배치 기록
        Map<String, String> initialMapping = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String key = "key_" + i;
            Node node = consistentHash.getNode(key);
            initialMapping.put(key, node.getId());
        }

        // When: 노드 하나 제거
        consistentHash.removeNode("server2");

        // Then: 새로운 배치 확인
        int movedKeys = 0;
        for (int i = 0; i < 1000; i++) {
            String key = "key_" + i;
            Node newNode = consistentHash.getNode(key);
            String initialNodeId = initialMapping.get(key);

            // server2에 있던 키들은 당연히 이동, 다른 노드의 키들은 이동하면 안됨
            if (initialNodeId.equals("server2")) {
                // server2에 있던 키는 다른 노드로 이동되어야 함
                assertThat(newNode.getId()).isNotEqualTo("server2");
            } else {
                // 다른 노드에 있던 키는 그대로 있어야 함
                if (!newNode.getId().equals(initialNodeId)) {
                    movedKeys++;
                }
            }
        }

        System.out.printf("노드 제거 후 불필요하게 이동된 키: %d개%n", movedKeys);

        // server2에 있지 않던 키들은 이동하면 안됨 (소수는 허용)
        assertThat(movedKeys).isLessThan(50); // 5% 미만

        System.out.println("✅ 노드 제거 시 최소 데이터 이동 테스트 통과");
    }

    @Test
    @DisplayName("동시성 테스트")
    @Execution(ExecutionMode.SAME_THREAD)
    void testConcurrency() throws InterruptedException {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(100);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        // When: 동시에 노드 추가/제거/조회 작업 수행
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    if (taskId % 3 == 0) {
                        // 노드 추가
                        consistentHash.addNode(new Node("concurrent_node_" + taskId));
                    } else if (taskId % 3 == 1 && taskId > 10) {
                        // 노드 제거 (일부만)
                        consistentHash.removeNode("concurrent_node_" + (taskId - 10));
                    } else {
                        // 키 조회
                        consistentHash.getNode("concurrent_key_" + taskId);
                    }
                } catch (Throwable e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(exceptions).isEmpty();

        executor.shutdown();
        System.out.println("✅ 동시성 테스트 통과");
    }

    @Test
    @DisplayName("성능 테스트")
    void testPerformance() {
        // Given: 100개 노드 추가
        long startTime = System.currentTimeMillis();
        for (int i = 1; i <= 100; i++) {
            consistentHash.addNode(new Node("perf_server_" + i));
        }
        long addTime = System.currentTimeMillis() - startTime;

        // When: 100,000번 키 조회
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            consistentHash.getNode("perf_key_" + i);
        }
        long lookupTime = System.currentTimeMillis() - startTime;

        // Then
        System.out.printf("성능 테스트 결과:%n");
        System.out.printf("  노드 100개 추가 시간: %dms%n", addTime);
        System.out.printf("  키 100,000개 조회 시간: %dms%n", lookupTime);
        System.out.printf("  평균 조회 시간: %.3fms%n", lookupTime / 100000.0);

        // 성능 기준: 노드 추가는 1초 미만, 조회는 평균 0.1ms 미만
        assertThat(addTime).isLessThan(1000);
        assertThat(lookupTime / 100000.0).isLessThan(0.1);

        System.out.println("✅ 성능 테스트 통과");
    }

    @Test
    @DisplayName("가상 노드 효과 테스트")
    void testVirtualNodesEffect() {
        // 가상 노드 수가 적은 경우
        ConsistentHash lowVirtualNodes = new ConsistentHash(10);
        ConsistentHash highVirtualNodes = new ConsistentHash(200);

        // 각각 5개 노드 추가
        for (int i = 1; i <= 5; i++) {
            lowVirtualNodes.addNode(new Node("server" + i));
            highVirtualNodes.addNode(new Node("server" + i));
        }

        // 분산도 측정 (1000개 키)
        double lowVariance = calculateDistributionVariance(lowVirtualNodes);
        double highVariance = calculateDistributionVariance(highVirtualNodes);

        System.out.printf("가상 노드 효과 테스트:%n");
        System.out.printf("  가상 노드 10개: 분산 %.2f%n", lowVariance);
        System.out.printf("  가상 노드 200개: 분산 %.2f%n", highVariance);

        // 가상 노드가 많을수록 분산이 더 균등해야 함
        assertThat(highVariance).isLessThan(lowVariance);

        System.out.println("✅ 가상 노드 효과 테스트 통과");
    }

    private double calculateDistributionVariance(ConsistentHash hash) {
        Map<String, Integer> distribution = new HashMap<>();

        for (int i = 0; i < 1000; i++) {
            Node node = hash.getNode("testkey_" + i);
            if (node != null) {
                distribution.merge(node.getId(), 1, Integer::sum);
            }
        }

        double average = 200.0; // 1000 / 5
        return distribution.values().stream()
                .mapToDouble(count -> Math.pow(count - average, 2))
                .average()
                .orElse(0.0);
    }

    @Test
    @DisplayName("엣지 케이스 테스트")
    void testEdgeCases() {
        // 빈 링에서 조회
        assertThat(consistentHash.getNode("any_key")).isNull();

        // 동일한 노드 중복 추가
        Node node1 = new Node("duplicate_test");
        consistentHash.addNode(node1);
        consistentHash.addNode(node1); // 중복 추가
        assertThat(consistentHash.getNodeCount()).isEqualTo(1);

        // 존재하지 않는 노드 제거
        consistentHash.removeNode("non_existent");
        assertThat(consistentHash.getNodeCount()).isEqualTo(1);

        // null 키 조회 (예외 처리 확인)
        assertThatThrownBy(() -> consistentHash.getNode(null))
                .isInstanceOf(Exception.class);

        System.out.println("✅ 엣지 케이스 테스트 통과");
    }

    @Test
    @DisplayName("링 정보 조회 테스트")
    void testRingInfo() {
        // Given
        for (int i = 1; i <= 3; i++) {
            consistentHash.addNode(new Node("info_server_" + i));
        }

        // When
        Map<String, Object> ringInfo = consistentHash.getRingInfo();

        // Then
        assertThat(ringInfo.get("물리노드수")).isEqualTo(3);
        assertThat(ringInfo.get("가상노드수")).isEqualTo(450); // 3 * 150
        assertThat(ringInfo.get("노드목록")).isInstanceOf(List.class);
        assertThat(ringInfo.get("데이터분포")).isInstanceOf(Map.class);

        System.out.println("링 정보: " + ringInfo);
        System.out.println("✅ 링 정보 조회 테스트 통과");
    }
}