package com.example.consistenthash.demo;

import com.example.consistenthash.model.Node;
import com.example.consistenthash.service.ConsistentHash;

import java.util.*;

/**
 * 안정 해시의 동작을 시연하는 데모 클래스
 * 콘솔에서 실행하여 안정 해시의 핵심 특성들을 확인할 수 있음.
 */
public class ConsistentHashDemo {

    private static final ConsistentHash hash = new ConsistentHash();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("           안정 해시(Consistent Hashing) 데모");
        System.out.println("=".repeat(60));

        // 자동 데모 실행
        runAutomaticDemo();

        // 수동 모드 제공
        runInteractiveMode();
    }

    private static void runAutomaticDemo() {
        System.out.println("\n📋 자동 데모를 시작합니다...\n");

        // 1단계: 초기 노드 추가
        System.out.println("🔹 1단계: 초기 노드 3개 추가");
        hash.addNode(new Node("서울-서버", "seoul.example.com", 8080));
        hash.addNode(new Node("부산-서버", "busan.example.com", 8080));
        hash.addNode(new Node("대전-서버", "daejeon.example.com", 8080));

        printRingStatus();

        // 2단계: 데이터 분산 확인
        System.out.println("\n🔹 2단계: 샘플 데이터 분산 확인");
        Map<String, List<String>> dataDistribution = distributeTestData();
        printDataDistribution(dataDistribution);

        // 3단계: 노드 추가 시 데이터 이동
        System.out.println("\n🔹 3단계: 새 노드 추가 시 데이터 이동 확인");
        System.out.println("광주-서버를 추가합니다...");

        Map<String, String> beforeAddition = captureCurrentMapping();
        hash.addNode(new Node("광주-서버", "gwangju.example.com", 8080));
        Map<String, String> afterAddition = captureCurrentMapping();

        analyzeDataMovement("노드 추가", beforeAddition, afterAddition);

        // 4단계: 노드 제거 시 데이터 이동
        System.out.println("\n🔹 4단계: 노드 제거 시 데이터 이동 확인");
        System.out.println("부산-서버를 제거합니다...");

        Map<String, String> beforeRemoval = captureCurrentMapping();
        hash.removeNode("부산-서버");
        Map<String, String> afterRemoval = captureCurrentMapping();

        analyzeDataMovement("노드 제거", beforeRemoval, afterRemoval);

        // 5단계: 최종 상태
        System.out.println("\n🔹 5단계: 최종 링 상태");
        printRingStatus();

        System.out.println("\n✅ 자동 데모가 완료되었습니다!");
        System.out.println("핵심 특징:");
        System.out.println("  • 데이터가 노드들에 균등하게 분산됨");
        System.out.println("  • 노드 추가/제거 시 최소한의 데이터만 이동");
        System.out.println("  • 가상 노드를 통해 더 균등한 분산 달성");

        pauseForUser();
    }

    private static void runInteractiveMode() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("           대화형 모드 (직접 테스트해보세요!)");
        System.out.println("=".repeat(60));

        while (true) {
            System.out.println("\n명령어를 선택하세요:");
            System.out.println("1. 노드 추가 (add)");
            System.out.println("2. 노드 제거 (remove)");
            System.out.println("3. 키 조회 (lookup)");
            System.out.println("4. 링 상태 (status)");
            System.out.println("5. 분산 테스트 (test)");
            System.out.println("6. 종료 (exit)");
            System.out.print("\n입력: ");

            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "1", "add" -> addNodeInteractive();
                case "2", "remove" -> removeNodeInteractive();
                case "3", "lookup" -> lookupKeyInteractive();
                case "4", "status" -> printRingStatus();
                case "5", "test" -> runDistributionTest();
                case "6", "exit" -> {
                    System.out.println("데모를 종료합니다. 감사합니다!");
                    return;
                }
                default -> System.out.println("잘못된 명령어입니다. 다시 시도해주세요.");
            }
        }
    }

    private static void addNodeInteractive() {
        System.out.print("노드 ID를 입력하세요: ");
        String nodeId = scanner.nextLine().trim();

        if (nodeId.isEmpty()) {
            System.out.println("❌ 노드 ID는 필수입니다.");
            return;
        }

        hash.addNode(new Node(nodeId));
        System.out.println("✅ 노드가 추가되었습니다: " + nodeId);
    }

    private static void removeNodeInteractive() {
        System.out.print("제거할 노드 ID를 입력하세요: ");
        String nodeId = scanner.nextLine().trim();

        if (nodeId.isEmpty()) {
            System.out.println("❌ 노드 ID는 필수입니다.");
            return;
        }

        hash.removeNode(nodeId);
        System.out.println("✅ 노드가 제거되었습니다: " + nodeId);
    }

    private static void lookupKeyInteractive() {
        System.out.print("조회할 키를 입력하세요: ");
        String key = scanner.nextLine().trim();

        if (key.isEmpty()) {
            System.out.println("❌ 키는 필수입니다.");
            return;
        }

        Node node = hash.getNode(key);
        if (node != null) {
            System.out.printf("🎯 키 '%s'의 담당 노드: %s%n", key, node.getId());
        } else {
            System.out.println("❌ 담당 노드를 찾을 수 없습니다 (노드가 없음)");
        }
    }

    private static void runDistributionTest() {
        if (hash.getNodeCount() == 0) {
            System.out.println("❌ 테스트를 위해 먼저 노드를 추가하세요.");
            return;
        }

        System.out.print("테스트할 키 개수를 입력하세요 (기본값: 1000): ");
        String input = scanner.nextLine().trim();

        int keyCount = 1000;
        if (!input.isEmpty()) {
            try {
                keyCount = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("잘못된 숫자 형식입니다. 기본값 1000을 사용합니다.");
            }
        }

        Map<String, Integer> distribution = new HashMap<>();
        for (int i = 0; i < keyCount; i++) {
            String key = "testkey_" + i;
            Node node = hash.getNode(key);
            if (node != null) {
                distribution.merge(node.getId(), 1, Integer::sum);
            }
        }

        final int finalKeyCount = keyCount; // final 변수로 복사
        System.out.printf("\n📊 %d개 키 분산 테스트 결과:%n", finalKeyCount);
        distribution.forEach((nodeId, count) -> {
            double percentage = (count / (double) finalKeyCount) * 100;
            System.out.printf("  %s: %d개 (%.2f%%)%n", nodeId, count, percentage);
        });

        // 균등성 점수 계산
        double expectedPerNode = (double) finalKeyCount / distribution.size();
        double variance = distribution.values().stream()
                .mapToDouble(count -> Math.pow(count - expectedPerNode, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        double uniformityScore = Math.max(0, 100 - (stdDev / expectedPerNode * 100));

        System.out.printf("균등성 점수: %.2f/100 (높을수록 좋음)%n", uniformityScore);
    }

    private static Map<String, List<String>> distributeTestData() {
        String[] testKeys = {
                "user:123", "user:456", "user:789", "user:abc", "user:def",
                "product:laptop", "product:phone", "product:tablet", "product:watch",
                "session:sess_001", "session:sess_002", "session:sess_003",
                "cache:homepage", "cache:profile", "cache:dashboard"
        };

        Map<String, List<String>> distribution = new HashMap<>();

        for (String key : testKeys) {
            Node node = hash.getNode(key);
            if (node != null) {
                distribution.computeIfAbsent(node.getId(), k -> new ArrayList<>()).add(key);
            }
        }

        return distribution;
    }

    private static void printDataDistribution(Map<String, List<String>> distribution) {
        System.out.println("📊 데이터 분산 현황:");
        distribution.forEach((nodeId, keys) -> {
            System.out.printf("  %s (%d개): %s%n",
                    nodeId, keys.size(), String.join(", ", keys));
        });
    }

    private static Map<String, String> captureCurrentMapping() {
        Map<String, String> mapping = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            String key = "sample_" + i;
            Node node = hash.getNode(key);
            if (node != null) {
                mapping.put(key, node.getId());
            }
        }
        return mapping;
    }

    private static void analyzeDataMovement(String operation,
                                            Map<String, String> before,
                                            Map<String, String> after) {
        int movedKeys = 0;
        int totalKeys = before.size();

        for (String key : before.keySet()) {
            String beforeNode = before.get(key);
            String afterNode = after.get(key);

            if (afterNode != null && !beforeNode.equals(afterNode)) {
                movedKeys++;
            }
        }

        double movePercentage = (movedKeys / (double) totalKeys) * 100;

        System.out.printf("📈 %s 영향 분석:%n", operation);
        System.out.printf("  총 키 개수: %d개%n", totalKeys);
        System.out.printf("  이동된 키: %d개%n", movedKeys);
        System.out.printf("  이동 비율: %.2f%%%n", movePercentage);

        if (movePercentage < 30) {
            System.out.println("✅ 최소한의 데이터 이동 - 안정 해시가 정상 작동 중!");
        } else {
            System.out.println("⚠️ 예상보다 많은 데이터 이동 발생");
        }
    }

    private static void printRingStatus() {
        Map<String, Object> ringInfo = hash.getRingInfo();

        System.out.println("\n🔍 현재 링 상태:");
        System.out.println("  물리 노드 수: " + ringInfo.get("물리노드수"));
        System.out.println("  가상 노드 수: " + ringInfo.get("가상노드수"));
        System.out.println("  노드 목록: " + ringInfo.get("노드목록"));

        @SuppressWarnings("unchecked")
        Map<String, Integer> distribution = (Map<String, Integer>) ringInfo.get("데이터분포");
        if (!distribution.isEmpty()) {
            System.out.println("  샘플 분산:");
            distribution.forEach((nodeId, count) ->
                    System.out.printf("    %s: %d개%n", nodeId, count));
        }
    }

    private static void pauseForUser() {
        System.out.println("\nEnter를 눌러 계속...");
        scanner.nextLine();
    }
}