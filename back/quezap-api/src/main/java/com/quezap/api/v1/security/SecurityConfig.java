package com.quezap.api.v1.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/apiv1/auth/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/apiv1/sessions/join")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/apiv1/sessions/*/answers")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/sessions/*/events")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/sessions/*/scores")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/sessions/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/quezaps/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/themes/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/questions/**")
                    .permitAll()
                    .requestMatchers("/actuator/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
