package com.quezap.domain.port.repositories;

import java.util.Set;

import com.quezap.domain.models.entities.Question;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.lib.ddd.Repository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public interface QuestionRepository extends Repository<Question> {
  PageOf<Question> paginate(Pagination pagination);

  PageOf<Question> paginateWithThemes(Pagination pagination, Set<ThemeId> themes);

  PageOf<Question> paginateSearching(Pagination pagination, SearchQuery search);

  PageOf<Question> paginateSearchingWithThemes(
      Pagination pagination, SearchQuery search, Set<ThemeId> themes);
}
