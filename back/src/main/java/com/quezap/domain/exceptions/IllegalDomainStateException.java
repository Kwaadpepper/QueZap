package com.quezap.domain.exceptions;

public class IllegalDomainStateException extends RuntimeException {
  public IllegalDomainStateException(String message) {
    super(message);
  }

  public IllegalDomainStateException(String message, Throwable cause) {
    super(message, cause);
  }

  public IllegalDomainStateException(Throwable cause) {
    super(cause);
  }

  public IllegalDomainStateException() {
    super();
  }
}
