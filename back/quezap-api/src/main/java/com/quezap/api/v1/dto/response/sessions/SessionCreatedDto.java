package com.quezap.api.v1.dto.response.sessions;

import java.util.UUID;

public record SessionCreatedDto(UUID id, Long number, String code) {}
