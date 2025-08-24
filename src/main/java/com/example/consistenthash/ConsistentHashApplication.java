package com.example.consistenthash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConsistentHashApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsistentHashApplication.class, args);
        System.out.println("===================================");
        System.out.println("안정 해시 시스템이 시작되었습니다!");
        System.out.println("API 테스트: http://localhost:8080");
        System.out.println("===================================");
    }
}