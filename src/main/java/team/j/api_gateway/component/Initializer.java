package team.j.api_gateway.component;

import java.io.File;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class Initializer {
    
    @PostConstruct
    public void mkdir() {
        new File("resources").mkdirs();
    }
}
