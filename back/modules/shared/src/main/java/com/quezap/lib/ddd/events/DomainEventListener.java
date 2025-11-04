package com.quezap.lib.ddd.events;

@FunctionalInterface
public interface DomainEventListener<T extends DomainEvent<?>> {
  void onEvent(T event);
}
