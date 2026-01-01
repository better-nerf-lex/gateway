package com.bntech.lexai.gateway.data.entity;

import java.util.UUID;

public interface OwnableEntity {
    Long getUserId();

    UUID getTeamId();
}
