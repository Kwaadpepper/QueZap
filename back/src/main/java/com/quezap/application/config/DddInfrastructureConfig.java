package com.quezap.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.quezap.aop.DomainEventPublishingAspect;
import com.quezap.application.dependencies.TransactionalUseCaseExecutor;
import com.quezap.lib.ddd.events.DomainEventPublisher;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

@Configuration
@EnableAspectJAutoProxy
public class DddInfrastructureConfig {

  @Bean
  UseCaseExecutor useCaseExecutor() {
    return new TransactionalUseCaseExecutor();
  }

  @Bean
  DomainEventPublishingAspect domainEventPublishingAspect(
      DomainEventPublisher domainEventPublisher) {
    return new DomainEventPublishingAspect(domainEventPublisher);
  }
}
