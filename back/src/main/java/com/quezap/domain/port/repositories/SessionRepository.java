package com.quezap.domain.port.repositories;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionNumber;
import com.quezap.lib.ddd.Repository;

import org.eclipse.jdt.annotation.Nullable;

public interface SessionRepository extends Repository<Session> {
  public @Nullable Session findByNumber(SessionNumber code);

  public @Nullable Session latestByCode();
}
