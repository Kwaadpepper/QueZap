package com.quezap.application.config.jackson;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.interfaces.api.v1.deserializers.ThemeNameDeserializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class DomainValueObjectModule extends SimpleModule {

  public DomainValueObjectModule() {
    super("DomainValueObjectModule");

    // API V1 Deserializers
    addDeserializer(ThemeName.class, new ThemeNameDeserializer());
  }
}
