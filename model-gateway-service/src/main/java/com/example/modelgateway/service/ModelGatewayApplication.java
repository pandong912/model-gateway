package com.example.modelgateway.service;

import org.mybatis.spring.annotation.MapperScan;
import com.example.modelgateway.service.config.ModelGatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.example.modelgateway.service.persistence.mapper")
@EnableConfigurationProperties(ModelGatewayProperties.class)
public class ModelGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModelGatewayApplication.class, args);
    }
}
