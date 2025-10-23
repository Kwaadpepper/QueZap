package com.quezap.lib.ddd;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Generic repository interface for managing aggregate root entities.
 *
 * <p>Provides basic CRUD operations for entities identified by a unique identifier.
 *
 * @param <E> the type of aggregate root entity managed by the repository
 */
public interface Repository<E extends AggregateRoot> {
  /**
   * Retrieves an entity by its unique identifier.
   *
   * @param id the unique identifier of the entity to find
   * @return the entity corresponding to the given identifier, or {@code null} if not found
   */
  @Nullable E find(UUID id);

  /**
   * Persists the given entity to the repository.
   *
   * @param entity the entity to be saved
   */
  void save(E entity);

  /**
   * Updates the given entity in the repository.
   *
   * @param entity the entity to update
   */
  void update(E entity);

  /**
   * Deletes the specified entity from the repository.
   *
   * @param entity the entity to be deleted
   */
  void delete(E entity);
}
