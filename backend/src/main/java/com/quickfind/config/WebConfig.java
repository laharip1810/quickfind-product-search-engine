package com.quickfind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS for running the Vite dev server against the API directly. In Docker the frontend's
 * nginx proxies /api, so browser requests are same-origin and CORS is not involved.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebConfig(QuickFindProperties properties) {
        this.allowedOrigins = properties.cors() == null || properties.cors().allowedOrigins() == null
                ? List.of()
                : properties.cors().allowedOrigins().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location", RequestLoggingFilter.REQUEST_ID_HEADER)
                .maxAge(3600);
    }
}
