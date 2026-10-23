package org.example.cafeteria_ordering_backend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.example.cafeteria_ordering_backend.service.interfaces.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Setup for cloudinary and local host, local has not been tested yet
 */
@Configuration
public class ImageStorageConfig {

    // Selects the image and storage provider, defaulting to local storage.
    @Value("${app.image-storage.provider:local}")
    private String provider;

    // Cloudinary account credentials loaded from application properties.
    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    // Creates a Cloudinary client when all required credentials are available.
    @Bean
    public Cloudinary cloudinary () {

        if (!cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank()) {
            return new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
        }

        //Return null when Cloudinary credentials are not configured.
        return null;
    }

    // Registers the selected image storage service as the primary implementation.
    @Bean
    @Primary
    public ImageStorageService activeImageStorageService (ApplicationContext context) {

        // Select the storage service based on the configured provider.
        return switch (provider.toLowerCase()) {
            case "cloudinary" -> context.getBean("cloudinaryImageStorage", ImageStorageService.class);
            case "local" -> context.getBean("localImageStorage", ImageStorageService.class);
            default -> throw new IllegalArgumentException("Unsupported image storage provider: " + provider);
        };
    }
}