package org.example.cafeteria_ordering_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CafeteriaOrderingBackendApplication {


    public static void main(String[] args) {

        SpringApplication.run(CafeteriaOrderingBackendApplication.class, args);
    }
}
