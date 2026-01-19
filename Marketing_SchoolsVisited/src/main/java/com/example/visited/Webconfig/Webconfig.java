package com.example.visited.Webconfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class Webconfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir; // uploads/marketing

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /uploads/marketing/** URLs to the uploadDir folder
        registry.addResourceHandler("/uploads/marketing/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
