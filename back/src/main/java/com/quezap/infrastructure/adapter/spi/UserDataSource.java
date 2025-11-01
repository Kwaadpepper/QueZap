package com.quezap.infrastructure.adapter.spi;

import java.util.List;
import java.util.function.Function;

import com.quezap.domain.models.entities.User;

public interface UserDataSource {
  <T> List<T> mapAll(Function<User, T> mapper);
}
