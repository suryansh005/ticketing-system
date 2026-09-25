package com.tothenew.ticket.api.dto;

import com.tothenew.ticket.infrastructure.persistence.UserEntity;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email) {

    public static UserResponse from(UserEntity entity) {
        return new UserResponse(entity.getId(), entity.getName(), entity.getEmail());
    }
}
