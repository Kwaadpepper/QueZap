package com.quezap.domain.port.repositories;

import java.util.Optional;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.Repository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public interface UserRepository extends Repository<User, UserId> {
  public Optional<User> findByName(String name);

  public PageOf<User> findAll(Pagination pagination);
}
