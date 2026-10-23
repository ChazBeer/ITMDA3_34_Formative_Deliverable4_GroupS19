package org.example.cafeteria_ordering_backend.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    // Uploads image file returning its URL
    String uploadImage (MultipartFile file, String folderName);

    // Deletes existing image by its URL
    void deleteImage (String imageUrl);
}