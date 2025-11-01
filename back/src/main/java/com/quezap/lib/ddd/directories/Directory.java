package com.quezap.lib.ddd.directories;

import com.quezap.lib.pagination.PageOf;
import com.quezap.lib.pagination.Pagination;

public interface Directory<V> {
  PageOf<V> paginate(Pagination pagination);
}
