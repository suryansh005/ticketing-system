package com.tothenew.ticket.config;

import com.tothenew.ticket.domain.TicketStateMachine;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TicketStateMachine ticketStateMachine(Clock clock) {
        return new TicketStateMachine(clock);
    }
}
