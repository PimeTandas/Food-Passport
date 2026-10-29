package com.foodpassport.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * In production the React build is copied into classpath:/static, so one server hosts both the API and
 * the website. React does its own routing in the browser, so a URL like /countries/JP has no file
 * behind it: for those we send index.html and let React draw the right page.
 */
@Configuration
public class WebsiteConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // Unknown API paths and missing files (anything with an extension, like
                        // favicon.ico) must stay 404s, not turn into the website.
                        String lastSegment = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")
                                || lastSegment.contains(".")) {
                            return null;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
