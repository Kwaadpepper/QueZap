package com.quezap.application.ports.questions;

import java.util.Set;

import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.usecases.UsecaseHandler;
import com.quezap.lib.ddd.usecases.UsecaseInput;
import com.quezap.lib.ddd.usecases.UsecaseOutput;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.Nullable;

public interface ListQuestions {
  sealed interface Input extends UsecaseInput {
    record PerPage(Pagination page, @Nullable UserId requesterId) implements Input {
      public PerPage(Pagination page) {
        this(page, null);
      }
    }

    record WithThemes(Pagination page, Set<ThemeId> themes, @Nullable UserId requesterId)
        implements Input {
      public WithThemes(Pagination page, Set<ThemeId> themes) {
        this(page, themes, null);
      }
    }

    record Searching(Pagination page, SearchQuery search, @Nullable UserId requesterId)
        implements Input {
      public Searching(Pagination page, SearchQuery search) {
        this(page, search, null);
      }
    }

    record SearchingWithThemes(
        Pagination page, SearchQuery search, Set<ThemeId> themes, @Nullable UserId requesterId)
        implements Input {
      public SearchingWithThemes(Pagination page, SearchQuery search, Set<ThemeId> themes) {
        this(page, search, themes, null);
      }
    }

    public static Input perPage(Pagination page) {
      return new PerPage(page);
    }

    public static Input perPage(Pagination page, @Nullable UserId requesterId) {
      return new PerPage(page, requesterId);
    }

    public static Input withThemes(Pagination page, Set<ThemeId> themes) {
      return new WithThemes(page, themes);
    }

    public static Input withThemes(
        Pagination page, Set<ThemeId> themes, @Nullable UserId requesterId) {
      return new WithThemes(page, themes, requesterId);
    }

    public static Input searching(Pagination page, SearchQuery search) {
      return new Searching(page, search);
    }

    public static Input searching(
        Pagination page, SearchQuery search, @Nullable UserId requesterId) {
      return new Searching(page, search, requesterId);
    }

    public static Input searchingWithThemes(
        Pagination page, SearchQuery search, Set<ThemeId> themes) {
      return new SearchingWithThemes(page, search, themes);
    }

    public static Input searchingWithThemes(
        Pagination page, SearchQuery search, Set<ThemeId> themes, @Nullable UserId requesterId) {
      return new SearchingWithThemes(page, search, themes, requesterId);
    }
  }

  record Output(PageOf<QuestionDto> value) implements UsecaseOutput {
    public record QuestionDto(QuestionId id, String question, TimelinePoint createdAt) {}
  }

  public interface ListQuestionsUsecase
      extends UsecaseHandler<ListQuestions.Input, ListQuestions.Output> {}
}
