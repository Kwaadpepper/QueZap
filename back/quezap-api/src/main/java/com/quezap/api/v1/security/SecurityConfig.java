package com.quezap.api.v1.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final List<String> allowedOrigins;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:4200,http://localhost:8000}")
          String allowedOriginsConfig) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.allowedOrigins =
        Arrays.stream(allowedOriginsConfig.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
  }

  @Bean
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/apiv1/auth/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/apiv1/users")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/apiv1/sessions/join")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/apiv1/sessions/by-code/*/join")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/apiv1/sessions/*/answers")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/apiv1/sessions/*/leave")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/sessions/*/events")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/sessions/*/scores")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/sessions/by-code/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/sessions/*")
                    .permitAll()
                    // Public read access to quiz catalog and preview; private visibility and
                    // answer masking are strictly enforced at method level in QuezapController.
                    .requestMatchers(HttpMethod.GET, "/apiv1/quezaps/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/apiv1/themes/**")
                    .permitAll()
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final var configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "X-Participation-Token",
            "Last-Event-ID"));
    configuration.setExposedHeaders(
        List.of("Authorization", "Content-Disposition", "X-Participation-Token"));
    configuration.setAllowCredentials(true);
    final var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
