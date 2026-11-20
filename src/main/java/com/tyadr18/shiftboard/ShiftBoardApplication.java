package com.tyadr18.shiftboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShiftBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiftBoardApplication.class, args);
    }
}
