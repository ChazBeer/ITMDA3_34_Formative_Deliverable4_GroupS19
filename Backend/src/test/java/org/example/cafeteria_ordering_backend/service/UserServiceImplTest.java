package org.example.cafeteria_ordering_backend.service;

import org.example.cafeteria_ordering_backend.dto.requests.UserCreateRequestDTO;
import org.example.cafeteria_ordering_backend.dto.response.UserResponseDTO;
import org.example.cafeteria_ordering_backend.entity.Role;
import org.example.cafeteria_ordering_backend.entity.User;
import org.example.cafeteria_ordering_backend.exception.BusinessException;
import org.example.cafeteria_ordering_backend.exception.ResourceNotFoundException;
import org.example.cafeteria_ordering_backend.mapper.UserMapper;
import org.example.cafeteria_ordering_backend.repository.RoleRepository;
import org.example.cafeteria_ordering_backend.repository.UserRepository;
import org.example.cafeteria_ordering_backend.service.implementation.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Initialize Mockito
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    // Creates service instance and inject mock classes into it
    @InjectMocks
    private UserServiceImpl userService;


    // Shared test data
    private UserCreateRequestDTO createRequestDTO;
    private Role role;
    private User user;
    private UserResponseDTO userResponseDTO;

    /**
     * Preps test data for each test to use.
     * Executed before every test.
     */
    @BeforeEach
    void setUp () {

        // Request data for "registered" user.
        createRequestDTO = UserCreateRequestDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .passwordHash("password123")
                .phoneNumber("0123456789")
                .build();

        // Role assigned during registration
        role = new Role();
        role.setRoleId(1L);
        role.setRoleName("ROLE_STUDENT");

        // User entity returned by Mocked mapper
        user = new User();
        user.setFirstName("John");
        user.setEmail("john.doe@example.com");
        user.setPasswordHash("encoded_password123");

        // Expected response returned to caller if successful
        userResponseDTO = UserResponseDTO.builder()
                .userId(1L)
                .email("john.doe@example.com")
                .roleName("ROLE_STUDENT")
                .build();
    }

    @Test
    @DisplayName("Should create user successfully when email is unique and role exists")
    void shouldCreateUserSuccessfully () {

        // Stimulate unique email and existing role as well as checks persistence
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userMapper.toUserEntity(createRequestDTO)).thenReturn(user);
        when(passwordEncoder.encode("encoded_password123")).thenReturn("encoded_password123");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponseDTO(any(User.class))).thenReturn(userResponseDTO);

        // Call service to create user
        UserResponseDTO result = userService.createUser(createRequestDTO, 1L);

        // Verify returned response and interactions
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(passwordEncoder, times(1)).encode(anyString());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw exception when creating user with existing email")
    void shouldThrowExceptionWhenEmailExists () {

        // Simulate existing email address
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // Verify duplicate registration is rejected
        assertThatThrownBy(() -> userService.createUser(createRequestDTO, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists!");

        // Ensure user is forgotten after duplicate deteciton
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when role ID is invalid")
    void shouldThrowExceptionWhenRoleNotFound () {

        // Setup the email being available, but role does not exist
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        // Verify that invalid role ID provides expected exception
        assertThatThrownBy(() -> userService.createUser(createRequestDTO, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Role not found with ID: 99");

        // Check that registration stops before user is saved
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return list of users successfully when searching by role")
    void shouldReturnUsersByRoleSuccessfully () {

        // Set up a repository result with one user with having requested role
        List<User> users = List.of(user);

        when(userRepository.findByRole_RoleName("ROLE_STUDENT")).thenReturn(users);
        when(userMapper.toUserResponseDTO(user)).thenReturn(userResponseDTO);

        // Get users assigned to specified role
        List<UserResponseDTO> result = userService.getUsersByRoleName("ROLE_STUDENT");

        // Check returned collection and mapped role information
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoleName()).isEqualTo("ROLE_STUDENT");

        // Ensure repo is queried and returned entity is mapped once
        verify(userRepository, times(1)).findByRole_RoleName("ROLE_STUDENT");
        verify(userMapper, times(1)).toUserResponseDTO(user);
    }

    @Test
    @DisplayName("Should suspend non-admin user successfully")
    void shouldSuspendUserSuccessfully () {

        // Set up active user with non ADMIN role
        user.setActive(true);
        role.setRoleName("CUSTOMER");
        user.setRole(role);

        // Simulate entity returned after account suspended
        User suspendedUser = new User();
        suspendedUser.setUserId(1L);
        suspendedUser.setActive(false);
        suspendedUser.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(suspendedUser);
        when(userMapper.toUserResponseDTO(any(User.class))).thenReturn(userResponseDTO);

        // Suspend the user
        UserResponseDTO result = userService.suspendUser(1L);

        // Verify a response is returned and acc is suspended
        assertThat(result).isNotNull();
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(user);
        assertThat(user.getActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when attempting to suspend an admin account")
    void shouldThrowExceptionWhenSuspendingAdmin () {

        // Assign the admin role to user
        role.setRoleName("ADMIN");
        user.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Admin accounts shouldnt be suspended
        assertThatThrownBy(() -> userService.suspendUser(1L))
                .isInstanceOf(BusinessException.class) // <--- Updated from IllegalArgumentException
                .hasMessage("Cannot suspend an administrative account. ");

        // Check user is retrieved but never saved after rejection
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should unsuspend user successfully")
    void shouldUnsuspendUserSuccessfully () {

        // Config active user with assigned role
        user.setActive(false);
        user.setRole(role);

        // Simulate entities state after unsuspended
        User unsuspendedUser = new User();
        unsuspendedUser.setUserId(1L);
        unsuspendedUser.setActive(true);
        unsuspendedUser.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(unsuspendedUser);
        when(userMapper.toUserResponseDTO(any(User.class))).thenReturn(userResponseDTO);

        // Reactivate account
        UserResponseDTO result = userService.unsuspendUser(1L);

        // Verify service returns a response and activates user
        assertThat(result).isNotNull();
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(user);
        assertThat(user.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when attempting to unsuspend non-existent user")
    void shouldThrowExceptionWhenUnsuspendingNonExistentUser () {

        // Simulate user ID not in repository
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Verify missing user produces expected exception
        assertThatThrownBy(() -> userService.unsuspendUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with ID: 99");

        // Ensure lookup occurs and no DB save happens
        verify(userRepository, times(1)).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should return users by active status successfully")
    void shouldGetUsersByActiveStatusSuccessfully () {

        // Create active user and its responseDTO
        User activeUser = new User();
        activeUser.setUserId(1L);
        activeUser.setActive(true);

        UserResponseDTO activeUserDto = new UserResponseDTO();
        activeUserDto.setUserId(1L);
        activeUserDto.setActive(true);

        when(userRepository.findByActive(true)).thenReturn(List.of(activeUser));
        when(userMapper.toUserResponseDTO(activeUser)).thenReturn(activeUserDto);

        // Get all users with aactive status
        List<UserResponseDTO> result = userService.getUsersByActiveStatus(true);

        // Check returned list contains the active user in question
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();

        // Check repo was queried with requested status
        verify(userRepository, times(1)).findByActive(true);
    }

    @Test
    @DisplayName("Should return empty list when no users match active status")
    void shouldReturnEmptyListWhenNoUsersMatchStatus () {

        // Simulate repo query with no matching user
        when(userRepository.findByActive(false)).thenReturn(List.of());

        // Get users with inactive status
        List<UserResponseDTO> result = userService.getUsersByActiveStatus(false);

        // Ensure service returns empty list NOT null.
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // Ensure repo was queried with request status
        verify(userRepository, times(1)).findByActive(false);
    }
}