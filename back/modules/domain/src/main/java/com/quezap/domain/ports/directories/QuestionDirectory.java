package com.quezap.domain.ports.directories;

import java.util.Set;

import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.identifiers.ThemeId;
import com.quezap.domain.ports.directories.views.QuestionView;
import com.quezap.lib.ddd.directories.Directory;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public interface QuestionDirectory extends Directory<QuestionView> {

  PageOf<QuestionView> paginateWithThemes(Pagination page, Set<ThemeId> themes);

  PageOf<QuestionView> paginateSearching(Pagination page, SearchQuery search);

  PageOf<QuestionView> paginateSearchingWithThemes(
      Pagination page, SearchQuery search, Set<ThemeId> themes);
}
