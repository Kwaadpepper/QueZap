package com.quezap.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextNormalizerTest {

  @Test
  @DisplayName("Should handle null and empty strings gracefully")
  void shouldHandleNullAndEmpty() {
    assertThat(TextNormalizer.normalize(null)).isEmpty();
    assertThat(TextNormalizer.normalize("")).isEmpty();
    assertThat(TextNormalizer.normalize("   ")).isEmpty();
    assertThat(TextNormalizer.matches(null, "tulipe")).isFalse();
    assertThat(TextNormalizer.matches("tulipe", null)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "tulipe",
        "Tulipe",
        "TULIPE",
        "tuliipe",
        "tulllipe",
        "tuuliippe",
        "túlipé",
        "tulipe 🌷",
        "  tulipe ! ",
        "tulipe...",
        "t-u-l-i-p-e",
      })
  @DisplayName("Should match valid tolerant variants of 'tulipe'")
  void shouldMatchValidTulipeVariants(String candidate) {
    assertThat(TextNormalizer.matches(candidate, "tulipe")).isTrue();
  }

  @Test
  @DisplayName("Should not match distinct words or collapse repeated digits in numbers")
  void shouldNotMatchDifferentWords() {
    assertThat(TextNormalizer.matches("rose", "tulipe")).isFalse();
    assertThat(TextNormalizer.matches("tournesol", "tulipe")).isFalse();
    assertThat(TextNormalizer.matches("tuile", "tulipe")).isFalse();
    assertThat(TextNormalizer.matches("10", "100")).isFalse();
    assertThat(TextNormalizer.matches("20", "2000")).isFalse();
    assertThat(TextNormalizer.matches("198", "1998")).isFalse();
    assertThat(TextNormalizer.matches("2000", "2000")).isTrue();
    assertThat(TextNormalizer.matches("cœur", "coeur")).isTrue();
    assertThat(TextNormalizer.matches("Sœur", "soeur")).isTrue();
  }
}
