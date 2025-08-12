package com.hinadt.miaocha.infrastructure.email;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Email template renderer using Thymeleaf on classpath templates. Keeps rendering generic and
 * business-agnostic.
 */
@Component
public class EmailTemplateRenderer {

    private final TemplateEngine engine;

    public EmailTemplateRenderer(TemplateEngine engine) {
        this.engine = engine;
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
