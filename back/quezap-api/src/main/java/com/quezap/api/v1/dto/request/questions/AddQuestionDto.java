package com.quezap.api.v1.dto.request.questions;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public sealed interface AddQuestionDto {

  public record AffirmationDto(
      @NotBlank String question,
      boolean isTrue,
      @Nullable MultipartFile picture,
      @NotNull ThemeId themeId)
      implements AddQuestionDto {}

  public record BinaryDto(
      @NotBlank String question,
      @NotNull @NotEmpty @Valid List<AnswerDto> answers,
      @Nullable MultipartFile picture,
      @NotNull ThemeId themeId)
      implements AddQuestionDto {}

  public record QuizzDto(
      @NotBlank String question,
      @NotNull @NotEmpty @Valid List<AnswerDto> answers,
      @Nullable MultipartFile picture,
      @NotNull ThemeId themeId)
      implements AddQuestionDto {}

  public record AnswerDto(@NotBlank String answer, @Nullable MultipartFile picture, boolean isTrue)
      implements AddQuestionDto {}
}
