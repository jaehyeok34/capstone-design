package capstone.design.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import capstone.design.client.Consumer;
import capstone.design.client.Producer;


@Configuration
public class BrokerClientConfig {

    private static final String BROKER_HOST = "localhost";
    private static final int BROKER_PORT = 3401;
    private static final String ID = "api_gateway";
    private static final String MAPPING_FILE_PATH = "option_mapping_table.properties";

    @Bean
    public Producer producer() throws Exception {
        return new Producer(BROKER_HOST, BROKER_PORT, ID, MAPPING_FILE_PATH);
    }

    @Bean
    public Consumer consumer() throws Exception {
        return new Consumer(BROKER_HOST, BROKER_PORT, ID, MAPPING_FILE_PATH);
    }
}
