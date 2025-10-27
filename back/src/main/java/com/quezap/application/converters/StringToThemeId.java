package com.quezap.application.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;

@Component
public class StringToThemeId implements Converter<String, ThemeId> {

  @Override
  public ThemeId convert(String source) {
    if (source == null || source.isBlank()) {
      return null;
    }

    return ThemeId.fromString(source);
  }
}
