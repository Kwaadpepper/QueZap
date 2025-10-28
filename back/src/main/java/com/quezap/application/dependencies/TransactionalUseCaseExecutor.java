package com.quezap.application.dependencies;

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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TransactionalUseCaseExecutor implements UseCaseExecutor {
  private final Logger logger = LoggerFactory.getLogger(TransactionalUseCaseExecutor.class);

  @Override
  @Transactional
  public <I extends UseCaseInput, O extends UseCaseOutput> O execute(
      UseCaseHandler<I, O> useCaseHandler, I usecaseInput) {

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
    private @Nullable Runnable onCommitHook;
    private @Nullable Runnable onRollbackHook;
    private final AtomicReference<Throwable> exceptionHolder = new AtomicReference<>();

    TransactionalUnitOfWork(Logger logger, String useCaseName) {
      this.logger = logger;
      this.useCaseName = useCaseName;
    }

    @Override
    public void onCommit(Runnable action) {
      this.onCommitHook = action;
    }

    @Override
    public void onRollback(Runnable action) {
      this.onRollbackHook = action;
    }

    public void setException(Throwable e) {
      this.exceptionHolder.set(e);
    }

    @Override
    public void afterCompletion(int status) {
      switch (status) {
        case TransactionSynchronization.STATUS_COMMITTED:
          log("SUCCESS", Level.INFO);
          if (onCommitHook != null) {
            onCommitHook.run();
          }
          break;
        case TransactionSynchronization.STATUS_ROLLED_BACK:
          log("FAILURE", Level.WARN);
          if (onRollbackHook != null) {
            onRollbackHook.run();
          }
          break;
        default:
          log("UNKNOWN", Level.ERROR);
          break;
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
