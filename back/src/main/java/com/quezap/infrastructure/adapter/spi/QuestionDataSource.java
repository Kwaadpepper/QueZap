package com.quezap.infrastructure.adapter.spi;

import java.util.List;
import java.util.function.Function;

import com.quezap.domain.models.entities.Question;

public interface QuestionDataSource {
  <T> List<T> mapAll(Function<Question, T> mapper);
}
