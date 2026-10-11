package com.quezap.api.errors;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.application.exceptions.ApplicationConstraintException;

@ControllerAdvice
public class GlobalExceptionHandler {
  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(ApplicationConstraintException.class)
  public ResponseEntity<ErrorResonseDto> handleApplicationConstraintException(
      ApplicationConstraintException ex) {
    final var code = ex.getCode();
    final var message = ex.getMessage();

    var status = HttpStatus.BAD_REQUEST;
    if (message != null) {
      final var lower = message.toLowerCase(Locale.ROOT);
      if (lower.contains("not found")
          || lower.contains("no such")
          || lower.contains("introuvable")) {
        status = HttpStatus.NOT_FOUND;
      } else if (lower.contains("already")
          || lower.contains("conflict")
          || lower.contains("existant")) {
        status = HttpStatus.CONFLICT;
      }
    }

    final var errorResponse = new ErrorResonseDto(code, message, ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, status);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResonseDto> handleResponseStatusException(ResponseStatusException ex) {
    final var status = HttpStatus.valueOf(ex.getStatusCode().value());
    final var message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
    final var errorResponse =
        new ErrorResonseDto(status.value(), message, ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, status);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResonseDto> handleValidationException(
      MethodArgumentNotValidException ex) {
    final var firstError = ex.getBindingResult().getAllErrors().stream().findFirst();
    final var message = firstError.map(e -> e.getDefaultMessage()).orElse("Validation error");
    final var errorResponse =
        new ErrorResonseDto(HttpStatus.BAD_REQUEST.value(), message, ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResonseDto> handleAccessDeniedException(AccessDeniedException ex) {
    final var errorResponse =
        new ErrorResonseDto(
            HttpStatus.FORBIDDEN.value(), ex.getMessage(), ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResonseDto> handleIllegalArgumentException(
      IllegalArgumentException ex) {
    final var errorResponse =
        new ErrorResonseDto(
            HttpStatus.BAD_REQUEST.value(), ex.getMessage(), ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }
}
