package com.quezap.application.ddd;

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.events.DomainEvent;
import com.quezap.lib.ddd.events.DomainEventPublisher;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

/** Aspect to publish domain events after repository operations such as save, update, and delete. */
@Aspect
public class DomainEventPublishingAspect {

  private final DomainEventPublisher domainEventPublisher;

  public DomainEventPublishingAspect(DomainEventPublisher domainEventPublisher) {
    this.domainEventPublisher = domainEventPublisher;
  }

  @After(
      "execution(* com.quezap.lib.ddd.Repository.save(..))"
          + " || execution(* com.quezap.lib.ddd.Repository.update(..))"
          + " || execution(* com.quezap.lib.ddd.Repository.delete(..))"
          + " && args(aggregate)")
  public void publishEventsAfterPersistence(AggregateRoot<?> aggregate) {
    List<DomainEvent<?>> events = new ArrayList<>(aggregate.getDomainEvents());
    aggregate.clearDomainEvents();

    if (events.isEmpty()) {
      return;
    }

    // Publish events if not in a transaction
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      events.forEach(domainEventPublisher::publish);
      return;
    }

    // Register synchronization to publish events after transaction commit
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            events.forEach(domainEventPublisher::publish);
          }
        });
  }
}
