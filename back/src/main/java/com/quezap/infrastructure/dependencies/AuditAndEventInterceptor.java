package com.quezap.infrastructure.dependencies;

import java.util.ArrayList;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.TracksUpdatedAt;
import com.quezap.lib.ddd.entities.EntityId;
import com.quezap.lib.ddd.events.DomainEventPublisher;
import com.quezap.lib.ddd.repositories.RepositoryMethodInterceptor;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Component
public final class AuditAndEventInterceptor implements RepositoryMethodInterceptor {
  private final DomainEventPublisher publisher;

  AuditAndEventInterceptor(@NonNull DomainEventPublisher publisher) {
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public @Nullable Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
    final var method = invocation.getMethod();
    final var name = method.getName();

    if (!AUDITED_METHODS.contains(name)) {
      return invocation.proceed();
    }

    if (invocation.getArguments().length != 1) {
      throw new IllegalStateException(
          "Repository method interception expected to have exactly one argument");
    }

    final var arg = invocation.getArguments()[0];

    // * Changes updatedAt before persisting
    if ((arg instanceof TracksUpdatedAt trackable) && name.equals(PERSIST_METHOD_NAME)) {
      trackable.setUpdateAt(TimelinePoint.now());
    }

    // * Publishes Domain Events for AggregateRoots
    if (arg instanceof AggregateRoot<? extends EntityId> aggregate) {
      final var result = invocation.proceed();

      publishDomainEventsAfterCommit(aggregate);

      return result;
    }

    return invocation.proceed();
  }

  @Override
  public <T extends EntityId> void publishDomainEventsAfterCommit(
      @NonNull AggregateRoot<T> aggregate) {

    final var events = new ArrayList<>(aggregate.getDomainEvents());

    if (events.isEmpty()) {
      return;
    }

    aggregate.clearDomainEvents();

    // * Publishes events immediately if no transaction is active
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      events.forEach(publisher::publish);
      return;
    }

    // * Registers synchronization to publish events after transaction commit
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            events.forEach(publisher::publish);
          }
        });
  }
}
