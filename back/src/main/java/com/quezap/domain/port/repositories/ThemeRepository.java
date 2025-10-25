package com.quezap.domain.port.repositories;

import com.quezap.domain.models.entities.Theme;
import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.models.valueobjects.ThemeName;
import com.quezap.lib.ddd.Repository;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

import org.jspecify.annotations.Nullable;

public interface ThemeRepository extends Repository<Theme> {
  @Nullable Theme findByName(ThemeName name);

  PageOf<Theme> paginate(Pagination pagination);

  PageOf<Theme> paginateSearching(Pagination pagination, SearchQuery search);
}
