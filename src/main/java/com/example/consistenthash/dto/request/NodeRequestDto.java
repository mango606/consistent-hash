package com.example.consistenthash.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 노드 추가 요청을 위한 DTO
 */
public class NodeRequestDto {

    @NotBlank(message = "노드 ID는 필수입니다")
    @Size(min = 1, max = 50, message = "노드 ID는 1-50자 사이여야 합니다")
    private String id;

    @Size(max = 100, message = "호스트명은 100자를 초과할 수 없습니다")
    private String host = "localhost";

    @Positive(message = "포트는 양수여야 합니다")
    private int port = 8080;

    // 기본 생성자
    public NodeRequestDto() {}

    // 전체 생성자
    public NodeRequestDto(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    // 편의 생성자
    public NodeRequestDto(String id) {
        this(id, "localhost", 8080);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return String.format("NodeRequestDto{id='%s', host='%s', port=%d}", id, host, port);
    }
}