package com.apptitle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApptitleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApptitleApplication.class, args);
    }
}
