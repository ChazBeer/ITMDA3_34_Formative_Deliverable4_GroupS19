package org.example.cafeteria_ordering_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Use SA Standard Time for injected clocks across the backend.
    @Bean
    public Clock clock () {
        return Clock.system(ZoneId.of("Africa/Johannesburg"));
    }

    // Configure the directory used to store uploaded files.
    @Value("${app.upload.dir:../uploads}")
    private String uploadDir;

    /**
     * Currently not completely working, originally created for local image hosting
     */
    // TODO: Get working, or remove like the rest of local image hosting
    @Override
    public void addResourceHandlers (ResourceHandlerRegistry registry) {

        // Resolve path relative to execution root
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        File uploadDirFile = uploadPath.toFile();

        // Ensure target directory exists on disk so Spring never fails silently
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }

        // Convert the directory path into a URI for Spring's resource handler.
        String uploadUri = uploadPath.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }

        // Serve files through /upload/ and allow browsers to cache them for seven days.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri)
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
    }
}