package com.quezap.api.v1.dto.events;

public record AnswerCountPayload(int slideIndex, int answeredCount, int totalCount) {}
