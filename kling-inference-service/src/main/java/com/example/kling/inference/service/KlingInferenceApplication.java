package com.example.kling.inference.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.kling.inference")
public class KlingInferenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KlingInferenceApplication.class, args);
    }
}
