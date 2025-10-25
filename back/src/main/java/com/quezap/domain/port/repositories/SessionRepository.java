package com.quezap.domain.port.repositories;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.domain.models.valueobjects.identifiers.SessionId;
import com.quezap.lib.ddd.Repository;

import org.jspecify.annotations.Nullable;

public interface SessionRepository extends Repository<Session, SessionId> {
  public @Nullable Session findByNumber(SessionNumber code);

  public @Nullable Session latestByCode();
}
