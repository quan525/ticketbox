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
                        .requestMatchers(HttpMethod.GET, "/auth/google/config").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/google").permitAll()
                        .requestMatchers("/auth/signup", "/auth/login", "/", "/index.html", "/assets/**", "/posters/**", "/favicon.svg", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/movies", "/movies/*", "/shows", "/shows/*", "/show-seats/show/*").permitAll()
                        .requestMatchers("/users/*/role").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/movies/**", "/shows/**", "/rooms/**", "/seats/**", "/show-seats/**", "/users").hasAnyAuthority("ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.PUT, "/movies/**", "/shows/**", "/rooms/**", "/seats/**", "/show-seats/**").hasAnyAuthority("ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/movies/**", "/shows/**", "/rooms/**", "/seats/**", "/show-seats/**").hasAnyAuthority("ADMIN", "MODERATOR")
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/payments/*/complete")
                        .hasAnyAuthority("ADMIN", "MODERATOR")
//                        .requestMatchers("/bookings/**").permitAll()   // login/register
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
