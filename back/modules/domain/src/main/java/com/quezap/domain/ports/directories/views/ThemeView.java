package com.quezap.domain.ports.directories.views;

import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

public record ThemeView(ThemeId id, ThemeName name, TimelinePoint createdAt) {}
