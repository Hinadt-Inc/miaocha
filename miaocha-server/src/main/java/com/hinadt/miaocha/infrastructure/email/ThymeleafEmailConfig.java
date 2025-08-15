package com.hinadt.miaocha.infrastructure.email;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ThymeleafEmailConfig {

    @Bean
    public ClassLoaderTemplateResolver emailTemplateResolver() {
        ClassLoaderTemplateResolver r = new ClassLoaderTemplateResolver();
        r.setPrefix("templates/email/"); // classpath:/templates/email/
        r.setSuffix(".html");
        r.setTemplateMode(TemplateMode.HTML);
        r.setCharacterEncoding("UTF-8");
        r.setCacheable(false);
        r.setOrder(1);
        r.setCheckExistence(true);
        return r;
    }
}
