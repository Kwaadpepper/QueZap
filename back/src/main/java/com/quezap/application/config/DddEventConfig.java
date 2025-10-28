package com.quezap.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.quezap.aop.DomainEventPublishingAspect;
import com.quezap.lib.ddd.events.DomainEventPublisher;

/**
 * Configuration for Domain-Driven Design specific infrastructure. This class explicitly enables and
 * configures the mechanism for publishing domain events from AggregateRoots.
 */
@Configuration
@EnableAspectJAutoProxy
public class DddEventConfig {

  @Bean
  DomainEventPublishingAspect domainEventPublishingAspect(
      DomainEventPublisher domainEventPublisher) {
    return new DomainEventPublishingAspect(domainEventPublisher);
  }
}
