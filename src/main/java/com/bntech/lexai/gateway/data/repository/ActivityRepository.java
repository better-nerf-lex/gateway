package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.Activity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface ActivityRepository extends R2dbcRepository<Activity, UUID> {
    Flux<Activity> findAllByOrderByCreatedAtDesc();

    Flux<Activity> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
}
