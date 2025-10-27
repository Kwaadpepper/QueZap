package com.quezap.application.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.quezap.lib.ddd.events.DomainEvent;
import com.quezap.lib.ddd.events.DomainEventPublisher;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {
  private final ApplicationEventPublisher applicationEventPublisher;

  public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publish(DomainEvent<?> event) {
    this.applicationEventPublisher.publishEvent(event);
  }
}
