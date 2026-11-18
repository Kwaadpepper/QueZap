package com.quezap.api.v1.dto.response.pictures;

import java.time.Instant;

public record PictureUploadResponseDto(String pictureUrl, String objectKey, Instant expiresAt) {}
