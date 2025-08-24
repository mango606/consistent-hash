package com.example.consistenthash.service;

import com.example.consistenthash.model.Node;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 안정 해시(Consistent Hashing) 구현 클래스
 * 분산 시스템에서 데이터를 균등하게 분산하고, 노드 추가/제거 시 최소한의 재배치만 발생.
 */
@Component
public class ConsistentHash {

    private static final int DEFAULT_VIRTUAL_NODES = 150; // 각 물리 노드당 가상 노드 수
    private static final String HASH_ALGORITHM = "SHA-1";

    private final int virtualNodesCount;
    private final TreeMap<Long, Node> ring; // 해시 링 (정렬된 맵)
    private final Map<String, Node> nodes; // 물리 노드들
    private final ReadWriteLock lock; // 동시성 제어

    public ConsistentHash() {
        this(DEFAULT_VIRTUAL_NODES);
    }

    public ConsistentHash(int virtualNodesCount) {
        this.virtualNodesCount = virtualNodesCount;
        this.ring = new TreeMap<>();
        this.nodes = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * 노드를 링에 추가
     */
    public void addNode(Node node) {
        lock.writeLock().lock();
        try {
            if (nodes.containsKey(node.getId())) {
                return; // 이미 존재하는 노드
            }

            nodes.put(node.getId(), node);

            // 가상 노드들을 링에 추가
            for (int i = 0; i < virtualNodesCount; i++) {
                String virtualNodeKey = node.getId() + "#" + i;
                long hash = hash(virtualNodeKey);
                ring.put(hash, node);
            }

            System.out.printf("노드 추가됨: %s (가상 노드 %d개)%n",
                    node.getId(), virtualNodesCount);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 노드를 링에서 제거
     */
    public void removeNode(String nodeId) {
        lock.writeLock().lock();
        try {
            Node node = nodes.remove(nodeId);
            if (node == null) {
                return; // 존재하지 않는 노드
            }

            // 해당 노드의 모든 가상 노드를 링에서 제거
            for (int i = 0; i < virtualNodesCount; i++) {
                String virtualNodeKey = nodeId + "#" + i;
                long hash = hash(virtualNodeKey);
                ring.remove(hash);
            }

            System.out.printf("노드 제거됨: %s%n", nodeId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 주어진 키에 대해 담당 노드를 찾음
     */
    public Node getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }

        lock.readLock().lock();
        try {
            long hash = hash(key);

            // 해시 값보다 크거나 같은 첫 번째 항목 찾기 (시계방향)
            Map.Entry<Long, Node> entry = ring.ceilingEntry(hash);

            // 찾지 못하면 링의 첫 번째 노드 (원형이므로)
            if (entry == null) {
                entry = ring.firstEntry();
            }

            return entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 현재 링 상태 정보 반환
     */
    public Map<String, Object> getRingInfo() {
        lock.readLock().lock();
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("물리노드수", nodes.size());
            info.put("가상노드수", ring.size());
            info.put("노드목록", new ArrayList<>(nodes.keySet()));

            // 각 물리 노드의 데이터 분포 계산
            Map<String, Integer> distribution = calculateDistribution();
            info.put("데이터분포", distribution);

            return info;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 테스트용: 샘플 키들에 대한 노드 분포 계산
     */
    private Map<String, Integer> calculateDistribution() {
        Map<String, Integer> distribution = new HashMap<>();

        // 샘플 키 1000개로 분포 테스트
        for (int i = 0; i < 1000; i++) {
            String key = "key_" + i;
            Node node = getNode(key);
            if (node != null) {
                distribution.merge(node.getId(), 1, Integer::sum);
            }
        }

        return distribution;
    }

    /**
     * SHA-1 해시 함수
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] digest = md.digest(key.getBytes());

            // 바이트 배열을 long으로 변환 (첫 8바이트 사용)
            long hash = 0;
            for (int i = 0; i < Math.min(digest.length, 8); i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }

            return Math.abs(hash); // 음수 방지
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시 알고리즘을 찾을 수 없습니다: " + HASH_ALGORITHM, e);
        }
    }

    /**
     * 모든 노드 제거 (테스트용)
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            ring.clear();
            nodes.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 현재 노드 수 반환
     */
    public int getNodeCount() {
        lock.readLock().lock();
        try {
            return nodes.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}