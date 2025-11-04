package com.quezap.lib.utils;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

public class EmptyConsumer implements Consumer<Object> {
  @Override
  public void accept(@Nullable Object t) {
    // Do nothing
  }

  public static <T> Consumer<T> accept() {
    return new EmptyConsumer()::accept;
  }
}
