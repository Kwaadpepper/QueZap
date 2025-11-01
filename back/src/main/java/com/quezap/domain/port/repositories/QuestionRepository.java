package com.quezap.domain.port.repositories;

import java.util.Set;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.identifiers.QuestionId;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.repositories.Repository;

public interface QuestionRepository extends Repository<Question, QuestionId> {
  long countWithThemes(Set<ThemeId> themeIds);
}
