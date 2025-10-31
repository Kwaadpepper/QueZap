package com.quezap.application.dependencies;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quezap.lib.ddd.repositories.Repository;
import com.quezap.lib.ddd.repositories.RepositoryMethodInterceptor;

import org.jspecify.annotations.Nullable;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RepositoryProxy implements BeanPostProcessor {
  private final RepositoryMethodInterceptor interceptor;

  public RepositoryProxy(RepositoryMethodInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Override
  public Object postProcessAfterInitialization(@Nullable Object bean, @Nullable String beanName)
      throws BeansException {

    if (bean == null) {
      throw new IllegalArgumentException("Bean cannot be null");
    }

    if (beanName == null) {
      throw new IllegalArgumentException("Bean name cannot be null");
    }

    if (!(bean instanceof Repository)) {
      return bean;
    }

    final var proxyFactory = new ProxyFactory(bean);

    // * JDK dynamic proxy (interfaces)
    proxyFactory.setProxyTargetClass(false);
    proxyFactory.addAdvice(interceptor);

    return proxyFactory.getProxy();
  }
}
