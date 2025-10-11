package com.quezap.lib.utils;

import com.quezap.domain.exceptions.IllegalDomainStateException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DomainTest {
  @Test
  void canCheckDonainState() {
    // GIVEN
    var shouldNotThrow = true;

    // WHEN / THEN
    Domain.checkDomain(() -> shouldNotThrow, "This should not throw");
  }

  @Test
  void willThrowIfDomainStateIsIllegal() {
    // GIVEN
    var shouldThrow = false;

    // WHEN / THEN
    Assertions.assertThatThrownBy(() -> Domain.checkDomain(() -> shouldThrow, "This should throw"))
        .isInstanceOf(IllegalDomainStateException.class)
        .hasMessage("This should throw");
  }
}
