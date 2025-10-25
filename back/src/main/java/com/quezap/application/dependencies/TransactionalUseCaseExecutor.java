package com.quezap.application.dependencies;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

  @Override
  @Transactional
  public <I extends UseCaseInput, O extends @Nullable UseCaseOutput> O execute(
      UseCaseHandler<I, O> useCaseHandler, I usecaseInput) {

    final Runnable onSuccessHook =
        useCaseHandler instanceof OnSuccess onSuccessHandler ? onSuccessHandler::onSuccess : null;
    final Consumer<Throwable> onFailureHook =
        useCaseHandler instanceof OnFailure onFailureHandler ? onFailureHandler::onFailure : null;
    final AtomicReference<Throwable> exceptionHolder = new AtomicReference<>();

    // Register transaction synchronization to handle commit/rollback events.
    TransactionSynchronizationManager.registerSynchronization(
        new UseCaseSynchronization(exceptionHolder, logger, onSuccessHook, onFailureHook));

    try {
      return useCaseHandler.handle(usecaseInput);
    } catch (Exception e) {

      // Capture the exception.
      exceptionHolder.set(e);

      throw e;
    }
  }

  private static class UseCaseSynchronization implements TransactionSynchronization {
    private final AtomicReference<Throwable> exceptionHolder;
    private final Logger logger;
    private final @Nullable Runnable onSuccessHook;
    private final @Nullable Consumer<Throwable> onFailureHook;

    public UseCaseSynchronization(
        AtomicReference<Throwable> exceptionHolder,
        Logger logger,
        @Nullable Runnable onSuccessHook,
        @Nullable Consumer<Throwable> onFailureHook) {
      this.onSuccessHook = onSuccessHook;
      this.onFailureHook = onFailureHook;
      this.exceptionHolder = exceptionHolder;
      this.logger = logger;
    }

    @Override
    public void afterCompletion(int status) {
      switch (status) {
        case TransactionSynchronization.STATUS_COMMITTED -> handleCommit();
        case TransactionSynchronization.STATUS_ROLLED_BACK -> handleRollback();
        case TransactionSynchronization.STATUS_UNKNOWN -> handleUnknown();
        default -> logger.error("Unknown transaction status: {}", status);
      }
    }

    private void handleCommit() {
      logger.info("use case transaction committed successfully.");

      if (onSuccessHook != null) {
        onSuccessHook.run();
      }
    }

    private void handleRollback() {
      final var error =
          exceptionHolder.get() != null
              ? exceptionHolder.get()
              : new RuntimeException("Transaction rolled back (unknown cause)");

      if (onFailureHook != null) {
        onFailureHook.accept(error);
      }
    }

    private void handleUnknown() {
      logger.error("use case transaction status is unknown.");

      if (onFailureHook != null) {
        onFailureHook.accept(new RuntimeException("Transaction status unknown"));
      }
    }
  }
}
