package com.quezap.lib.ddd.repositories;

import java.util.Set;

import com.quezap.lib.ddd.AggregateRoot;
import com.quezap.lib.ddd.entities.EntityId;

import org.aopalliance.intercept.MethodInterceptor;

public interface RepositoryMethodInterceptor extends MethodInterceptor {
  public static final String PERSIST_METHOD_NAME = "persist";
  public static final Set<String> AUDITED_METHODS = Set.<String>of(PERSIST_METHOD_NAME, "delete");

  public <T extends EntityId> void publishDomainEventsAfterCommit(AggregateRoot<T> aggregate);
}
