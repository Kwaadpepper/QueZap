package com.quezap.domain.port.repositories;

import com.quezap.domain.models.entities.User;
import com.quezap.lib.ddd.Repository;

import org.eclipse.jdt.annotation.Nullable;

public interface UserRepository extends Repository<User> {
  public @Nullable User findByName(String name);
}
