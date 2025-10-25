package com.quezap.lib.ddd.exceptions;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

public class DomainConstraintException extends RuntimeException {
  private final DomainErrorCode error;

  public DomainConstraintException(DomainErrorCode error) {
    super(error.getMessage());
    this.error = error;
  }

  public DomainConstraintException(DomainErrorCode error, String message) {
    super(message);
    this.error = error;
  }

  public DomainConstraintException(DomainErrorCode error, Throwable cause) {
    super(error.getMessage(), cause);
    this.error = error;
  }

  public Integer getCode() {
    return error.getCode();
  }

  @Override
  public @NonNull String getMessage() {
    return Objects.requireNonNull(super.getMessage());
  }
}
