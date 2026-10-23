package org.example.cafeteria_ordering_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// TODO: Investigate non-usage, I think its likely regarding local storage not working atm
public class ImageUploadResponseDTO {

    // ID of the menu item associated with the uploaded image.
    private Long menuItemId;

    // Path used to access the uploaded image.
    private String imagePath;

    // Message describing the result of the upload operation.
    private String message;
}