package com.moneytransfersystem.config;

import com.moneytransfersystem.service.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ✅ Disable CSRF for REST APIs
            .csrf(csrf -> csrf.disable())

            // ✅ Authorization Rules
            .authorizeHttpRequests(auth -> auth

                // Public endpoints
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/accounts/login").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                .requestMatchers("/api/snowflake/**").permitAll()


                // Everything else needs auth
                .anyRequest().authenticated()
            )

            // ✅ Allow H2 Console frames
            .headers(headers ->
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
            )

            // ✅ Enable ONLY Basic Auth
            .httpBasic(httpBasic -> {})

        // Disable Form Login (Fix)
        .formLogin(form -> form.disable());


        return http.build();
    }

    // Authentication Manager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // Password Encoder (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Load users from DB (AccountService)
    @Bean
    public UserDetailsService userDetailsService(AccountService accountService) {
        return accountService::loadUserByUsername;
    }
}
