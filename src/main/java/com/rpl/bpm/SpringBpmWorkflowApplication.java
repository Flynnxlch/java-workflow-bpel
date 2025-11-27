package com.rpl.bpm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.rpl.bpm.repository")
@EntityScan(basePackages = "com.rpl.bpm.entity")
public class SpringBpmWorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBpmWorkflowApplication.class, args);
    }
}

