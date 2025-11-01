package com.quezap.infrastructure.events;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import com.quezap.lib.ddd.events.DomainEvent;
import com.quezap.lib.ddd.events.DomainEventPublisher;

@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
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
