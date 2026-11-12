package com.example.ticketbox.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/payments/sepay/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/payments/sepay/events", "/payments/sepay/deliveries").hasAuthority("VIEW_PAYMENTS")
                        .requestMatchers("/admin/users/**", "/users/*/role").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/admin/workspace").hasAuthority("VIEW_OPERATIONS")
                        .requestMatchers("/admin/rooms/**").hasAuthority("MANAGE_ROOMS")
                        .requestMatchers("/admin/shows/**").hasAuthority("MANAGE_SHOWS")
                        .requestMatchers("/admin/movies/**").hasAuthority("MANAGE_MOVIES")
                        .requestMatchers("/admin/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/auth/google/config").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/google", "/auth/refresh", "/auth/logout").permitAll()
                        .requestMatchers("/auth/signup", "/auth/login", "/", "/index.html", "/assets/**", "/posters/**", "/payment-qr/**", "/favicon.svg", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/movies", "/movies/*", "/shows", "/shows/*", "/show-seats/show/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/rooms/**", "/seats/**").hasAuthority("VIEW_ROOMS")
                        .requestMatchers(HttpMethod.POST, "/movies/**").hasAuthority("MANAGE_MOVIES")
                        .requestMatchers(HttpMethod.PUT, "/movies/**").hasAuthority("MANAGE_MOVIES")
                        .requestMatchers(HttpMethod.DELETE, "/movies/**").hasAuthority("MANAGE_MOVIES")
                        .requestMatchers(HttpMethod.POST, "/shows/**", "/show-seats/**").hasAuthority("MANAGE_SHOWS")
                        .requestMatchers(HttpMethod.PUT, "/shows/**", "/show-seats/**").hasAuthority("MANAGE_SHOWS")
                        .requestMatchers(HttpMethod.DELETE, "/shows/**", "/show-seats/**").hasAuthority("MANAGE_SHOWS")
                        .requestMatchers(HttpMethod.POST, "/rooms/**", "/seats/**").hasAuthority("MANAGE_ROOMS")
                        .requestMatchers(HttpMethod.PUT, "/rooms/**", "/seats/**").hasAuthority("MANAGE_ROOMS")
                        .requestMatchers(HttpMethod.DELETE, "/rooms/**", "/seats/**").hasAuthority("MANAGE_ROOMS")
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/payments/*/complete").hasAuthority("MANAGE_PAYMENTS")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> response.sendError(401)))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
