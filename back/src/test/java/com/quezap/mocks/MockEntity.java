package com.quezap.mocks;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public abstract class MockEntity {
  public static <T> T eq(T value) {
    return Mockito.eq(value);
  }

  public static <T> Optional<T> optionalOfNullable(T object) {
    return Optional.ofNullable(object);
  }

  public static <T> Optional<T> optional(Class<@NonNull T> classT) {
    return Optional.of(mock(classT));
  }

  public static <T> Optional<T> optional(@NonNull T object) {
    return Optional.of(object);
  }

  public static <T> Optional<T> optional() {
    return Optional.empty();
  }

  public static <T> MockedStatic<T> mockStatic(Class<@NonNull T> classToMock) {
    return Mockito.mockStatic(classToMock);
  }

  public static <T> @NonNull T mock(Class<@NonNull T> classT) {
    return Mockito.mock(classT);
  }

  public static <T, A> @NonNull T mock(Class<@NonNull T> classT, Answer<A> defaultAnswer) {
    return Mockito.mock(classT, defaultAnswer);
  }

  public static <T> @NonNull T any(Class<@NonNull T> classT) {
    return Mockito.any(classT);
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  public static <T> T any() {
    return Mockito.any();
  }
}
