package com.quezap.lib.ddd.events;

import java.time.Instant;

public interface DomainEvent<T> {
  String routingKey();

  T payload();

  Instant timestamp();
}
