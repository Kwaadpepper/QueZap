package com.quezap.api.v1.config.jackson;

import com.quezap.api.v1.deserializers.ThemeNameDeserializer;
import com.quezap.domain.models.valueobjects.ThemeName;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class DomainValueObjectModule extends SimpleModule {

  public DomainValueObjectModule() {
    super("DomainValueObjectModule");

    // API V1 Deserializers
    addDeserializer(ThemeName.class, new ThemeNameDeserializer());
  }
}
