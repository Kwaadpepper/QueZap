package com.quezap.lib.pagination;

import com.quezap.lib.utils.Domain;

public record PageRequest(Long pageNumber, Long pageSize) {
  public PageRequest {
    Domain.checkDomain(() -> pageNumber > 0, "Page number must be greater than 0");
    Domain.checkDomain(() -> pageSize > 0, "Page size must be greater than 0");
    Domain.checkDomain(() -> pageSize <= 50, "Page size must be less than or equal to 50");
  }
}
