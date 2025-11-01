package com.quezap.interfaces.api.v1.dto.internal;

import java.io.InputStream;

import com.quezap.interfaces.api.v1.dto.request.questions.AddQuestionDto.AnswerDto;

import org.jspecify.annotations.Nullable;

public record AnswerWithStream(AnswerDto dto, @Nullable InputStream stream) {}
