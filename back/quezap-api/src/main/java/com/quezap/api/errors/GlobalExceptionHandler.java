package com.quezap.api.errors;

import java.time.Clock;
import java.time.ZonedDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.quezap.application.exceptions.ApplicationConstraintException;

@ControllerAdvice
public class GlobalExceptionHandler {
  private final Clock clock;

  GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(ApplicationConstraintException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResonseDto> handleDomainConstraintException(
      ApplicationConstraintException ex) {
    final var code = ex.getCode();
    final var message = ex.getMessage();

    final ErrorResonseDto errorResponse =
        new ErrorResonseDto(code, message, ZonedDateTime.now(clock));

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }
}
