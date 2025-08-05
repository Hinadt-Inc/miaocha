package com.hinadt.miaocha.config;

import com.hinadt.miaocha.spi.validation.SpiProviderIdValidator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ProviderValidationInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        new SpiProviderIdValidator().validateProviderIds();
    }
}
