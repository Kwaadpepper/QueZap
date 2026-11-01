package com.quezap.domain.services;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class TextNormalizer {

  private static final Pattern DIACRITICS_PATTERN =
      Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");
  private static final Pattern CONSECUTIVE_DUPLICATES_PATTERN = Pattern.compile("([a-z])\\1+");

  private TextNormalizer() {}

  /**
   * Normalise un texte selon les règles de tolérance canonique : 1. Trim et passage en minuscules.
   * 2. Translittération des ligatures (œ -> oe, æ -> ae) et décomposition Unicode NFD avec
   * suppression des signes diacritiques (accents, cédilles, trémas). 3. Suppression des emojis,
   * ponctuations et caractères non-alphanumériques. 4. Réduction des répétitions consécutives de
   * lettres (ex: "tuliipe" -> "tulipe", "tullipe" -> "tulipe") sans altérer les nombres.
   */
  public static String normalize(@Nullable String input) {
    if (input == null) {
      return "";
    }
    var text =
        input
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("œ", "oe")
            .replace("æ", "ae")
            .replace("ß", "ss");
    if (text.isEmpty()) {
      return "";
    }

    // 1. Décomposition Unicode et suppression des accents
    final var decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
    final var withoutDiacritics = DIACRITICS_PATTERN.matcher(decomposed).replaceAll("");

    // 2. Suppression de toute ponctuation, emoji et caractères non alphanumériques
    final var alphanumericOnly = NON_ALPHANUMERIC_PATTERN.matcher(withoutDiacritics).replaceAll("");

    // 3. Réduction des répétitions consécutives de lettres (2 ou plus ramenées à 1 seule lettre)
    return CONSECUTIVE_DUPLICATES_PATTERN.matcher(alphanumericOnly).replaceAll("$1");
  }

  /** Compare deux textes de façon tolérante en appliquant la normalisation symétrique. */
  public static boolean matches(@Nullable String candidate, @Nullable String expected) {
    final var normCandidate = normalize(candidate);
    final var normExpected = normalize(expected);
    return !normCandidate.isEmpty() && normCandidate.equals(normExpected);
  }
}
