package com.quezap.infrastructure.adapter.repositories;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.port.repositories.UserRepository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

@Repository
public class UserInMemoryRepository implements UserRepository {
  private final ConcurrentHashMap<UserId, User> storage = new ConcurrentHashMap<>();

  @Override
  public Optional<User> find(UserId id) {
    return Optional.ofNullable(storage.get(id));
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
  public Optional<User> findByName(String name) {
    return storage.values().stream().filter(user -> user.getName().equals(name)).findFirst();
  }

  @Override
  public PageOf<User> findAll(Pagination pagination) {
    final var users = new ArrayList<>(storage.values());
    final var fromIndex = ((pagination.pageNumber() - 1) * pagination.pageSize());
    final var totalItems = users.size();

    if (fromIndex >= totalItems) {
      return PageOf.empty(pagination);
    }

    users.sort(createdAtComparator());

    final var toIndex = Math.min(fromIndex + pagination.pageSize(), totalItems);
    final var pageItems = users.subList((int) fromIndex, (int) toIndex);

    return PageOf.of(pagination, pageItems, (long) totalItems);
  }

  private Comparator<User> createdAtComparator() {
    return Comparator.comparing(User::getCreatedAt);
  }
}
