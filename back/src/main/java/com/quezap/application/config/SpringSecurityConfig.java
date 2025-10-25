package com.quezap.application.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SpringSecurityConfig {

  /** User Argon2 instead of BCrypt. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  @Bean
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    return http.sessionManagement(
            // No cookie session, just state less API.
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // No CSRF for stateless APIs.
        .csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(
            request -> {

              // Allow all for now
              request.anyRequest().permitAll();

              // Allow root path.
              // request.requestMatchers("/").permitAll();

              // Any other routes are.
              // request.anyRequest().fullyAuthenticated();
            })
        .build();
  }
}
