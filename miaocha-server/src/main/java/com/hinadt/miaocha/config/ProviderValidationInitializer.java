package com.hinadt.miaocha.config;

import com.hinadt.miaocha.spi.validation.SpiProviderIdValidator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ProviderValidationInitializer {

    @Bean
    public CommandLineRunner validateProviders() {
        return args -> {
            try {
                new SpiProviderIdValidator().validateProviderIds();
            } catch (IllegalStateException e) {
                System.exit(1);
            }
        };
    }
}
