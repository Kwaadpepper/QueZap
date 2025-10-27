package com.quezap.application.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.quezap.lib.ddd.events.DomainEvent;
import com.quezap.lib.ddd.events.DomainEventPublisher;
import com.quezap.lib.ddd.usecases.OnFailure;
import com.quezap.lib.ddd.usecases.OnSuccess;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TransactionalUseCaseExecutor implements UseCaseExecutor {
  private final Logger logger = LoggerFactory.getLogger(TransactionalUseCaseExecutor.class);

  private DomainEventPublisher domainEventPublisher;

  public TransactionalUseCaseExecutor(DomainEventPublisher domainEventPublisher) {
    this.domainEventPublisher = domainEventPublisher;
  }

  @Override
  @Transactional
  public <I extends UseCaseInput, O extends UseCaseOutput> O execute(
      UseCaseHandler<I, O> useCaseHandler, I usecaseInput) {

    final AtomicReference<Throwable> exceptionHolder = new AtomicReference<>();

    try {
      final var useCaseName = AopUtils.getTargetClass(useCaseHandler).getCanonicalName();
      final Runnable onSuccessHook =
          useCaseHandler instanceof OnSuccess onSuccessHandler ? onSuccessHandler::onSuccess : null;
      final Consumer<Throwable> onFailureHook =
          useCaseHandler instanceof OnFailure onFailureHandler ? onFailureHandler::onFailure : null;

      // Register transaction synchronization to handle commit/rollback events.
      TransactionSynchronizationManager.registerSynchronization(
          new UseCaseSynchronization(
              logger, useCaseName, exceptionHolder, onSuccessHook, onFailureHook));
      return useCaseHandler.handle(usecaseInput);
    } catch (Exception e) {

      // Capture the exception.
      exceptionHolder.set(e);

      throw e;
    }
  }

  @Override
  public void publish(DomainEvent<?> event) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {

      domainEventPublisher.publish(event);
      return;
    }

    @SuppressWarnings("unchecked")
    List<DomainEvent<?>> transactionEvents =
        (List<DomainEvent<?>>) TransactionSynchronizationManager.getResource(DomainEvent.class);

    if (transactionEvents == null) {
      transactionEvents = new ArrayList<>();
      // Link the list to the current transaction
      TransactionSynchronizationManager.bindResource(DomainEvent.class, transactionEvents);

      // Register synchronization to publish events after commit
      TransactionSynchronizationManager.registerSynchronization(
          new EventPublishingSynchronization(transactionEvents, domainEventPublisher));
    }

    transactionEvents.add(event);
  }

  private static class UseCaseSynchronization implements TransactionSynchronization {
    private final Logger logger;
    private final AtomicReference<Throwable> exceptionHolder;
    private final String useCaseName;
    private final @Nullable Runnable onSuccessHook;
    private final @Nullable Consumer<Throwable> onFailureHook;

    UseCaseSynchronization(
        Logger logger,
        String useCaseName,
        AtomicReference<Throwable> exceptionHolder,
        @Nullable Runnable onSuccessHook,
        @Nullable Consumer<Throwable> onFailureHook) {
      this.logger = logger;
      this.useCaseName = useCaseName;
      this.exceptionHolder = exceptionHolder;
      this.onSuccessHook = onSuccessHook;
      this.onFailureHook = onFailureHook;
    }

    @Override
    public void afterCompletion(int status) {
      switch (status) {
        case TransactionSynchronization.STATUS_COMMITTED -> handleCommit();
        case TransactionSynchronization.STATUS_ROLLED_BACK -> handleRollback();
        case TransactionSynchronization.STATUS_UNKNOWN -> handleUnknown();
        default -> log("transaction completed with unrecognized status: {}", Level.ERROR, status);
      }
    }

    private void handleCommit() {
      log("SUCCESS", Level.INFO);

      if (onSuccessHook != null) {
        onSuccessHook.run();
      }
    }

    private void handleRollback() {
      final var error =
          exceptionHolder.get() != null
              ? exceptionHolder.get()
              : new RuntimeException("Transaction rolled back (unknown cause)");
      final var errorMessage = error.getMessage();

      log("FAILURE", Level.WARN);

      if (errorMessage != null) {
        log("Cause: {}", Level.DEBUG, errorMessage);
      }

      if (onFailureHook != null) {
        onFailureHook.accept(error);
      }
    }

    private void handleUnknown() {
      log("UNKNOWN", Level.ERROR);

      if (onFailureHook != null) {
        onFailureHook.accept(new RuntimeException("Transaction status unknown"));
      }
    }

    private void log(String message, Level level, Object... args) {
      final var prefixedMessage = "[UseCase: %s] %s".formatted(useCaseName, message);
      switch (level) {
        case INFO -> logger.info(prefixedMessage, args);
        case WARN -> logger.warn(prefixedMessage, args);
        case ERROR -> logger.error(prefixedMessage, args);
        default -> logger.debug(prefixedMessage, args);
      }
    }

    private enum Level {
      DEBUG,
      INFO,
      WARN,
      ERROR
    }
  }

  private static class EventPublishingSynchronization implements TransactionSynchronization {
    private final List<DomainEvent<?>> events;
    private final DomainEventPublisher publisher;

    public EventPublishingSynchronization(
        List<DomainEvent<?>> events, DomainEventPublisher publisher) {
      this.events = events;
      this.publisher = publisher;
    }

    @Override
    public void afterCompletion(int status) {
      TransactionSynchronizationManager.unbindResourceIfPossible(DomainEvent.class);
    }

    @Override
    public void afterCommit() {
      events.forEach(publisher::publish);
    }
  }
}
