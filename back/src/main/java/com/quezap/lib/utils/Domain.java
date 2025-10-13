package com.quezap.lib.utils;

import java.util.function.BooleanSupplier;

import com.quezap.lib.ddd.exceptions.IllegalDomainStateException;

/** Utility class for domain validation logic. */
public final class Domain {
  private Domain() {}

  /**
   * Checks a domain predicate and throws an IllegalDomainStateException if the predicate evaluates
   * to false.
   */
  public static void checkDomain(BooleanSupplier predicate, String message)
      throws IllegalDomainStateException {
    if (!predicate.getAsBoolean()) {
      throw new IllegalDomainStateException(message);
    }
  }
}
