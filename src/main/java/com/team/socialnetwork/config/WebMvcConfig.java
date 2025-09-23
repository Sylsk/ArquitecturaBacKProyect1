package com.team.socialnetwork.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir recursos estáticos de SockJS
        registry.addResourceHandler("/notifications/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
                
        registry.addResourceHandler("/chat/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}