package com.quezap.infrastructure.adapter.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawPassword;
import com.quezap.domain.port.services.PasswordHasher;

@Service
public class PasswordHasherImpl implements PasswordHasher {
  private final PasswordEncoder passwordEncoder;

  public PasswordHasherImpl(final PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public HashedPassword hash(RawPassword rawPassword) {
    final var passwordValue = rawPassword.value();

    return new HashedPassword(passwordEncoder.encode(passwordValue));
  }

  @Override
  public boolean verify(PasswordCandidate password, HashedPassword hashedPassword) {
    final var passwordValue = password.value();
    final var hashedPasswordValue = hashedPassword.value();

    return passwordEncoder.matches(passwordValue, hashedPasswordValue);
  }
}
