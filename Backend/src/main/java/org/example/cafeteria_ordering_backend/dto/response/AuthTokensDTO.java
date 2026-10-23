package org.example.cafeteria_ordering_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokensDTO {

    // JWT access token used to authenticate API requests.
    private String accessToken;

    // Refresh token used to obtain a new access token.
    private String refreshToken;
}