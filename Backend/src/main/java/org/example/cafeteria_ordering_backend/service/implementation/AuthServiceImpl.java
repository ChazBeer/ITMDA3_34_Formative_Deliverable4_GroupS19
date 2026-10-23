package org.example.cafeteria_ordering_backend.service.implementation;

import org.example.cafeteria_ordering_backend.dto.requests.LoginRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.RegisterRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.AuthResponseDTO;
import org.example.cafeteria_ordering_backend.dto.response.AuthTokensDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserProfileDTO;
import org.example.cafeteria_ordering_backend.entity.RefreshToken;
import org.example.cafeteria_ordering_backend.entity.Role;
import org.example.cafeteria_ordering_backend.entity.User;
import org.example.cafeteria_ordering_backend.exception.BusinessException;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.repository.RefreshTokenRepository;
import org.example.cafeteria_ordering_backend.repository.RoleRepository;
import org.example.cafeteria_ordering_backend.repository.UserRepository;
import org.example.cafeteria_ordering_backend.security.JwtUtils;
import org.example.cafeteria_ordering_backend.security.TokenBlacklistService;
import org.example.cafeteria_ordering_backend.service.interfaces.AuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

    @Service
    public class AuthServiceImpl implements AuthService {

        private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

        // Refresh tokens remain valid for seven days
        private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7;

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtils jwtUtils;
        private final AuthenticationManager authenticationManager;
        private final TokenBlacklistService tokenBlacklistService;
        private final RefreshTokenRepository refreshTokenRepository;

        // Injects the repositories and security components required for authentication.
        @Autowired
        public AuthServiceImpl (
                UserRepository userRepository,
                RoleRepository roleRepository,
                PasswordEncoder passwordEncoder,
                JwtUtils jwtUtils,
                AuthenticationManager authenticationManager,
                TokenBlacklistService tokenBlacklistService,
                RefreshTokenRepository refreshTokenRepository) {

            this.userRepository = userRepository;
            this.roleRepository = roleRepository;
            this.passwordEncoder = passwordEncoder;
            this.jwtUtils = jwtUtils;
            this.authenticationManager = authenticationManager;
            this.tokenBlacklistService = tokenBlacklistService;
            this.refreshTokenRepository = refreshTokenRepository;
        }

        @Override
        public boolean isStaffOrAdmin (String email) {

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            Role role = user.getRole();
            if (role == null) {
                return false;
            }

            String roleName = role.getRoleName();

            return roleName.contains("STAFF") || roleName.contains("ADMIN");
        }

        // Register a user and returns an access token.
        @Override
        @Transactional
        public AuthResponseDTO register (RegisterRequestDTO request) {

            AuthTokensDTO tokens = registerWithRefreshToken(request);
            return new AuthResponseDTO(tokens.getAccessToken());
        }

        // Creates a new customer account and generates access and refresh tokens.
        @Override
        @Transactional
        public AuthTokensDTO registerWithRefreshToken (RegisterRequestDTO request) {

            logger.info("Registration attempt for email: {}", request.getEmail());

            // Prevents registration with an email that is already in use.
            if (userRepository.existsByEmail(request.getEmail())) {

                logger.warn("Registration failed - email already exists: {}",request.getEmail());
                throw new BusinessException("Email is already registered!");
            }

            // Retrieves the default role assigned to newly registered users.
            Role defaultRole =roleRepository.findByRoleName("CUSTOMER")
                            .orElseThrow(() -> new ResourceNotFoundException(
                                            "Default role 'CUSTOMER' not found in database."
                                    )
                            );

            User user = new User();

            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());

            // Hashes the password before storing it in the database.
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

            // Assigns the default customer role and generates a unique customer code.
            user.setRole(defaultRole);
            user.setCustomerCode(generateUniqueCustomerCode());
            user.setActive(true);

            User savedUser = userRepository.save(user);

            // Generates an access token and a refresh token for the new account.
            String accessToken = jwtUtils.generateToken(savedUser.getEmail());
            String refreshToken = createRefreshToken(savedUser);

            logger.info("User registered successfully: {}",savedUser.getEmail());

            return new AuthTokensDTO(accessToken,refreshToken);
        }

        // Authenticates a user and returns an access token.
        @Override
        public AuthResponseDTO login (LoginRequestDTO request) {

            AuthTokensDTO tokens = loginWithRefreshToken(request);
            return new AuthResponseDTO(tokens.getAccessToken());
        }

        // Verifies login credentials and generates access and refresh tokens.
        @Override
        @Transactional
        public AuthTokensDTO loginWithRefreshToken (LoginRequestDTO request) {

            logger.info("Login attempt for email: {}",request.getEmail());

            // Authenticates the supplied email and password using Spring Security.
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Retrieves the authenticated user's account details.
            User user = userRepository.findByEmail(request.getEmail()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "User not found with email: "
                                            + request.getEmail()
                            )
                    );

            // Prevents inactive accounts from logging in.
            if (!Boolean.TRUE.equals(user.getActive())) {
                throw new BusinessException("User account is inactive");
            }

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String accessToken = jwtUtils.generateToken(user.getEmail());
            String refreshToken = createRefreshToken(user);

            logger.info("User logged in successfully: {}",user.getEmail());

            return new AuthTokensDTO(accessToken,refreshToken);
        }

        // Generates a refresh token and stores its hash in the database.
        private String createRefreshToken (User user) {

            String rawRefreshToken = jwtUtils.generateRefreshToken();
            String tokenHash = jwtUtils.hashRefreshToken(rawRefreshToken);

            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setTokenHash(tokenHash);
            refreshToken.setUser(user);
            refreshToken.setExpiresAt(Instant.now().plus(Duration.ofDays(REFRESH_TOKEN_EXPIRATION_DAYS)));
            refreshToken.setRevoked(false);

            refreshTokenRepository.save(refreshToken);

            // Returns the original token to the caller without storing it directly.
            return rawRefreshToken;
        }

        // Validates a refresh token and issues a new pair of tokens.
        // TODO: Look into if we handle concurrent refresh req from reusing the same token, if not must find fix
        @Override
        @Transactional
        public AuthTokensDTO refreshAccessToken (String rawRefreshToken) {

            if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
                throw new BusinessException("Refresh token is missing");
            }

            // Hashes the supplied token to find its database record.
            String tokenHash = jwtUtils.hashRefreshToken(rawRefreshToken);

            RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                            .orElseThrow(() -> new BusinessException("Invalid refresh token"));

            // Rejects refresh tokens that have already been revoked or have expired.
            if (storedToken.isRevoked()) {
                throw new BusinessException("Refresh token has been revoked");
            }

            if (storedToken.getExpiresAt().isBefore(Instant.now())) {
                throw new BusinessException("Refresh token has expired");
            }

            User user = storedToken.getUser();

            // Prevents inactive accounts from obtaining new access tokens.
            if (!Boolean.TRUE.equals(user.getActive())) {
                throw new BusinessException("User account is inactive");
            }

            // Revokes the old refresh token to prevent it from being reused.
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);

            // Issues a new access token and refresh token.
            String newAccessToken = jwtUtils.generateToken(user.getEmail());
            String newRefreshToken = createRefreshToken(user);

            logger.info("Access token refreshed for user: {}",user.getEmail());

            return new AuthTokensDTO(newAccessToken,newRefreshToken);
        }

        // Deletes expired refresh tokens from the database every day at 3:00 AM.
        @Scheduled(cron = "0 0 3 * * ?")
        @Transactional
        public void purgeExpiredTokens () {
            refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
            logger.info("Purged expired refresh tokens from database.");
        }

        // Invalidates the current access token when the user logs out.
        // TODO: Logout must still revoke refresh token, look into revoking all sessions if user is logged in on
        //  multiple devices
        @Override
        public void logout (String token) {

            logger.info("Logout request received");

            // Continues only if the supplied value uses the Bearer token format.
            if (token == null || !token.startsWith("Bearer ")) {return;}

            token = token.substring(7).trim();

            try {
                // Adds the token to the blacklist until its expiration time.
                Instant expiration = jwtUtils.getExpirationDateFromToken(token);
                tokenBlacklistService.blacklistToken(token,expiration);

                logger.info("Access token blacklisted");

            } catch (Exception e) {
                // Handles tokens that are invalid or have already expired.
                logger.info("Access token was already expired or invalid");
            }
        }

        // Revokes a refresh token so it cannot be used again.
        @Override
        @Transactional
        public void revokeRefreshToken (String rawRefreshToken) {

            if (rawRefreshToken == null || rawRefreshToken.isBlank()) {return;}

            // Uses the token hash to locate the stored refresh token.
            String tokenHash = jwtUtils.hashRefreshToken(rawRefreshToken);

            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);

                refreshTokenRepository.save(token);
                }
            );

            logger.info("Refresh token revoked");
        }

        // Retrieves the profile details of the user identified by email.
        @Override
        public UserProfileDTO getCurrentUserProfile (String email) {

            logger.info("Fetching profile for user: {}",email);

            User user = userRepository.findByEmail(email).orElseThrow(() ->
                new ResourceNotFoundException("User not found with email: "+ email)
            );

            return new UserProfileDTO(
                    user.getUserId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getCustomerCode(),
                    user.getRole().getRoleName(),
                    user.getLastLogin()
            );
        }

        // Generates a customer code that does not already exist in the database.
        private String generateUniqueCustomerCode () {

            String code;

            do {
                code = "CUST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            } while (userRepository.existsByCustomerCode(code));

            return code;
        }
    }