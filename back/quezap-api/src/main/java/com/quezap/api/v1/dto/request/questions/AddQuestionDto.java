package com.quezap.api.v1.dto.request.questions;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;

import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.jspecify.annotations.Nullable;

public sealed interface AddQuestionDto {

  public record AffirmationDto(
      String question, boolean isTrue, @Nullable MultipartFile picture, @Nonnull ThemeId themeId)
      implements AddQuestionDto {}

  public record BinaryDto(
      String question,
      @Nonnull @NotEmpty @Valid List<AnswerDto> answers,
      @Nullable MultipartFile picture,
      @Nonnull ThemeId themeId) {}

  public record QuizzDto(
      String question,
      @Nonnull @NotEmpty @Valid List<AnswerDto> answers,
      @Nullable MultipartFile picture,
      @Nonnull ThemeId themeId) {}

  public record AnswerDto(String answer, @Nullable MultipartFile picture, boolean isTrue)
      implements AddQuestionDto {}
}
