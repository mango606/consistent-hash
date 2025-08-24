package com.example.consistenthash.dto.response;

/**
 * 키에 대한 노드 조회 결과 응답 DTO
 */
public class NodeLookupResponseDto {

    private String key;
    private String nodeId;
    private String nodeAddress;
    private boolean found;

    // 기본 생성자
    public NodeLookupResponseDto() {}

    // 성공 응답 생성자
    public NodeLookupResponseDto(String key, String nodeId, String nodeAddress) {
        this.key = key;
        this.nodeId = nodeId;
        this.nodeAddress = nodeAddress;
        this.found = true;
    }

    // 실패 응답 생성자 (노드를 찾지 못한 경우)
    public NodeLookupResponseDto(String key) {
        this.key = key;
        this.found = false;
    }

    // 정적 팩토리 메서드들
    public static NodeLookupResponseDto found(String key, String nodeId, String nodeAddress) {
        return new NodeLookupResponseDto(key, nodeId, nodeAddress);
    }

    public static NodeLookupResponseDto notFound(String key) {
        return new NodeLookupResponseDto(key);
    }

    // Getters and Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    @Override
    public String toString() {
        if (found) {
            return String.format("NodeLookupResponseDto{key='%s', nodeId='%s', nodeAddress='%s'}",
                    key, nodeId, nodeAddress);
        } else {
            return String.format("NodeLookupResponseDto{key='%s', found=false}", key);
        }
    }
}