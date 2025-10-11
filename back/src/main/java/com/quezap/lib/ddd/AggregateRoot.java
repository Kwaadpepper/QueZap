package com.quezap.lib.ddd;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.quezap.lib.utils.UuidV7;

/**
 * Represents the base class for aggregate roots in a domain-driven design context.
 *
 * <p>Each aggregate root is assigned a unique identifier upon creation using {@link
 * UuidV7#randomUuid()}. Subclasses should extend this class to inherit the unique identity
 * functionality.
 */
public abstract class AggregateRoot implements Entity {
  protected final UUID id;
  protected final ZonedDateTime createdAt;

  /**
   * Protected constructor for AggregateRoot. Initializes the aggregate root with a randomly
   * generated UUID version 7. This ensures each aggregate root instance has a unique identifier.
   */
  protected AggregateRoot() {
    this.id = UuidV7.randomUuid();
    this.createdAt = extractCreatedAtFromId(id);
  }

  /** Protected constructor for reconstitution: use when hydrating an aggregate from persistence. */
  protected AggregateRoot(UUID id) {
    this.id = Objects.requireNonNull(id, "id");
    this.createdAt = extractCreatedAtFromId(id);
  }

  /**
   * Returns the unique identifier of this aggregate root.
   *
   * @return the UUID of the aggregate root
   */
  public abstract UUID getId();

  /**
   * Returns the creation timestamp of this aggregate root.
   *
   * @return the ZonedDateTime when the aggregate root was created
   */
  public abstract ZonedDateTime getCreatedAt();

  private ZonedDateTime extractCreatedAtFromId(UUID id) {
    return ZonedDateTime.ofInstant(UuidV7.extractInstant(id), ZoneOffset.UTC);
  }
}
