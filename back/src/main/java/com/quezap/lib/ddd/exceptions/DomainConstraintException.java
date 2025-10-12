package com.quezap.lib.ddd.exceptions;

public class DomainConstraintException extends RuntimeException {
  public DomainConstraintException(String message) {
    super(message);
  }

  public DomainConstraintException(String message, Throwable cause) {
    super(message, cause);
  }

  public DomainConstraintException(Throwable cause) {
    super(cause);
  }

  public DomainConstraintException() {
    super();
  }
}
