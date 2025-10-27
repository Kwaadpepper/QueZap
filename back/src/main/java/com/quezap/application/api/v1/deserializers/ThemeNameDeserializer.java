package com.quezap.application.api.v1.deserializers;

import java.io.IOException;

import com.quezap.domain.models.valueobjects.ThemeName;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ThemeNameDeserializer extends JsonDeserializer<ThemeName> {

  @Override
  public ThemeName deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    final var nameValue = p.getText();

    return new ThemeName(nameValue);
  }
}
