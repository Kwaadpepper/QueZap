package com.quezap.api.v1.routes.questions;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.UserRepository;

import org.jspecify.annotations.Nullable;

final class QuestionAuthHelper {
  private QuestionAuthHelper() {}

  static Authentication requireAuthentication(@Nullable Authentication authentication) {
    final var effectiveAuth =
        authentication != null
            ? authentication
            : SecurityContextHolder.getContext().getAuthentication();
    if (effectiveAuth == null
        || !effectiveAuth.isAuthenticated()
        || effectiveAuth.getPrincipal() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    return effectiveAuth;
  }

  static UserId requireUserId(
      Authentication authentication, @Nullable UserRepository userRepository) {
    final var principalName = authentication.getName();
    if (principalName == null || principalName.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user available");
    }
    try {
      final var uuid = UUID.fromString(principalName);
      if (userRepository != null) {
        return userRepository.find(new UserId(uuid)).map(User::getId).orElse(new UserId(uuid));
      }
      return new UserId(uuid);
    } catch (IllegalArgumentException _) {
      if (userRepository == null) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "No authenticated user available");
      }
      return userRepository
          .findByName(principalName)
          .map(User::getId)
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.UNAUTHORIZED, "No authenticated user available"));
    }
  }

  static boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);
  }
}
