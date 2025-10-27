package com.quezap.lib.ddd.events;

@FunctionalInterface
public interface DomainEventPublisher {
  void publish(DomainEvent<?> event);
}
