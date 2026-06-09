package com.example.modelgateway.dbmigration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ModelGatewayDbMigrationApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ModelGatewayDbMigrationApplication.class, args);
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }
}
