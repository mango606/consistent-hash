package com.example.consistenthash.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDto<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String error;

    // 기본 생성자
    public ApiResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    // 성공 응답 생성자
    public ApiResponseDto(T data, String message) {
        this();
        this.success = true;
        this.data = data;
        this.message = message;
    }

    // 단순 성공 응답
    public ApiResponseDto(String message) {
        this();
        this.success = true;
        this.message = message;
    }

    // 오류 응답 생성자
    public ApiResponseDto(String error, boolean isError) {
        this();
        this.success = false;
        this.error = error;
    }

    // 정적 팩토리 메서드들
    public static <T> ApiResponseDto<T> success(T data, String message) {
        return new ApiResponseDto<>(data, message);
    }

    public static <T> ApiResponseDto<T> success(T data) {
        return new ApiResponseDto<>(data, "요청이 성공적으로 처리되었습니다");
    }

    public static ApiResponseDto<Void> success(String message) {
        return new ApiResponseDto<>(message);
    }

    public static <T> ApiResponseDto<T> error(String errorMessage) {
        return new ApiResponseDto<>(errorMessage, true);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}