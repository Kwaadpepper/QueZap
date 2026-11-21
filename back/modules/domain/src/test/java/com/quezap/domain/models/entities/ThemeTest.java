package com.quezap.domain.models.entities;

import java.util.UUID;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.utils.UuidV7;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ThemeTest {

  @Test
  void canInstantiate() {
    // GIVEN
    var value = new ThemeName("Science");

    // WHEN
    new Theme(value);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canHydrate() {
    // GIVEN
    var id = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var value = new ThemeName("Science");

    // WHEN
    Theme.hydrate(id, value);

    // THEN
    Assertions.assertDoesNotThrow(() -> {});
  }

  @Test
  void canInstantiateAndHydrateWithCreatedBy() {
    var id = new ThemeId(UUID.fromString("017f5a80-7e6d-7e6e-0000-000000000000"));
    var name = new ThemeName("Science");
    var creator = new UserId(UuidV7.randomUuid());

    var created = new Theme(name, creator);
    Assertions.assertEquals(creator, created.getCreatedBy());

    var hydrated = Theme.hydrate(id, name, creator);
    Assertions.assertEquals(creator, hydrated.getCreatedBy());
  }
}
