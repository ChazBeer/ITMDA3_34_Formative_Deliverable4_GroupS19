package org.example.cafeteria_ordering_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.example.cafeteria_ordering_backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableScheduling
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig (JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {

        http
                // Cross Origin Resource Sharing (CORS) allowing front end to communicate restricted backend resources
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable CSRF protection from the stateless API.
                .csrf(csrf -> csrf.disable())

                // Use JWT authentication instead of server-side HTTP sessions.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // Allow any authenticated user to access their own /me endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/users/me"
                        ).authenticated()

                        // Public Authentication & Application Info Endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/staff-login",
                                "/api/auth/refresh",
                                "/api/auth/staff-refresh",
                                "/api/auth/logout",
                                "/api/auth/staff-logout",
                                "/ws-orders",
                                "/ws-orders/**",
                                "/uploads/**"
                        ).permitAll()

                        // Publicly viewable cafeteria operating status (for PWA)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/cafeteria-status"
                        ).permitAll()

                        // Admin override controls for cafeteria status
                        .requestMatchers(
                                "/api/cafeteria-status/**"
                        ).hasRole("ADMIN")

                        // User profile access
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/me"
                        ).authenticated()

                        // Menu item update and delete (Admin only)
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/menu-items/{id}"
                        ).hasRole("ADMIN")

                        // Menu item availability toggle (Staff & Admin)
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/menu-items/{id}/availability"
                        ).hasAnyRole("STAFF", "ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/menu-items/{id}"
                        ).hasRole("ADMIN")

                        // Menu item image management (Staff & Admin)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/menu-items/{id}/image"
                        ).hasAnyRole("STAFF", "ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/menu-items/{id}/image"
                        ).hasAnyRole("STAFF", "ADMIN")

                        // Menu/Category read access (all authenticated roles)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/categories",
                                "/api/categories/{id}",
                                "/api/menu-items",
                                "/api/menu-items/{id}",
                                "/api/menu-items/category/{categoryId}",
                                "/api/menu-items/{menuItemId}/customizations"
                        ).hasAnyRole("CUSTOMER","STAFF","ADMIN","STUDENT")

                        // Staff/Admin view of ALL items (including inactive)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/menu-items/admin/all"
                        ).hasAnyRole("STAFF", "ADMIN")

                        // Category/menu management (admin only)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/categories"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/categories/{id}"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/categories/{id}"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/menu-items/category/{categoryId}"
                        ).hasRole("ADMIN")

                        // Menu customization creation
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/menu-items/{menuItemId}/modifier-groups",
                                "/api/menu-items/modifier-groups/{groupId}/modifiers"
                        ).hasRole("ADMIN")

                        // Menu customization update
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/menu-items/modifier-groups/{groupId}",
                                "/api/menu-items/modifiers/{modifierId}"
                        ).hasRole("ADMIN")

                        // Menu customization deletion
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/menu-items/modifier-groups/{groupId}",
                                "/api/menu-items/modifiers/{modifierId}"
                        ).hasRole("ADMIN")

                        // Customer Checkout
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/orders/checkout"
                        ).hasAnyRole("CUSTOMER","STAFF","ADMIN","STUDENT")

                        // Order Cancellation
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/orders/{id}/cancel"
                        ).hasAnyRole("CUSTOMER","STAFF","ADMIN","STUDENT")

                        // Staff and Admin order management
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/orders",
                                "/api/orders/active"
                        ).hasAnyRole("STAFF","ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/orders/{id}/status"
                        ).hasAnyRole("STAFF","ADMIN")

                        // Order lookup and history
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/orders/{id}",
                                "/api/orders/user/{userId}",
                                "/api/orders/user/{userId}/current"
                        ).hasAnyRole("CUSTOMER","STAFF","ADMIN","STUDENT")

                        // Role Management
                        .requestMatchers(
                                "/api/roles/**"
                        ).hasRole("ADMIN")

                        // User Management
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/{id}/suspend",
                                "/api/users/{id}/unsuspend",
                                "/api/users/{id}/reset-password"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users",
                                "/api/users/{id}",
                                "/api/users/profiles",
                                "/api/users/role/**",
                                "/api/users/status/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/{id}/profile"
                        ).hasAnyRole("STUDENT", "CUSTOMER", "STAFF", "ADMIN")
                )

                // Return 401 for unauthenticated requests and 403 for access-denied requests.
                .exceptionHandling(exception -> exception
                                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                                .accessDeniedHandler(
                                        (request,
                                                response,
                                                accessDeniedException) -> {
                                            response.setStatus(HttpStatus.FORBIDDEN.value());
                                        }
                                )
                )

                // Run JWT authentication before the username/password authentication filter.
                .addFilterBefore(jwtAuthenticationFilter,UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Configure BCrypt password hashing.
    @Bean
    public PasswordEncoder passwordEncoder () {
        return new BCryptPasswordEncoder();
    }

    // Provide the authentication manager used to authenticate users.
    @Bean
    public AuthenticationManager authenticationManager (

            AuthenticationConfiguration authConfig) throws Exception {

        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource () {

        CorsConfiguration configuration = new CorsConfiguration();

        // Set allowed CORS origins from a comma spaced string
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));

        // Allowed HTTP Methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers allowed in requests
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        // Expose Authorization header so frontend can read fresh/refreshed tokens
        configuration.setExposedHeaders(List.of("Authorization"));

        // Allow sending cookies/auth headers
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}