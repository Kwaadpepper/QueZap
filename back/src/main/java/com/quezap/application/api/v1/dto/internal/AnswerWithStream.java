package com.quezap.application.api.v1.dto.internal;

import java.io.InputStream;

import com.quezap.application.api.v1.dto.request.questions.NewBinaryDto.AnswerDto;

import org.jspecify.annotations.Nullable;

public record AnswerWithStream(AnswerDto dto, @Nullable InputStream stream) {}
