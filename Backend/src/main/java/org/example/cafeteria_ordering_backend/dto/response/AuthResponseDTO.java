package org.example.cafeteria_ordering_backend.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    // blank on success, otherwise returns msg
    private String message;

    // JWT access token returned after successful authentication.
    private String token;

    // Defaults the token type to Bearer when using the builder.
    @Builder.Default
    private String tokenType = "Bearer";

    // Creates a response with the token and the default token type.
    public AuthResponseDTO (String token) {

        this.token = token;
        this.tokenType = "Bearer";
    }

}
