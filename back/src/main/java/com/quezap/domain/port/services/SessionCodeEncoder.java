package com.quezap.domain.port.services;

import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.domain.models.valueobjects.SessionNumber;

public interface SessionCodeEncoder {
  SessionCode encode(SessionNumber number);

  SessionNumber decode(SessionCode code);
}
