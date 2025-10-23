package com.quezap.domain.port.repositories;

import com.quezap.domain.models.entities.User;
import com.quezap.lib.ddd.Repository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.PageRequest;

import org.jspecify.annotations.Nullable;

public interface UserRepository extends Repository<User> {
  public @Nullable User findByName(String name);

  public PageOf<User> findAll(PageRequest pageRequest);
}
