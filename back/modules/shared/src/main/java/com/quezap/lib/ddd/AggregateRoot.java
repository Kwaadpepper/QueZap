package com.quezap.lib.ddd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.quezap.lib.ddd.entities.Entity;
import com.quezap.lib.ddd.entities.EntityId;
import com.quezap.lib.ddd.events.DomainEvent;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.utils.UuidV7;

/**
 * Represents the base class for aggregate roots in a domain-driven design context.
 *
 * <p>Each aggregate root is assigned a unique identifier upon creation using {@link
 * UuidV7#randomUuid()}. Subclasses should extend this class to inherit the unique identity
 * functionality.
 */
public abstract class AggregateRoot<T extends EntityId> implements Entity {
  private final List<DomainEvent<?>> domainEvents = new ArrayList<>();

  protected final UUID rawId;
  protected final TimelinePoint createdAt;

  /**
   * Protected constructor for AggregateRoot. Initializes the aggregate root with a randomly
   * generated UUID version 7. This ensures each aggregate root instance has a unique identifier.
   */
  protected AggregateRoot() {
    this.rawId = UuidV7.randomUuid();
    this.createdAt = extractCreatedAtFromId(rawId);
  }

  /** Protected constructor for reconstitution: use when hydrating an aggregate from persistence. */
  protected AggregateRoot(UUID id) {
    this.rawId = Objects.requireNonNull(id, "id");
    this.createdAt = extractCreatedAtFromId(id);
  }

  /**
   * Returns the unique identifier of this aggregate root.
   *
   * @return the unique identifier as an instance of {@link EntityId}
   */
  public abstract T getId();

  /**
   * Returns the creation timestamp of this aggregate root.
   *
   * @return the creation timestamp as a {@link TimelinePoint}
   */
  public TimelinePoint getCreatedAt() {
    return createdAt;
  }

  private TimelinePoint extractCreatedAtFromId(UUID id) {
    return new TimelinePoint(UuidV7.extractInstant(id));
  }

  protected void registerEvent(DomainEvent<?> event) {
    this.domainEvents.add(event);
  }

  public List<DomainEvent<?>> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  public void clearDomainEvents() {
    this.domainEvents.clear();
  }
}
