package com.apptitle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Phase 1 skeleton: stateless session policy, CORS wired to the CorsConfig bean,
 * CSRF disabled (token-based API, not cookie/session based), only the health
 * check is public.
 *
 * NOT YET WIRED (arrives in Phase 2 alongside auth/ module):
 *   - JWT authentication filter
 *   - Role-based endpoint rules (/api/teacher/**, /api/students/**)
 *   - AuthenticationProvider backed by the User entity
 *
 * Until Phase 2 lands, every endpoint other than /api/health effectively has
 * no working authentication mechanism to satisfy "authenticated()" with — that
 * is expected and intentional; no feature endpoints exist yet either.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
