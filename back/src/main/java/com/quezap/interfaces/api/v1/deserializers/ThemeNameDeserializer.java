package com.quezap.interfaces.api.v1.deserializers;

import java.io.IOException;

import com.quezap.domain.models.valueobjects.ThemeName;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.jspecify.annotations.Nullable;

public class ThemeNameDeserializer extends JsonDeserializer<ThemeName> {

  @Override
  public ThemeName deserialize(@Nullable JsonParser p, @Nullable DeserializationContext ctxt)
      throws IOException {

    if (p == null) {
      throw new IOException("JsonParser is null");
    }

    final var nameValue = p.getText();

    return new ThemeName(nameValue);
  }
}
