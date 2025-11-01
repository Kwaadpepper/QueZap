package com.quezap.domain.ports.services;

import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.domain.models.valueobjects.participations.ParticipationToken;

public interface ParticipationTokenGenerator {
  ParticipationToken generate(SessionId sessionId);
}
