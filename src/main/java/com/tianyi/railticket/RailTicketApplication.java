package com.tianyi.railticket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RailTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(RailTicketApplication.class, args);
    }
}
