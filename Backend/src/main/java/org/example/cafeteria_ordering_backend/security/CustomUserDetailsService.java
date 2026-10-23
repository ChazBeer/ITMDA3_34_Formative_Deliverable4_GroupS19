package org.example.cafeteria_ordering_backend.security;

import org.springframework.security.authentication.DisabledException;
import org.springframework.transaction.annotation.Transactional;
import org.example.cafeteria_ordering_backend.entity.User;
import org.example.cafeteria_ordering_backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // Repository used to retrieve user details from the database.
    private final UserRepository userRepository;

    public CustomUserDetailsService (UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Loads the user's authentication details using their email address.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername (String email) throws UsernameNotFoundException {

        // Finds the user or throws an exception if no matching account exists.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Check if the user account has been suspended or deactivated
        if (user.getActive() != null && !user.getActive()) {
            throw new DisabledException("User account has been suspended.");
        }

        // Retrieves the user's role for Spring Security authorization.
        String roleName = user.getRole().getRoleName();

        // Appends ROLE as hasRole requires it
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        // Returns the user's credentials, account status, and granted authority.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.getActive(),
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}
