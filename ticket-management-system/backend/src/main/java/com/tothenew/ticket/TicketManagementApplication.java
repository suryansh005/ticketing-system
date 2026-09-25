package com.tothenew.ticket;

import com.tothenew.ticket.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TicketManagementApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(TicketManagementApplication.class, args);
    }
}
