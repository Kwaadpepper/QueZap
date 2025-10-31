package com.quezap.domain.port.repositories;

import java.util.Optional;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.lib.ddd.repositories.Repository;

public interface SessionRepository extends Repository<Session, SessionId> {
  public Optional<Session> findByNumber(SessionNumber code);

  public Optional<Session> latestByNumber();
}
