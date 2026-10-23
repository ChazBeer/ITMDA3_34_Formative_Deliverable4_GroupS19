package org.example.cafeteria_ordering_backend.service.implementation;

import org.example.cafeteria_ordering_backend.dto.requests.AdminPasswordResetRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.UserCreateRequestDTO;
import org.example.cafeteria_ordering_backend.dto.requests.UserUpdateRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserProfileDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserResponseDTO;
import org.example.cafeteria_ordering_backend.entity.Role;
import org.example.cafeteria_ordering_backend.entity.User;
import org.example.cafeteria_ordering_backend.exception.BusinessException;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.mapper.UserMapper;
import org.example.cafeteria_ordering_backend.repository.RefreshTokenRepository;
import org.example.cafeteria_ordering_backend.repository.RoleRepository;
import org.example.cafeteria_ordering_backend.repository.UserRepository;
import org.example.cafeteria_ordering_backend.security.JwtUtils;
import org.example.cafeteria_ordering_backend.security.TokenBlacklistService;
import org.example.cafeteria_ordering_backend.service.interfaces.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtils jwtUtils;

    // Injects the repositories and security components required to manage users.
    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            RefreshTokenRepository refreshTokenRepository,
            TokenBlacklistService tokenBlacklistService,
            JwtUtils jwtUtils) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtUtils = jwtUtils;
    }

    // Creates a user account and assigns the requested role.
    @Override
    @Transactional
    public UserResponseDTO createUser (UserCreateRequestDTO requestDTO, Long roleId) {

        logger.info("Creating user with email: {} and role ID: {}", requestDTO.getEmail(), roleId);

        // Check whether email is already registered
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            logger.warn("User creation failed - email already exists: {}", requestDTO.getEmail());
            throw new BusinessException("Email already exists!");
        }

        // Find the requested role
        Role role = roleRepository.findById(roleId).orElseThrow(() -> {
                    logger.warn("User creation failed - role not found with ID: {}", roleId);
                    return new ResourceNotFoundException("Role not found with ID: " + roleId);
                });

        // Mapping requestDTO to User entity
        User user = userMapper.toUserEntity(requestDTO);
        user.setRole(role);

        // Hash the password before saving it
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }

        // Generate customer code if one was not provided
        if (user.getCustomerCode() == null || user.getCustomerCode().isBlank()) {
            user.setCustomerCode(generateUniqueCustomerCode());
        }

        User savedUser = userRepository.save(user);

        logger.info("User created successfully with ID: {} and email: {}", savedUser.getUserId(), savedUser.getEmail());
        return userMapper.toUserResponseDTO(savedUser);
    }

    // Retrieves a user by ID or throws an exception if the user does not exist.
    @Override
    @Transactional(readOnly = true)
    public User getUserById (Long id) {

        logger.info("Fetching user with ID: {}", id);

        return userRepository.findById(id).orElseThrow(() -> {
            logger.warn("User not found with ID: {}", id);
            return new ResourceNotFoundException("User not found with ID: " + id);
        });
    }

    // Retrieves a user's profile details as a DTO.
    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfileById (Long id) {

        logger.info("Fetching user profile DTO with ID: {}", id);
        User user = getUserById(id);

        return userMapper.toUserProfileDTO(user);
    }

    // Retrieves a user's response details as a DTO.
    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserResponseById (Long id) {

        logger.info("Fetching user response DTO with ID: {}", id);
        User user = getUserById(id);

        return userMapper.toUserResponseDTO(user);
    }

    // Retrieves all user entities from the database.
    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers () {

        logger.info("Fetching all users");
        return userRepository.findAll();
    }

    // Retrieves all users and converts them into profile DTOs.
    @Override
    @Transactional(readOnly = true)
    public List<UserProfileDTO> getAllUserProfiles () {

        logger.info("Fetching all user profile DTOs");
        return userRepository.findAll().stream().map(userMapper::toUserProfileDTO).toList();
    }

    // Retrieves all users and converts them into response DTOs.
    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUserResponses () {

        logger.info("Fetching all user response DTOs");
        return userRepository.findAll().stream().map(userMapper::toUserResponseDTO).toList();
    }

    // Generates unique customer code (CUST-XXXXXX, must be unique
    private String generateUniqueCustomerCode () {

        String code;

        // Checks the database to ensure the generated code is not already in use.
        do {
            code = "CUST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (userRepository.existsByCustomerCode(code));

        return code;
    }

    // Updates the authenticated user's profile after verifying their current password.
    @Override
    @Transactional
    public UserResponseDTO updateCurrentUser (String email, UserUpdateRequestDTO requestDTO) {

        // Fetch the user by their authenticated email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Verify the current password matches
        if (!passwordEncoder.matches(requestDTO.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Incorrect current password provided.");
        }

        // Conditionally update standard profile details
        if (requestDTO.getFirstName() != null) {
            user.setFirstName(requestDTO.getFirstName());
        }
        if (requestDTO.getLastName() != null) {
            user.setLastName(requestDTO.getLastName());
        }
        if (requestDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(requestDTO.getPhoneNumber());
        }

        // Handle email update (Optional: check if the new email is already taken)
        if (requestDTO.getEmail() != null && !requestDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(requestDTO.getEmail())) {
                throw new BusinessException("Email is already in use by another account.");
            }
            user.setEmail(requestDTO.getEmail());
        }

        // Handle password change if a new password is provided
        if (requestDTO.getNewPassword() != null && !requestDTO.getNewPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(requestDTO.getNewPassword()));
        }

        // Save changes and return response DTO
        User updatedUser = userRepository.save(user);
        return userMapper.toUserResponseDTO(updatedUser);
    }

    // Returns user as UserResponseDTO for data safety
    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserResponseByEmail (String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return userMapper.toUserResponseDTO(user);
    }

    // Retrieves a user's response details using their email address.
    @Override
    @Transactional
    public void deleteCurrentUser (String email, String authHeader) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        user.setActive(false);
        userRepository.save(user);

        // Invalidate db refresh tokens
        refreshTokenRepository.deleteByUser(user);

        // Blacklist the current access token if provided
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();

            // Extract expiration time from the token so the blacklist service knows when it expires
            try {
                Instant expiryTime = jwtUtils.getExpirationDateFromToken(token);
                tokenBlacklistService.blacklistToken(token, expiryTime);
            } catch (Exception e) {
                // If token is already invalid/expired we are ignoring it
            }
        }
    }

    // Allows an administrator to reset another user's password after identity verification.
    @Override
    @Transactional
    public void resetUserPassword (String adminEmail, Long targetUserId, AdminPasswordResetRequestDTO requestDTO) {

        // Verify identity and password of the admin making the request
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (!passwordEncoder.matches(requestDTO.getAdminPassword(), admin.getPasswordHash())) {
            throw new BadCredentialsException("Invalid admin password provided.");
        }

        // Fetch the target user whose password needs resetting
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + targetUserId));

        // Prevent admin from resetting their own password
        if (admin.getUserId().equals(targetUser.getUserId())) {
            throw new BusinessException("Admins cannot reset their own password through the user management endpoint.");
        }

        // Update the target users password hash
        targetUser.setPasswordHash(passwordEncoder.encode(requestDTO.getNewPassword()));
        userRepository.save(targetUser);

        logger.warn("SECURITY_AUDIT: Admin [{}] successfully reset password for user ID [{}]", admin.getEmail(), targetUser.getUserId());
    }

    @Override
    public List<UserResponseDTO> getUsersByRoleName (String roleName) {

        List<User> users = userRepository.findByRole_RoleName(roleName);

        return users.stream()
                .map(userMapper::toUserResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public UserResponseDTO suspendUser (Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        //safety check, stops suspending other admins
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new BusinessException("Cannot suspend an administrative account. ");
        }

        user.setActive(false);
        User updatedUser = userRepository.save(user);

        return userMapper.toUserResponseDTO(updatedUser);
    }

    @Transactional
    @Override
    public UserResponseDTO unsuspendUser (Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setActive(true);
        User updatedUser = userRepository.save(user);

        return userMapper.toUserResponseDTO(updatedUser);
    }

    // Returns users currently marked as active/ unactive
    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUsersByActiveStatus (Boolean active) {

        List<User> users = userRepository.findByActive(active);
        return users.stream()
                .map(userMapper::toUserResponseDTO)
                .collect(Collectors.toList());
    }
}