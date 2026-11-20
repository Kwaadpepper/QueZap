package com.quezap.api.errors;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.quezap.application.exceptions.ApplicationConstraintException;
import com.quezap.domain.models.valueobjects.sessions.SessionDomainException;
import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final Set<Integer> NOT_FOUND_ERROR_CODES =
      Set.of(
          1101, // DeleteUserError.NO_SUCH_USER
          1201, // UpdateUserPasswordError.NO_SUCH_USER
          1301, // FindUserError.NO_SUCH_USER
          2103, // StartSessionError.NO_SUCH_SESSION
          2201, // AddSessionError.NO_SUCH_USER
          2401, // AddQuestionError.NO_SUCH_QUESTION (sessions)
          2402, // AddQuestionError.NO_SUCH_SESSION
          2501, // RemoveQuestionError.NO_SUCH_QUESTION
          2502, // RemoveQuestionError.NO_SUCH_SESSION
          2601, // AnswerSessionError.NO_SUCH_SESSION
          2701, // RemoveThemeError.THEME_DOES_NOT_EXISTS
          2801, // RenameThemeError.THEME_DOES_NOT_EXISTS
          2901, // AddQuestionError.THEME_DOES_NOT_EXISTS
          3101 // DeleteQuestionError.QUESTION_NOT_FOUND
          );

  private static final Set<Integer> CONFLICT_ERROR_CODES =
      Set.of(
          1001, // AddUserError.IDENTIFIER_ALREADY_TAKEN
          1002, // AddUserError.USER_NAME_ALREADY_TAKEN
          2002, // ParticipateSessionError.NAME_ALREADY_TAKEN
          2702, // RemoveThemeError.THEME_HAS_QUESTIONS
          2802, // RenameThemeError.THEME_NAME_ALREADY_EXISTS
          3001 // AddThemeError.THEME_ALREADY_EXISTS
          );

  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(ApplicationConstraintException.class)
  public ResponseEntity<ErrorResonseDto> handleApplicationConstraintException(
      ApplicationConstraintException ex) {
    final var code = ex.getCode();
    final var message = ex.getMessage();

    final var status = resolveApplicationConstraintStatus(code, message);
    final var errorResponse = new ErrorResonseDto(code, message, ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, status);
  }

  private HttpStatus resolveApplicationConstraintStatus(Integer code, String message) {
    if (NOT_FOUND_ERROR_CODES.contains(code)) {
      return HttpStatus.NOT_FOUND;
    }
    if (CONFLICT_ERROR_CODES.contains(code)) {
      return HttpStatus.CONFLICT;
    }

    final var lower = message.toLowerCase(Locale.ROOT);
    if (lower.contains("not found")
        || lower.contains("no such")
        || lower.contains("introuvable")
        || lower.contains("does not exist")
        || lower.contains("no theme exists")) {
      return HttpStatus.NOT_FOUND;
    }
    if (lower.contains("already")
        || lower.contains("conflict")
        || lower.contains("existant")
        || lower.contains("cannot be removed")) {
      return HttpStatus.CONFLICT;
    }
    return HttpStatus.BAD_REQUEST;
  }

  @ExceptionHandler(SessionDomainException.class)
  public ResponseEntity<ErrorResonseDto> handleSessionDomainException(SessionDomainException ex) {
    final var status =
        switch (ex.category()) {
          case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
          case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
          case FORBIDDEN -> HttpStatus.FORBIDDEN;
          case NOT_FOUND -> HttpStatus.NOT_FOUND;
          case CONFLICT -> HttpStatus.CONFLICT;
        };
    final var errorResponse =
        new ErrorResonseDto(status.value(), ex.getMessage(), ZonedDateTime.now(clock));
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

  @ExceptionHandler({
    IllegalDomainStateException.class,
    IllegalArgumentException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    ConstraintViolationException.class
  })
  public ResponseEntity<ErrorResonseDto> handleBadRequestExceptions(Exception ex) {
    final var message =
        ex instanceof HttpMessageNotReadableException
            ? "Malformed request body"
            : (ex.getMessage() != null ? ex.getMessage() : "Invalid request");
    final var errorResponse =
        new ErrorResonseDto(HttpStatus.BAD_REQUEST.value(), message, ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResonseDto> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    logger.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    final var errorResponse =
        new ErrorResonseDto(
            HttpStatus.CONFLICT.value(),
            "Resource conflict or constraint violation",
            ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResonseDto> handleAccessDeniedException(AccessDeniedException ex) {
    final var errorResponse =
        new ErrorResonseDto(
            HttpStatus.FORBIDDEN.value(), ex.getMessage(), ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResonseDto> handleUnexpectedException(Exception ex) {
    logger.error("Unhandled internal server error", ex);
    final var errorResponse =
        new ErrorResonseDto(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected internal error occurred",
            ZonedDateTime.now(clock));
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
