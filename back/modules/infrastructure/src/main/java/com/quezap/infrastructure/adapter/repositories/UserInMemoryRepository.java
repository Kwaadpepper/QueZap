package com.quezap.infrastructure.adapter.repositories;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.quezap.domain.models.entities.User;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.domain.ports.repositories.UserRepository;
import com.quezap.infrastructure.adapter.spi.DataSource;

@Repository("userInMemoryRepository")
public class UserInMemoryRepository implements UserRepository, DataSource<User> {
  private final ConcurrentHashMap<UserId, User> storage = new ConcurrentHashMap<>();

  @Override
  public Optional<User> find(UserId id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public void persist(User entity) {
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
  public List<User> getAll() {
    return storage.values().stream().<User>map(this::clone).toList();
  }

  private User clone(User user) {
    return User.hydrate(user.getId(), user.getName(), user.getCredential(), user.getCreatedAt());
  }
}
