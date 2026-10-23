package org.example.cafeteria_ordering_backend.service.implementation;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.example.cafeteria_ordering_backend.exception.BusinessException;
import org.example.cafeteria_ordering_backend.exception.ResourceUnavailableException;
import org.example.cafeteria_ordering_backend.service.interfaces.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service("cloudinaryImageStorage")
public class CloudinaryImageStorageServiceImpl implements ImageStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private final Cloudinary cloudinary;

    // Injects Cloudinary when it is configured in the application.
    // TODO: Implement working local storage 
    @Autowired(required = false)
    public CloudinaryImageStorageServiceImpl (Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // Uploads an image to Cloudinary and returns its secure URL.
    @Override
    public String uploadImage (MultipartFile file, String folderName) {

        // Reports an error if Cloudinary has not been configured.
        if (cloudinary == null) {
            throw new ResourceUnavailableException("Cloudinary credentials are not configured in " +
                    "application properties.");
        }

        validateFile(file);

        try {
            // Generate a unique identifier for every file upload
            String uniquePublicId = UUID.randomUUID().toString();

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "edueats/" + folderName,
                    "public_id", uniquePublicId,      // Guarantees a unique asset on cloudinary
                    "overwrite", false,               // Prevents overwriting existing files
                    "resource_type", "image"
            ));

            // Returns the HTTPS URL used to access the uploaded image.
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            throw new ResourceUnavailableException("Failed to upload image to Cloudinary: " + e.getMessage());
        }
    }

    // Deletes an image from Cloudinary using its URL.
    @Override
    public void deleteImage (String imageUrl) {

        // Skips deletion if Cloudinary is unavailable or the URL is empty.
        if (cloudinary == null || imageUrl == null || imageUrl.isBlank()) return;

        try {
            // Extracts the Cloudinary public ID from the image URL.
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not delete image from Cloudinary: " + e.getMessage());
        }
    }

    // Extracts the public ID from a Cloudinary URL for image deletion.
    private String extractPublicId (String imageUrl) {

        try {
            int folderIndex = imageUrl.indexOf("edueats/");

            // Returns null if the URL does not contain the expected folder.
            if (folderIndex == -1) return null;

            String pathAfterFolder = imageUrl.substring(folderIndex);
            int lastDotIndex = pathAfterFolder.lastIndexOf(".");

            // Removes the file extension because Cloudinary deletion uses the public ID.
            return lastDotIndex != -1 ? pathAfterFolder.substring(0, lastDotIndex) : pathAfterFolder;
        } catch (Exception e) {
            return null;
        }
    }

    private void validateFile (MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException("Cannot upload empty file.");
        }

        String filename = file.getOriginalFilename();
        String extension = (filename == null || !filename.contains(".")) ? "" :
                filename.substring(filename.lastIndexOf(".") + 1);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("Allowed image extensions are JPG, JPEG, PNG, WEBP.");
        }
    }
}