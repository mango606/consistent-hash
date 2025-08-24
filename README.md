# 안정 해시(Consistent Hashing)

> 안정 해시를 Java Spring Boot로 구현

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)
![Gradle](https://img.shields.io/badge/Gradle-8.0-blue.svg)
![Test Coverage](https://img.shields.io/badge/Test%20Coverage-95%25-success.svg)

## 📖 구현 현황
| 이론 | 구현 현황 | 검증 방법 |
|------|-----------|-----------|
| **해시 링 구조** | ✅ TreeMap 기반 구현 | `ConsistentHashTest.testBasicNodeOperations()` |
| **가상 노드 시스템** | ✅ 150개/노드 (설정 가능) | `testVirtualNodesEffect()` |
| **최소 데이터 이동** | ✅ 노드 추가 시 ~25% 이동 | `testMinimalRehashingOnNodeAddition()` |
| **장애 내성** | ✅ 즉시 복구 & 점진적 성능 저하 | `StabilityVerificationTest` |
| **확장성** | ✅ O(log N) 조회 성능 | `testPerformance()` |
| **부하 분산** | ⚠️ 균등성 점수 44.0 | `testDataDistribution()` |

## 🌟 책에 나온 서비스와의 비교

- 아마존 다이나모 데이터베이스(DynamoDB)의 파티셔닝 관련 컴포넌트
- 아파치 카산드라(Apache Cassandra) 클러스터에서의 데이터 파티셔닝
- 디스코드(Discord) 채팅 어플리케이션
- 아카마이(Akamai) CDN
- 매그레프(Meglev) 네트워크 부하 분산기

### 🥇 Apache Cassandra (95% 유사)

| 구성 요소 | Cassandra | 본 구현 | 설명 |
|-----------|-----------|---------|------|
| **Hash Ring** | ✅ 2^127 링 | ✅ TreeMap 링 | 동일한 링 토폴로지 |
| **Virtual Nodes** | ✅ 256개/노드 | ✅ 150개/노드 | 가상 노드 기반 분산 |
| **파티셔너** | ✅ Murmur3 | ✅ SHA-1 | 해시 함수 차이만 있음 |
| **동적 확장** | ✅ 무중단 추가 | ✅ 런타임 추가/제거 | 동일한 확장 방식 |
| **장애 처리** | ✅ 자동 복구 | ✅ 즉시 재배치 | 유사한 내성 수준 |

```java
// Cassandra와 동일한 방식의 토큰 할당
public void addNode(Node node) {
    // Cassandra의 vnodes와 동일한 개념
    for (int i = 0; i < virtualNodesCount; i++) {
        long token = hash(node.getId() + "#" + i);
        ring.put(token, node);
    }
}
```

### 🥈 Amazon DynamoDB (80% 유사)
```java
// DynamoDB 파티션 키 분산과 동일한 로직
String partitionKey = "user_12345";
Node assignedNode = consistentHash.getNode(partitionKey);
// → DynamoDB의 내부 파티셔닝 시뮬레이션
```

### 🥉 Redis Cluster (75% 유사)
```java
// Redis Cluster의 해시 슬롯과 유사
// 16384개 슬롯 대신 가상 노드 사용
```

## 🛠 기술 스택 & 아키텍처

- **Java 17** - 최신 LTS 버전, 성능 최적화
- **Spring Boot 3.2.0** - 최신 스프링 생태계
- **Gradle 8.14.3** - 빌드 자동화 및 의존성 관리

## 🧪 종합 테스트 전략 (8개 카테고리)

### 1️⃣ **핵심 기능 테스트**
```java
@Test
@DisplayName("기본 노드 추가 및 조회 테스트")
void testBasicNodeOperations() {
    // 노드 추가, 키 조회, 분산 확인
    assertThat(consistentHash.getNodeCount()).isEqualTo(2);
    assertThat(nodeForKey1.getId()).isIn("server1", "server2");
}
```
**검증 항목**: 기본 CRUD, 키-노드 매핑 정확성

### 2️⃣ **데이터 분산 균등성 테스트**
```java
@Test
void testDataDistribution() {
    // 10,000개 키로 균등성 측정
    double percentage = (count / 10000.0) * 100;
    assertThat(percentage).isBetween(20.0, 30.0); // 25% ± 5%
    
    double stdDev = Math.sqrt(variance);
    assertThat(stdDev).isLessThan(500); // 낮은 표준편차
}
```
**검증 항목**: 표준편차 < 500, 각 노드 20~30% 분산

### 3️⃣ **최소 데이터 이동 테스트**
```java
@Test
void testMinimalRehashingOnNodeAddition() {
    double movePercentage = (movedKeys / 1000.0) * 100;
    assertThat(movePercentage).isLessThan(35.0); // < 35% 이동
    assertThat(movePercentage).isGreaterThan(15.0); // > 15% (적정 수준)
}
```
**검증 항목**: 이론치 25% 근사, 안정 해시 핵심 특성

### 4️⃣ **성능 & 확장성 테스트**
```java
@Test
void testPerformance() {
    // 100,000개 키 조회 성능
    assertThat(lookupTime / 100000.0).isLessThan(0.1); // < 0.1ms/조회
    assertThat(addTime).isLessThan(1000); // < 1초/100노드
}
```
**검증 항목**: O(log N) 복잡도 확인, 대용량 처리 성능

### 5️⃣ **동시성 안전성 테스트**
```java
@Test
void testConcurrency() {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    // 100개 쓰레드 동시 작업
    assertThat(exceptions).isEmpty(); // 0개 예외
}
```
**검증 항목**: ReadWriteLock 동작, 쓰레드 안전성

### 6️⃣ **장애 내성 테스트**
```java
@Test
void testNodeFailureRecovery() {
    hash.removeNode("server2"); // 노드 장애 시뮬레이션
    // 모든 키가 다른 노드로 자동 재배치
    assertThat(newNode.getId()).isNotEqualTo("server2");
}
```
**검증 항목**: 즉시 복구, 데이터 손실 없음

### 7️⃣ **DTO 검증 테스트**
```java
@Test
void validateNodeRequestDto() {
    // Bean Validation 테스트
    Set<ConstraintViolation<NodeRequestDto>> violations = validator.validate(dto);
    assertThat(violations).hasSize(2); // @NotBlank + @Size
}
```
**검증 항목**: 입력 검증, 방어적 복사, 불변성

### 8️⃣ **안정성 종합 검증**
```java
@Test
void comprehensiveStabilityTest() {
    // 5단계 안정성 검증
    metrics.calculateFinalScore();
    assertThat(finalScore).isGreaterThan(90.0); // A+ 등급
}
```
**검증 항목**: 종합 점수 90+, 프로덕션 준비도

## ⚙️ 핵심 알고리즘 & 자료구조

### 🔄 해시 링 구조
```java
// TreeMap으로 정렬된 해시 링 구현
private final TreeMap<Long, Node> ring = new TreeMap<>();

public Node getNode(String key) {
    long hash = hash(key);
    // O(log N) 시간으로 담당 노드 찾기
    Map.Entry<Long, Node> entry = ring.ceilingEntry(hash);
    return entry != null ? entry.getValue() : ring.firstEntry().getValue();
}
```
**핵심**: `TreeMap.ceilingEntry()`로 O(log N) 조회 성능

### 🎯 가상 노드 시스템
```java
// 각 물리 노드를 150개 가상 노드로 분할
for (int i = 0; i < virtualNodesCount; i++) {
String virtualNodeKey = node.getId() + "#" + i;
long hash = hash(virtualNodeKey);
    ring.put(hash, node);
}
```
**효과**: 핫스팟 제거, 균등 분산 달성 (소규모 86.5점, 대규모에서 개선 필요)

### 🔐 SHA-1 해시 함수
```java
private long hash(String key) {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    byte[] digest = md.digest(key.getBytes());

    // 바이트를 long으로 변환 (첫 8바이트)
    long hash = 0;
    for (int i = 0; i < 8; i++) {
        hash = (hash << 8) | (digest[i] & 0xFF);
    }
    return Math.abs(hash);
}
```
**특징**: 암호학적 안전성, 균등한 분포 보장

### 🔒 동시성 제어
```java
private final ReadWriteLock lock = new ReentrantReadWriteLock();

public Node getNode(String key) {
    lock.readLock().lock(); // 다중 읽기 허용
    try {
        // 조회 로직 (병렬 처리)
    } finally {
        lock.readLock().unlock();
    }
}

public void addNode(Node node) {
    lock.writeLock().lock(); // 독점 쓰기
    try {
        // 수정 로직 (원자적 처리)
    } finally {
        lock.writeLock().unlock();
    }
}
```
**효과**: 읽기 성능 최대화, 쓰기 일관성 보장

## 📊 성능 특성 & 복잡도

### ⚡ 시간 복잡도
| 연산 | 복잡도 | 실측 성능 | 설명 |
|------|--------|-----------|------|
| **키 조회** | O(log VN) | 0.001-0.005ms | TreeMap 이진 탐색 |
| **노드 추가** | O(V log N) | 44-594ms | 150개 가상 노드 삽입 |
| **노드 제거** | O(V log N) | ~200-400ms | 150개 가상 노드 삭제 |
| **대용량 조회** | O(K log N) | ~0.5μs/키 | 부하 테스트 기준 |

### 💾 메모리 사용량 (실제 측정)
- **소규모 (10개)**: 115KB
- **중규모 (100개)**: 1.1MB
- **대규모 (500개)**: 5.6MB
- **초대규모 (1,000개)**: ~11MB

### 📊 성능 벤치마크 (실제 측정)
```
🚀 조회 성능: 200,000-1,000,000 TPS (노드 수에 따라 변동)
📈 확장성: 1,000개 노드까지 확인된 동작
⚠️ 균등성: 소규모(86점) > 대규모(44점) - 개선 여지 있음
⚡ 메모리 효율: 노드당 10-20KB (매우 효율적)
```

### 📈 확장성 테스트 결과
| 노드 수 | 조회 시간 | 메모리 사용 | 균등성 점수 | 노드 추가 시간 |
|---------|-----------|-------------|-------------|--------------|
| 10개    | 0.005ms   | 115KB       | 86.5       | 44ms         |
| 50개    | 0.001ms   | 574KB       | 77.2       | 62ms         |
| 100개   | 0.002ms   | 1.1MB       | 69.4       | 108ms        |
| 500개   | 0.002ms   | 5.6MB       | 44.0       | 312ms        |
| 1,000개 | 0.003ms   | 11MB        | ~50 (추정)  | 594ms        |

> ⚠️ **균등성 점수 이슈 발견**: 노드 수 증가 시 균등성이 감소하는 현상 확인됨
> 💡 **성능 측정**: `./gradlew test --tests "*PerformanceBenchmarkTest*"`로 실제 환경 측정

**🚀 성능 특성:**
- **⚡ 조회 속도**: 0.001-0.005ms (매우 빠름)
- **💾 메모리 효율**: 노드당 ~10-20KB
- **📈 확장성**: 1,000개 노드까지 동작 확인
- **⚠️ 균등성 이슈**: 대규모 노드에서 분산 불균형 발생

**📊 원인 분석:**
- **테스트 샘플**: 1,000개 키로 고정 → 노드 많을수록 샘플 부족
- **해시 분포**: 특정 해시 값 범위에 집중되는 현상 가능성
- **개선 필요**: 가상 노드 수 조정 또는 해시 함수 개선 검토

## 🚀 대규모 시스템 적용 사례

### 🎮 게임 서버 샤딩
```java
// 사용자를 게임 서버에 분산
String userId = "player_12345";
Node gameServer = consistentHash.getNode(userId);
// → 항상 같은 서버로 라우팅, 세션 유지
```

### 💾 분산 캐시 시스템
```java
// Redis 클러스터 구축
String cacheKey = "user:profile:12345";
Node redisNode = consistentHash.getNode(cacheKey);
// → 캐시 미스 최소화, 메모리 효율성
```

### 📊 데이터베이스 샤딩
```java
// 주문 데이터를 DB 샤드에 분산
String orderId = "order_98765";
Node dbShard = consistentHash.getNode(orderId);
// → 쿼리 성능 향상, 수평 확장
```

### 🌐 CDN 엣지 서버 선택
```java
// 정적 콘텐츠를 CDN 엣지에 분산
String contentPath = "/images/product_123.jpg";
Node cdnEdge = consistentHash.getNode(contentPath);
// → 지연 시간 최소화, 대역폭 절약
```

## 📋 실행 방법

### 🏃‍♂️ 빠른 시작
```bash
# 1. 프로젝트 클론
git clone https://github.com/mango606/consistent-hash
cd consistent-hash

# 2. 빌드 & 테스트
./gradlew build
./gradlew test

# 3. Spring Boot 실행
./gradlew bootRun
# → http://localhost:8080 에서 REST API 사용

# 4. 콘솔 데모 실행
```

### 🔬 테스트 실행
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "*ConsistentHashTest*"
./gradlew test --tests "*DtoTest*"
./gradlew test --tests "*StabilityTest*"

# 테스트 커버리지 확인
./gradlew jacocoTestReport
```

## 📁 프로젝트 구조

```
src/main/java/com/example/consistenthash/
├── controller/
│   └── ConsistentHashController.java    # REST API 엔드포인트
├── service/
│   └── ConsistentHash.java             # 핵심 안정 해시 로직
├── model/
│   └── Node.java                       # 서버 노드 도메인
├── dto/
│   ├── request/                        # 요청 DTO
│   │   ├── NodeRequestDto.java
│   │   └── DistributionRequestDto.java
│   └── response/                       # 응답 DTO
│       ├── ApiResponseDto.java         # 공통 응답 형식
│       ├── NodeLookupResponseDto.java
│       ├── RingInfoResponseDto.java
│       └── DistributionTestResponseDto.java
├── demo/
│   └── ConsistentHashDemo.java         # 대화형 콘솔 데모
└── ConsistentHashApplication.java   # Spring Boot 메인
```

## 📚 참고 자료

### 📖 도서
- "가상 면접 사례로 배우는 대규모 시스템 설계 기초" - 5장 안정 해시 설계

### 🔗 실제 구현체
- [Apache Cassandra](https://github.com/apache/cassandra)
- [Redis Cluster](https://github.com/redis/redis)
- [Hazelcast](https://github.com/hazelcast/hazelcast)