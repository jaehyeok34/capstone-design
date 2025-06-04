package team.j.api_gateway.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LockConfig {
    
    @Bean
    public Object registeredDataTableLock() {
        return new Object() {};
    }

    @Bean
    public Object topicTableLock() {
        return new Object() {};
    }
}
