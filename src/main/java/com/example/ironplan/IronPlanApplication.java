package com.example.ironplan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
public class IronPlanApplication {

    public static void main(String[] args) {
        SpringApplication.run(IronPlanApplication.class, args);
    }
}
