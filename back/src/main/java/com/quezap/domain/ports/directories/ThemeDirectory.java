package com.quezap.domain.ports.directories;

import com.quezap.domain.models.valueobjects.SearchQuery;
import com.quezap.domain.ports.directories.views.ThemeView;
import com.quezap.lib.ddd.directories.Directory;
import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public interface ThemeDirectory extends Directory<ThemeView> {
  PageOf<ThemeView> paginateSearching(Pagination pagination, SearchQuery search);
}
