package com.quezap.domain.port.repositories;

import com.quezap.domain.models.entities.Session;
import com.quezap.domain.models.valueobjects.SessionCode;
import com.quezap.lib.ddd.Repository;

import org.eclipse.jdt.annotation.Nullable;

public interface SessionRepository extends Repository<Session> {
  public @Nullable Session findByCode(SessionCode code);

  public @Nullable Session latestByCode();
}
