package com.hinadt.miaocha.infrastructure.email;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Email template renderer using Thymeleaf on classpath templates. Keeps rendering generic and
 * business-agnostic.
 */
@Component
public class EmailTemplateRenderer {

    private final TemplateEngine engine;

    public EmailTemplateRenderer() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/email/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    /** Render template by name (without prefix/suffix) */
    public String render(String templateName, Map<String, Object> model) {
        Context ctx = new Context();
        if (model != null) {
            model.forEach(ctx::setVariable);
        }
        return engine.process(templateName, ctx);
    }
}
