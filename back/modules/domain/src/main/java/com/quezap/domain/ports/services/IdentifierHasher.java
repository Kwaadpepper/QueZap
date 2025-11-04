package com.quezap.domain.ports.services;

import com.quezap.domain.models.valueobjects.auth.HashedIdentifier;
import com.quezap.domain.models.valueobjects.auth.RawIdentifier;

public interface IdentifierHasher {
  HashedIdentifier hash(RawIdentifier identifier);
}
