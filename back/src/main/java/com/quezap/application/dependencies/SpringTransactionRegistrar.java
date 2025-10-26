package com.quezap.application.dependencies;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.quezap.lib.ddd.usecases.TransactionRegistrar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SpringTransactionRegistrar implements TransactionRegistrar {
  private static final Logger logger = LoggerFactory.getLogger(SpringTransactionRegistrar.class);
  private static final Object TSM_KEY = new Object();

  @Override
  @SuppressWarnings("unchecked")
  public void register(Object item) {

    Objects.requireNonNull(item);

    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      logger.error("Attempt to register rollback outside of DB transaction.");
      return;
    }

    // Utilise la classe concrète de l'objet pour typer le Set dans la Map
    final Set<Object> resourceSet = getOrCreateSetForType((Class<Object>) item.getClass());

    resourceSet.add(item);
  }

  @Override
  public <T> Set<T> retrieveAndUnbind(Class<T> type) {
    final Map<Class<T>, Set<T>> resourceMap = getOrCreateResourceMap();

    var retrievedSet = resourceMap.remove(type);

    if (retrievedSet == null) {
      retrievedSet = new HashSet<>();
    }
    return retrievedSet;
  }

  @SuppressWarnings("unchecked")
  private <T> Map<Class<T>, Set<T>> getOrCreateResourceMap() {

    Map<Class<T>, Set<T>> resourceMap =
        (Map<Class<T>, Set<T>>) TransactionSynchronizationManager.getResource(TSM_KEY);

    if (resourceMap != null) {
      return resourceMap;
    }

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      resourceMap = new ConcurrentHashMap<>();
      TransactionSynchronizationManager.bindResource(TSM_KEY, resourceMap);

      TransactionSynchronizationManager.registerSynchronization(
          new GenericResourceSynchronization());
      return resourceMap;
    }

    return Map.of();
  }

  /**
   * Hook de Spring pour nettoyer le ThreadLocal de la Map des ressources après la fin de la
   * transaction (COMMIT ou ROLLBACK), même si le Handler a oublié de récupérer un type de
   * ressource.
   */
  private static class GenericResourceSynchronization implements TransactionSynchronization {

    @Override
    public void afterCompletion(int status) {
      TransactionSynchronizationManager.unbindResource(TSM_KEY);
    }
  }

  private <T> Set<T> getOrCreateSetForType(Class<T> type) {
    final Map<Class<T>, Set<T>> resourceMap = getOrCreateResourceMap();

    return resourceMap.computeIfAbsent(type, k -> new HashSet<>());
  }
}
