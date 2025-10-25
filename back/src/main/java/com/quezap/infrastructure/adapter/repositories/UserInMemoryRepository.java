package com.quezap.infrastructure.adapter.repositories;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.Nullable;

@Repository
public class UserInMemoryRepository implements UserRepository {
  private final ConcurrentHashMap<UserId, User> storage = new ConcurrentHashMap<>();

  @Override
  public @Nullable User find(UserId id) {
    return storage.get(id);
  }

  @Override
  public void save(User entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void update(User entity) {
    storage.put(entity.getId(), entity);
  }

  @Override
  public void delete(User entity) {
    storage.remove(entity.getId());
  }

  @Override
  public @Nullable User findByName(String name) {
    return storage.values().stream()
        .filter(user -> user.getName().equals(name))
        .findFirst()
        .orElse(null);
  }

  @Override
  public PageOf<User> findAll(Pagination pagination) {
    var users = storage.values().stream().toArray();
    long totalElements = users.length;
    long fromIndex = Math.min((pagination.pageNumber() - 1) * pagination.pageSize(), totalElements);
    long toIndex = Math.min(fromIndex + pagination.pageSize(), totalElements);
    var content = new ArrayList<User>();

    for (long i = fromIndex; i < toIndex; i++) {
      content.add((User) users[(int) i]);
    }

    return PageOf.of(pagination, content, totalElements);
  }
}
