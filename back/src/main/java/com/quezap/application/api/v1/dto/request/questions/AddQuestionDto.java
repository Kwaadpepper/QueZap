package com.quezap.application.api.v1.dto.request.questions;

import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import com.quezap.domain.models.valueobjects.identifiers.ThemeId;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public sealed interface AddQuestionDto {

  public record AffirmationDto(
      @NotBlank @Size(max = 255) String question,
      boolean isTrue,
      @Nullable MultipartFile picture,
      @Nonnull ThemeId themeId)
      implements AddQuestionDto {}

  public record BinaryDto(
      @NotBlank @Size(max = 255) String question,
      @Nonnull Set<AnswerDto> answers,
      @Nullable MultipartFile picture,
      @Nonnull ThemeId themeId) {}

  public record QuizzDto(
      @NotBlank @Size(max = 255) String question,
      @Nonnull Set<AnswerDto> answers,
      @Nullable MultipartFile picture,
      @Nonnull ThemeId themeId) {}

  public record AnswerDto(
      @NotBlank @Size(max = 255) String answerText, @Nullable MultipartFile picture, boolean isTrue)
      implements AddQuestionDto {}
}
