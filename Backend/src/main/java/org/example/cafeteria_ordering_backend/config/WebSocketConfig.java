package org.example.cafeteria_ordering_backend.config;

import org.example.cafeteria_ordering_backend.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public WebSocketConfig (JwtUtils jwtUtils, UserDetailsService userDetailsService) {

        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureMessageBroker (MessageBrokerRegistry config) {

       // Enable topic destinations for broadcasting messages to subscribed clients.
        config.enableSimpleBroker("/topic");

       // Route clients messages sent to /app to application message handlers.
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints (StompEndpointRegistry registry) {

        // Use the same dynamic allowed origin patterns as your SecurityConfig
        registry.addEndpoint("/ws-orders")
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }

    @Override
    public void configureClientInboundChannel (ChannelRegistration registration) {

        //Intercept incoming STOMP messages to authenticate WebSocket connection.
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Authenticate the clients when it establishes a STOMP connection.
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    // Extract and validate the JWT from the Authorization header.
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        if (jwtUtils.validateToken(token)) {
                            // Load the user associated with the validated token.
                            String username = jwtUtils.getUsernameFromToken(token);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                            // Create an authenticated principal with the user's authorities.
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(userDetails, null,
                                            userDetails.getAuthorities());

                            // Associate the authenticated user with the WebSocket session.
                            accessor.setUser(authentication);
                        }
                    }
                }
                return message;
            }
        });
    }
}