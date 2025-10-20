package com.quezap.domain.port.services;

import com.quezap.domain.models.valueobjects.SessionCode;

public interface SessionCodeGenerator {
  SessionCode generateUniqueCode();
}
