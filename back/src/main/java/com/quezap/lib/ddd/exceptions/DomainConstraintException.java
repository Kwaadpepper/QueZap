package com.quezap.lib.ddd.exceptions;

public class DomainConstraintException extends RuntimeException {
  private final DomainErrorCode error;

  public DomainConstraintException(DomainErrorCode error) {
    super(error.getMessage());
    this.error = error;
  }

  public DomainConstraintException(DomainErrorCode error, Throwable cause) {
    super(error.getMessage(), cause);
    this.error = error;
  }

  public DomainErrorCode getError() {
    return error;
  }
}
