package com.phcpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MulticoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(MulticoreApplication.class, args);
    }
}
