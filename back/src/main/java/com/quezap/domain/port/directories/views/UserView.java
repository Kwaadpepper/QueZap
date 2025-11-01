package com.quezap.domain.port.directories.views;

import com.quezap.domain.models.valueobjects.identifiers.UserId;
import com.quezap.lib.ddd.valueobjects.TimelinePoint;

public record UserView(UserId id, String name, TimelinePoint createdAt, TimelinePoint updatedAt) {}
