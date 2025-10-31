package com.quezap.lib.ddd.repositories;

import java.util.Optional;

import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.entities.EntityId;

import org.jspecify.annotations.NonNull;

/**
 * Generic repository interface for managing aggregate root entities.
 *
 * <p>Provides basic CRUD operations for entities identified by a unique identifier.
 *
 * @param <E> the type of aggregate root entity managed by the repository
 * @param <I> the type of the unique identifier of the entity
 */
public interface Repository<E extends AggregateRoot<I>, I extends EntityId> {
  /**
   * Retrieves an entity by its unique identifier.
   *
   * @param id the unique identifier of the entity to find
   * @return the entity corresponding to the given identifier, or {@code null} if not found
   */
  Optional<E> find(@NonNull I id);

  /**
   * Persists the given entity to the repository.
   *
   * @param entity the entity to be saved
   */
  void persist(@NonNull E entity);

  /**
   * Deletes the specified entity from the repository.
   *
   * @param entity the entity to be deleted
   */
  void delete(@NonNull E entity);
}
