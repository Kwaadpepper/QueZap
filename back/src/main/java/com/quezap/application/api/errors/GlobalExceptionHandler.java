package com.quezap.application.api.errors;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.quezap.lib.ddd.exceptions.DomainConstraintException;

/** Gère les exceptions de Domaine et les mappe aux statuts HTTP appropriés. */
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DomainConstraintException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND) // Optionnel, mais clair
  public ResponseEntity<ErrorResonseDto> handleDomainConstraintException(
      DomainConstraintException ex) {
    final var code = ex.getCode();
    final var message = ex.getMessage();

    final ErrorResonseDto errorResponse =
        new ErrorResonseDto(code, message, ZonedDateTime.now(ZoneId.of("UTC")));

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }
}
