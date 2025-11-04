package com.quezap.domain.ports.services;

import com.quezap.domain.models.valueobjects.auth.HashedPassword;
import com.quezap.domain.models.valueobjects.auth.PasswordCandidate;
import com.quezap.domain.models.valueobjects.auth.RawPassword;

public interface PasswordHasher {
  HashedPassword hash(RawPassword password);

  boolean verify(PasswordCandidate passwordCandidate, HashedPassword hashedPassword);
}
