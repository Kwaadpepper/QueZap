package com.quezap.api.v1.dto.response.auth;

import com.quezap.api.v1.dto.response.users.UserSummaryDto;

public record LoginResponseDto(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserSummaryDto user) {}
