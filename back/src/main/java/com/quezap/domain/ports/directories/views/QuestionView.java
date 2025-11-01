package com.quezap.domain.ports.directories.views;

import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

public record QuestionView(
    QuestionId id, String question, ThemeId theme, TimelinePoint createdAt) {}
