package com.quezap.lib.ddd.usecases;

import java.util.Set;

public interface TransactionRegistrar {
  void register(Object item);

  <T> Set<T> retrieveAndUnbind(Class<T> type);
}
