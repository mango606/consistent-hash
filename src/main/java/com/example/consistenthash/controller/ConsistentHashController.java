package com.example.consistenthash.controller;

import com.example.consistenthash.dto.request.DistributionRequestDto;
import com.example.consistenthash.dto.request.NodeRequestDto;
import com.example.consistenthash.dto.response.*;
import com.example.consistenthash.model.Node;
import com.example.consistenthash.service.ConsistentHash;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/consistent-hash")
public class ConsistentHashController {

    private final ConsistentHash consistentHash;

    @Autowired
    public ConsistentHashController(ConsistentHash consistentHash) {
        this.consistentHash = consistentHash;
    }

    /**
     * 노드 추가
     */
    @PostMapping("/nodes")
    public ResponseEntity<ApiResponseDto<Void>> addNode(@Valid @RequestBody NodeRequestDto request) {
        Node node = new Node(request.getId(), request.getHost(), request.getPort());
        consistentHash.addNode(node);

        String message = String.format("노드가 성공적으로 추가되었습니다: %s (%s:%d)",
                request.getId(), request.getHost(), request.getPort());
        return ResponseEntity.ok(ApiResponseDto.success(message));
    }

    /**
     * 노드 제거
     */
    @DeleteMapping("/nodes/{nodeId}")
    public ResponseEntity<ApiResponseDto<Void>> removeNode(@PathVariable String nodeId) {
        consistentHash.removeNode(nodeId);

        String message = String.format("노드가 성공적으로 제거되었습니다: %s", nodeId);
        return ResponseEntity.ok(ApiResponseDto.success(message));
    }

    /**
     * 키에 대한 담당 노드 조회
     */
    @GetMapping("/nodes/lookup/{key}")
    public ResponseEntity<ApiResponseDto<NodeLookupResponseDto>> getNodeForKey(@PathVariable String key) {
        Node node = consistentHash.getNode(key);

        if (node != null) {
            NodeLookupResponseDto responseData = NodeLookupResponseDto.found(
                    key, node.getId(), node.getAddress()
            );
            return ResponseEntity.ok(ApiResponseDto.success(responseData, "담당 노드를 찾았습니다"));
        } else {
            NodeLookupResponseDto responseData = NodeLookupResponseDto.notFound(key);
            return ResponseEntity.ok(ApiResponseDto.success(responseData, "담당 노드를 찾을 수 없습니다 (노드가 없음)"));
        }
    }

    /**
     * 링 상태 정보 조회
     */
    @GetMapping("/ring/info")
    public ResponseEntity<ApiResponseDto<RingInfoResponseDto>> getRingInfo() {
        Map<String, Object> ringInfo = consistentHash.getRingInfo();

        @SuppressWarnings("unchecked")
        RingInfoResponseDto responseData = new RingInfoResponseDto(
                (Integer) ringInfo.get("물리노드수"),
                (Integer) ringInfo.get("가상노드수"),
                (ArrayList<String>) ringInfo.get("노드목록"),
                (Map<String, Integer>) ringInfo.get("데이터분포")
        );

        return ResponseEntity.ok(ApiResponseDto.success(responseData, "링 정보를 조회했습니다"));
    }

    /**
     * 모든 노드 제거 (테스트용)
     */
    @DeleteMapping("/nodes")
    public ResponseEntity<ApiResponseDto<Void>> clearNodes() {
        consistentHash.clear();
        return ResponseEntity.ok(ApiResponseDto.success("모든 노드가 성공적으로 제거되었습니다"));
    }

    /**
     * 여러 키에 대한 노드 분포 테스트
     */
    @PostMapping("/test/distribution")
    public ResponseEntity<ApiResponseDto<DistributionTestResponseDto>> testDistribution(
            @Valid @RequestBody DistributionRequestDto request) {

        Map<String, Integer> nodeCount = new HashMap<>();

        for (int i = 0; i < request.getKeyCount(); i++) {
            String key = request.getKeyPrefix() + "_" + i;
            Node node = consistentHash.getNode(key);

            if (node != null) {
                nodeCount.merge(node.getId(), 1, Integer::sum);
            }
        }

        DistributionTestResponseDto responseData = new DistributionTestResponseDto(
                request.getKeyCount(), nodeCount
        );

        String message = String.format("분산 테스트 완료 - %d개 키를 %d개 노드에 분산 (균등성 점수: %.2f)",
                request.getKeyCount(), nodeCount.size(), responseData.getUniformityScore());

        return ResponseEntity.ok(ApiResponseDto.success(responseData, message));
    }
}