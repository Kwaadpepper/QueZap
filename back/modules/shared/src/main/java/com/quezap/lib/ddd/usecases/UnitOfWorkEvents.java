package com.quezap.lib.ddd.usecases;

public interface UnitOfWorkEvents {
  void onRollback(Runnable action);

  void onCommit(Runnable action);
}
