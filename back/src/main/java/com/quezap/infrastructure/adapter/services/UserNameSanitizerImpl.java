package com.quezap.infrastructure.adapter.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.quezap.application.config.ProfanityConfig;
import com.quezap.domain.ports.services.UserNameSanitizer;

import com.modernmt.text.profanity.ProfanityFilter;
import com.modernmt.text.profanity.dictionary.Dictionary;
import com.modernmt.text.profanity.dictionary.Profanity;
import org.jspecify.annotations.Nullable;

@Service
public class UserNameSanitizerImpl implements UserNameSanitizer {
  private final ProfanityFilter profanityFilter;
  private final Dictionary customDictionary;
  private final Dictionary frDictionary;
  private final Dictionary enDictionary;

  public UserNameSanitizerImpl(ProfanityConfig profanityConfig) {
    final var customWords = profanityConfig.getForbiddenWords();

    this.profanityFilter = new ProfanityFilter();
    this.frDictionary = loadDictionary("fr");
    this.enDictionary = loadDictionary("en");
    this.customDictionary =
        new Dictionary(
            "custom",
            customWords.stream().map(w -> new Profanity(w, 0)).collect(Collectors.toSet()));
  }

  @Override
  public String sanitize(String userName) {
    var normalized = normalizeWhitespace(userName).toLowerCase(Locale.ROOT);

    @Nullable String profanity;

    do {
      profanity = findProfanity(normalized);
      if (profanity != null) {
        normalized = stripWordFrom(normalized, profanity);
      }
    } while (profanity != null);

    return titleCase(normalized);
  }

  private @Nullable String findProfanity(String input) {
    final @Nullable Profanity frProfanity = profanityFilter.find("fr", input);
    final @Nullable Profanity enProfanity = profanityFilter.find("en", input);
    final @Nullable Profanity customProfanity = customDictionary.matcher(0).find(input);

    if (frProfanity != null) {
      return frProfanity.text();
    } else if (enProfanity != null) {
      return enProfanity.text();
    } else if (customProfanity != null) {
      return customProfanity.text();
    } else {
      return findAnyProfanityWords(input);
    }
  }

  private @Nullable String findAnyProfanityWords(String input) {
    final var frIterator = frDictionary.iterator();
    final var enIterator = enDictionary.iterator();
    final var customIterator = customDictionary.iterator();

    while (frIterator.hasNext()) {
      final var p = frIterator.next();
      if (input.contains(p.text().toLowerCase(Locale.ROOT))) {
        return p.text();
      }
    }

    while (enIterator.hasNext()) {
      final var p = enIterator.next();
      if (input.contains(p.text().toLowerCase(Locale.ROOT))) {
        return p.text();
      }
    }

    while (customIterator.hasNext()) {
      final var p = customIterator.next();
      if (input.contains(p.text().toLowerCase(Locale.ROOT))) {
        return p.text();
      }
    }

    return null;
  }

  @SuppressWarnings("null")
  private String titleCase(String input) {
    if (input.isEmpty()) {
      return input;
    }
    return input
        .chars()
        .mapToObj(c -> (char) c)
        .reduce(
            new StringBuilder(),
            (sb, c) -> {
              if (sb.isEmpty() || sb.charAt(sb.length() - 1) == ' ') {
                sb.append(Character.toUpperCase(c));
              } else {
                sb.append(Character.toLowerCase(c));
              }
              return sb;
            },
            StringBuilder::append)
        .toString();
  }

  private String normalizeWhitespace(String input) {
    return input.trim().replaceAll("\\s+", " ");
  }

  private String stripWordFrom(String input, String badWord) {
    final var escapedWord = Pattern.quote(badWord.toLowerCase(Locale.ROOT));
    final var result = input.replaceAll(escapedWord, "");

    return normalizeWhitespace(result);
  }

  /** Load words from dependency. */
  private Dictionary loadDictionary(String language) {
    String resource = "dictionary." + language;

    InputStream stream = ProfanityFilter.class.getResourceAsStream(resource);
    if (stream == null) {
      throw new DictLoadingException("Internal resource not found: " + resource);
    }

    try {
      return Dictionary.read(language, stream);
    } catch (IOException _) {
      throw new DictLoadingException("Unable to load internal resource: " + resource);
    } finally {
      try {
        stream.close();
      } catch (IOException _) {
        // ignore
      }
    }
  }

  private static class DictLoadingException extends RuntimeException {
    DictLoadingException(String message) {
      super(message);
    }
  }
}
