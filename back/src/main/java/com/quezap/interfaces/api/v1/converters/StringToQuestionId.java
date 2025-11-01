package com.quezap.interfaces.api.v1.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;

import org.jspecify.annotations.Nullable;

@Component
public class StringToQuestionId implements Converter<String, QuestionId> {

  @Override
  public @Nullable QuestionId convert(@Nullable String source) {
    if (source == null || source.isBlank()) {
      return null;
    }

    return QuestionId.fromString(source);
  }
}
