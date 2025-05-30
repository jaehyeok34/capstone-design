package team.j.api_gateway.configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import team.j.api_gateway.dto.EventDTO;

@Configuration
public class EventConfig {

    @Bean
    BlockingQueue<EventDTO> eventQueue() {
        return new LinkedBlockingQueue<>();
    }
}
