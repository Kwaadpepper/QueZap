package com.quezap.application.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.quezap.lib.ddd.usecases.UnitOfWorkEvents;
import com.quezap.lib.ddd.usecases.UseCaseExecutor;
import com.quezap.lib.ddd.usecases.UseCaseHandler;
import com.quezap.lib.ddd.usecases.UseCaseInput;
import com.quezap.lib.ddd.usecases.UseCaseOutput;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TransactionalUseCaseExecutor implements UseCaseExecutor {
  private final Logger logger = LoggerFactory.getLogger(TransactionalUseCaseExecutor.class);

  @Override
  @Transactional
  public <I extends UseCaseInput, O extends UseCaseOutput> O execute(
      UseCaseHandler<I, O> useCaseHandler, @NonNull I usecaseInput) {

    final var useCaseName = AopUtils.getTargetClass(useCaseHandler).getCanonicalName();
    final var unitOfWork = new TransactionalUnitOfWork(logger, useCaseName);

    TransactionSynchronizationManager.registerSynchronization(unitOfWork);

    try {
      return useCaseHandler.handle(usecaseInput, unitOfWork);
    } catch (Exception e) {
      unitOfWork.setException(e);
      throw e;
    }
  }

  private static class TransactionalUnitOfWork
      implements UnitOfWorkEvents, TransactionSynchronization {
    private final Logger logger;
    private final String useCaseName;
    private final List<Runnable> onCommitHooks;
    private final List<Runnable> onRollbackHooks;
    private final AtomicReference<Throwable> exceptionHolder = new AtomicReference<>();

    TransactionalUnitOfWork(Logger logger, String useCaseName) {
      this.logger = logger;
      this.useCaseName = useCaseName;
      this.onCommitHooks = new ArrayList<>();
      this.onRollbackHooks = new ArrayList<>();
    }

    @Override
    public void onCommit(Runnable action) {
      this.onCommitHooks.add(action);
    }

    @Override
    public void onRollback(Runnable action) {
      this.onRollbackHooks.add(action);
    }

    void setException(Throwable e) {
      this.exceptionHolder.set(e);
    }

    @Override
    public void afterCompletion(int status) {
      switch (status) {
        case TransactionSynchronization.STATUS_COMMITTED -> {
          log("SUCCESS", Level.INFO);

          executeHooks(onCommitHooks, "commit");
        }
        case TransactionSynchronization.STATUS_ROLLED_BACK -> {
          log("FAILURE", Level.WARN);

          executeHooks(onRollbackHooks, "rollback");
        }
        default -> log("UNKNOWN", Level.ERROR);
      }
    }

    private void executeHooks(List<Runnable> hooks, String hookType) {
      if (hooks.isEmpty()) {
        return;
      }

      for (final var hook : hooks) {
        try {
          hook.run();
        } catch (Exception e) {
          log("Error executing %s hook: %s".formatted(hookType, e.getMessage()), Level.ERROR);
        }
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
}
