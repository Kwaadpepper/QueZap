package com.quezap.api.v1.dto.response.sessions;

import java.util.List;

public record QuestionReportDto(
    int questionOrder,
    String questionLabel,
    String questionType,
    int totalAnswers,
    int correctAnswers,
    double successRate,
    List<QuestionAnswerStatDto> answers) {}
