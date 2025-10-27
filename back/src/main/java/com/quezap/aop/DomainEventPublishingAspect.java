package com.quezap.aop;

import java.util.List;

import org.springframework.stereotype.Component;

import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.events.DomainEvent;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

/** Aspect to publish domain events after repository operations such as save, update, and delete. */
@Component
@Aspect
public class DomainEventPublishingAspect {
  private final UseCaseExecutor executor;

  public DomainEventPublishingAspect(UseCaseExecutor executor) {
    this.executor = executor;
  }

  @AfterReturning(
      pointcut =
          "execution(* com.quezap.lib.ddd.Repository.save(..)) || "
              + "execution(* com.quezap.lib.ddd.Repository.update(..))")
  public void afterSaveOrUpdate(JoinPoint joinPoint) {
    processAggregateForEvents(joinPoint);
  }

  @AfterReturning(pointcut = "execution(* com.quezap.lib.ddd.Repository.delete(..))")
  public void afterDelete(JoinPoint joinPoint) {
    processAggregateForEvents(joinPoint);
  }

  private void processAggregateForEvents(JoinPoint joinPoint) {

    if (joinPoint.getArgs().length < 1) {
      throw new IllegalArgumentException(
          "Expected at least one argument for repository method, but got none.");
    }

    final Object entity = joinPoint.getArgs()[0];

    if (!(entity instanceof AggregateRoot<?> aggregate)) {
      throw new IllegalArgumentException(
          "Expected argument to be an instance of AggregateRoot, but got: "
              + entity.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    List<DomainEvent<?>> events = (List<DomainEvent<?>>) (List<?>) aggregate.getDomainEvents();

    if (!events.isEmpty()) {
      events.forEach(executor::publish);

      aggregate.clearDomainEvents();
    }
  }
}
