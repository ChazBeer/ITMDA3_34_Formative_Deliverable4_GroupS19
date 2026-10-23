package org.example.cafeteria_ordering_backend.service;

import org.example.cafeteria_ordering_backend.dto.requests.LoginRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.RegisterRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.AuthResponseDTO;
import org.example.cafeteria_ordering_backend.dto.response.AuthTokensDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserProfileDTO;
import org.example.cafeteria_ordering_backend.entity.RefreshToken;
import org.example.cafeteria_ordering_backend.entity.Role;
import org.example.cafeteria_ordering_backend.entity.User;
import org.example.cafeteria_ordering_backend.repository.RefreshTokenRepository;
import org.example.cafeteria_ordering_backend.repository.RoleRepository;
import org.example.cafeteria_ordering_backend.repository.UserRepository;
import org.example.cafeteria_ordering_backend.security.JwtUtils;
import org.example.cafeteria_ordering_backend.security.TokenBlacklistService;
import org.example.cafeteria_ordering_backend.service.implementation.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    // Injects mocks into service being tested
    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;
    private Role customerRole;
    private User testUser;

    // Reusable setup before each test
    @BeforeEach
    void setUp() {

        customerRole = new Role();
        customerRole.setRoleId(1L);
        customerRole.setRoleName("CUSTOMER");

        registerRequest = new RegisterRequestDTO();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john.doe@example.com");
        registerRequest.setPassword("Password123!");

        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("john.doe@example.com");
        loginRequest.setPassword("Password123!");

        testUser = new User();
        testUser.setUserId(101L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPasswordHash("hashed_password");
        testUser.setCustomerCode("CUST-123456");
        testUser.setRole(customerRole);
        testUser.setActive(true);
    }

    @Nested
    @DisplayName("Register Method Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should successfully register user")
        void register_Success () {

            // Config expected registration dependencies
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(roleRepository.findByRoleName("CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtils.generateToken(registerRequest.getEmail())).thenReturn("fake.jwt.token");

            // Reg new user
            AuthResponseDTO response = authService.register(registerRequest);

            // Check response
            assertNotNull(response);
            assertEquals("fake.jwt.token",response.getToken());

            verify(userRepository, times(1)).save(any(User.class));
            verify(jwtUtils, times(1)).generateToken(registerRequest.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_ThrowException_WhenEmailExists () {

            // Simulate an registered email address
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            // Reg must fail with given exception
            RuntimeException exception = assertThrows(
                            RuntimeException.class,
                            () -> authService.register(registerRequest)
                    );

            assertEquals("Email is already registered!",exception.getMessage());

            // Make sure the user is not saved
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when CUSTOMER role does not exist")
        void register_ThrowsException_WhenCustomerRoleNotFound () {

            // Missing customer role
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(roleRepository.findByRoleName("CUSTOMER")).thenReturn(Optional.empty());

            // Must throw appropriate exception
            RuntimeException exception = assertThrows(
                            RuntimeException.class,
                            () -> authService.register(registerRequest)
                    );

            assertEquals("Default role 'CUSTOMER' not found in database.",exception.getMessage());

            // Ensure reg is not saved
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Register With Refresh Token Tests")
    class RegisterWithRefreshTokenTests {

        @Test
        @DisplayName("Should register user and create refresh token")
        void registerWithRefreshToken_Success () {

            // Configuring registration, token gen, and token hashing
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(roleRepository.findByRoleName("CUSTOMER")).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtils.generateToken(registerRequest.getEmail())).thenReturn("fake.access.token");
            when(jwtUtils.generateRefreshToken()).thenReturn("fake.refresh.token");
            when(jwtUtils.hashRefreshToken("fake.refresh.token")).thenReturn("fake.refresh.hash");

            // Register the user
            AuthTokensDTO response =authService.registerWithRefreshToken(registerRequest);

            // Ensure returned tokens and refresh tokens are saved
            assertNotNull(response);
            assertEquals("fake.access.token",response.getAccessToken());
            assertEquals("fake.refresh.token",response.getRefreshToken());

            verify(userRepository, times(1)).save(any(User.class));
            verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("Login Method Tests")
    class LoginTests {

        @Test
        @DisplayName("Should authenticate credentials and return token")
        void login_success () {

            // Provide existing user and their token
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(jwtUtils.generateToken(loginRequest.getEmail())).thenReturn("fake.jwt.token");

            // Authenticate user
            AuthResponseDTO response = authService.login(loginRequest);

            // Verify response and authentication
            assertNotNull(response);
            assertEquals("fake.jwt.token",response.getToken());

            verify(authenticationManager, times(1))
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtils, times(1)).generateToken(loginRequest.getEmail());
        }
    }

    @Nested
    @DisplayName("Login With Refresh Token Tests")
    class LoginWithRefreshTokenTests {

        @Test
        @DisplayName("Should login and create refresh token")
        void loginWithRefreshToken_Success () {

            // Configure authentication and token functions
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(jwtUtils.generateToken(loginRequest.getEmail())).thenReturn("fake.access.token");
            when(jwtUtils.generateRefreshToken()).thenReturn("fake.refresh.token");
            when(jwtUtils.hashRefreshToken("fake.refresh.token")).thenReturn("fake.refresh.hash");

            // Authenticate user and generate tokens
            AuthTokensDTO response = authService.loginWithRefreshToken(loginRequest);

            // Verify tokens
            assertNotNull(response);
            assertEquals("fake.access.token", response.getAccessToken());
            assertEquals("fake.refresh.token", response.getRefreshToken());

            verify(authenticationManager, times(1))
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("Refresh Access Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh access token successfully")
        void refreshAccessToken_Success () {

            // Prepare valid stored refresh token
            RefreshToken storedToken = new RefreshToken();

            storedToken.setId(1L);
            storedToken.setTokenHash("old.hash");
            storedToken.setUser(testUser);
            storedToken.setExpiresAt(Instant.now().plusSeconds(3600));
            storedToken.setRevoked(false);

            when(jwtUtils.hashRefreshToken("old.refresh.token")).thenReturn("old.hash");
            when(refreshTokenRepository.findByTokenHash("old.hash")).thenReturn(Optional.of(storedToken));
            when(jwtUtils.generateToken(testUser.getEmail())).thenReturn("new.access.token");
            when(jwtUtils.generateRefreshToken()).thenReturn("new.refresh.token");
            when(jwtUtils.hashRefreshToken("new.refresh.token")).thenReturn("new.hash");

            // Exchange existing token for new pair
            AuthTokensDTO response = authService.refreshAccessToken("old.refresh.token");

            // Verify new tokens and revocation of previous tokens
            assertNotNull(response);
            assertEquals("new.access.token",response.getAccessToken());
            assertEquals("new.refresh.token",response.getRefreshToken());
            assertTrue(storedToken.isRevoked());

            verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should reject invalid refresh token")
        void refreshAccessToken_InvalidToken () {

            // Simulate an incorrect token
            when(jwtUtils.hashRefreshToken("invalid.token")).thenReturn("invalid.hash");
            when(refreshTokenRepository.findByTokenHash("invalid.hash")).thenReturn(Optional.empty());

            // Must return the correct exception
            RuntimeException exception = assertThrows(
                            RuntimeException.class,
                            () -> authService.refreshAccessToken(
                                    "invalid.token"
                            )
                    );

            assertEquals("Invalid refresh token",exception.getMessage());
        }

        @Test
        @DisplayName("Should reject revoked refresh token")
        void refreshAccessToken_RevokedToken () {

            // Prepare fresh revoked token
            RefreshToken storedToken = new RefreshToken();

            storedToken.setTokenHash("revoked.hash");
            storedToken.setUser(testUser);
            storedToken.setExpiresAt(Instant.now().plusSeconds(3600));
            storedToken.setRevoked(true);

            when(jwtUtils.hashRefreshToken("revoked.token")).thenReturn("revoked.hash");
            when(refreshTokenRepository.findByTokenHash("revoked.hash")).thenReturn(Optional.of(storedToken));

            // Revoked tokens must not be accepted for renewal
            RuntimeException exception = assertThrows(
                            RuntimeException.class,
                            () -> authService.refreshAccessToken(
                                    "revoked.token"
                            )
                    );

            assertEquals("Refresh token has been revoked", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject expired refresh token")
        void refreshAccessToken_ExpiredToken () {

            // Prep fresh expired token
            RefreshToken storedToken = new RefreshToken();

            storedToken.setTokenHash("expired.hash");
            storedToken.setUser(testUser);
            storedToken.setExpiresAt(Instant.now().minusSeconds(3600));
            storedToken.setRevoked(false);

            when(jwtUtils.hashRefreshToken("expired.token")).thenReturn("expired.hash");
            when(refreshTokenRepository.findByTokenHash("expired.hash")).thenReturn(Optional.of(storedToken));

            // Expect correct exception
            RuntimeException exception = assertThrows(
                            RuntimeException.class,
                            () -> authService.refreshAccessToken(
                                    "expired.token"
                            )
                    );

            assertEquals("Refresh token has expired", exception.getMessage()
            );
        }
    }

    @Nested
    @DisplayName("Logout Method Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should extract token from Bearer prefix and blacklist it")
        void logout_Success_WithBearerPrefix () {

            // Setup bearer authorization with JWT
            String bearerToken = "Bearer raw.jwt.token";
            String rawToken = "raw.jwt.token";

            Instant expiration = Instant.now().plusSeconds(3600);

            when(jwtUtils.getExpirationDateFromToken(rawToken)).thenReturn(expiration);

            authService.logout(bearerToken);

            // Ensure raw token is blacklisted until it expires
            verify(jwtUtils, times(1)).getExpirationDateFromToken(rawToken);
            verify(tokenBlacklistService, times(1))
                    .blacklistToken(rawToken, expiration);
        }

        @Test
        @DisplayName("Should ignore token when Bearer prefix is missing")
        void logout_WithoutBearerPrefix () {

            // Provide the token without the bearer prefix
            String rawToken = "raw.jwt.token";

            authService.logout(rawToken);

            // Ensure no expiration lookup or blacklisting occurs
            verify(jwtUtils, never()).getExpirationDateFromToken(rawToken);
            verify(tokenBlacklistService, never()).blacklistToken(any(), any());
        }

        @Test
        @DisplayName("Should revoke refresh token")
        void revokeRefreshToken_Success () {

            // Prep active refresh token in repo
            RefreshToken storedToken = new RefreshToken();

            storedToken.setId(1L);
            storedToken.setTokenHash("refresh.hash");
            storedToken.setUser(testUser);
            storedToken.setExpiresAt(Instant.now().plusSeconds(3600));
            storedToken.setRevoked(false);

            when(jwtUtils.hashRefreshToken("refresh.token")).thenReturn("refresh.hash");
            when(refreshTokenRepository.findByTokenHash("refresh.hash")).thenReturn(Optional.of(storedToken));

            // Revoke said refresh token
            authService.revokeRefreshToken("refresh.token");

            // Ensure it is revoked and saved
            assertTrue(storedToken.isRevoked());

            verify(refreshTokenRepository, times(1)).save(storedToken);
        }
    }

    @Nested
    @DisplayName("GetCurrentUserProfile Method Tests")
    class ProfileTests {

        @Test
        @DisplayName("Should return UserProfileDTO when user exists")
        void getCurrentUserProfile_Success () {

            // configuring the repo to return requested user
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));

            UserProfileDTO profile = authService.getCurrentUserProfile("john.doe@example.com");

            // Ensure user profile has expected details
            assertNotNull(profile);
            assertEquals(101L,profile.getUserId());

            assertEquals("John",profile.getFirstName());
            assertEquals("Doe",profile.getLastName());
            assertEquals("john.doe@example.com",profile.getEmail());
            assertEquals("CUST-123456",profile.getCustomerCode());
            assertEquals("CUSTOMER",profile.getRole());
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void getCurrentUserProfile_ThrowsException_WhenNotFound () {

            // Simulate lookup of user that doesn't exist
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Expect correct Exception
            RuntimeException exception = assertThrows(
                            RuntimeException.class,
                            () -> authService.getCurrentUserProfile(
                                    "notfound@example.com"
                            )
                    );

            assertEquals("User not found with email: notfound@example.com",exception.getMessage());
        }
    }
}