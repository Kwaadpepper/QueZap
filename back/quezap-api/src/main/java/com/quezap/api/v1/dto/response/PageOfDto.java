package com.quezap.api.v1.dto.response;

import java.util.List;

public record PageOfDto<T>(
    List<T> data,

    // Métadonnées
    long totalElements,
    long totalPages,
    long page,
    long pageSize,
    boolean hasNext,
    boolean hasPrevious) {}
