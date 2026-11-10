package io.batchstats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BatchStatsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchStatsApplication.class, args);
    }
}
