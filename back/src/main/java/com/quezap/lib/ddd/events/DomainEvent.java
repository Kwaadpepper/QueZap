package com.quezap.lib.ddd.events;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface DomainEvent<T> {
  String routingKey();

  T payload();

  Instant timestamp();
}
