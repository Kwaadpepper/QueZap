package com.quezap.infrastructure.adapter.spi;

import java.util.List;
import java.util.function.Function;

import com.quezap.domain.models.entities.Theme;

public interface ThemeDataSource {
  <T> List<T> mapAll(Function<Theme, T> mapper);
}
