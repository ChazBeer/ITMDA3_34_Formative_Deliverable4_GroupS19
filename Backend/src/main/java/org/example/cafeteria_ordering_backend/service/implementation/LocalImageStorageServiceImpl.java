package org.example.cafeteria_ordering_backend.service.implementation;

import org.example.cafeteria_ordering_backend.exception.BusinessException;
import org.example.cafeteria_ordering_backend.exception.ResourceUnavailableException;
import org.example.cafeteria_ordering_backend.service.interfaces.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Stores images uploaded locally
// TODO: Get working, right now only cloud image storage works properly, need to get the two working separately
@Service("localImageStorage")
public class LocalImageStorageServiceImpl implements ImageStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalImageStorageServiceImpl.class);

    // Directory where uploaded images are stored.
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // Base URL used to construct image URLs returned to the client.
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");

    // Validates the image, saves it locally, and returns its URL.
    @Override
    public String uploadImage(MultipartFile file, String folderName) {
        validateFile(file);

        try {
            // Generates a unique filename to avoid naming conflicts.
            String extension = getFileExtension(file.getOriginalFilename());
            String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

            // Creates the target directory if it does not already exist.
            Path uploadPath = Paths.get(uploadDir, folderName).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Copies the uploaded file into the target directory.
            Path filePath = uploadPath.resolve(uniqueFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Builds the URL that clients can use to request the image.
            return String.format("%s/uploads/%s/%s", baseUrl, folderName, uniqueFilename);

        } catch (IOException ex) {
            throw new ResourceUnavailableException("Failed to store local file: " + ex.getMessage());
        }
    }

    // Deletes a locally stored image using its URL.
    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        try {
            // Extracts the file path from the image URL.
            int uploadIndex = imageUrl.indexOf("/uploads/");
            if (uploadIndex == -1) return;

            String relativePath = imageUrl.substring(uploadIndex + "/uploads/".length());
            Path filePath = Paths.get(uploadDir, relativePath).toAbsolutePath().normalize();

            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            logger.warn("Could not delete local file: {}", ex.getMessage());
        }
    }

    // Rejects empty files and files with unsupported extensions.
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Cannot upload empty file.");
        }

        // Check extension
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("Allowed image extensions are JPG, JPEG, PNG, WEBP.");
        }

        // Check actual content/MIME type for security
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Invalid file type. Only valid image files are allowed.");
        }
    }

    // Extracts the file extension from the original filename.
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "png";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}