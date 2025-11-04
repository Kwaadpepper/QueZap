package com.quezap.infrastructure.adapter.spi;

import java.util.List;

public interface DataSource<T> {
  List<T> getAll();
}
