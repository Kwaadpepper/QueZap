package com.quezap.api.v1.dto.response.users;

import java.time.Instant;
import java.util.UUID;

public record UserDetailDto(UUID id, String name, Instant createdAt, Instant updatedAt) {}
