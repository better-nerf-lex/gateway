package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.security.Team;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends R2dbcRepository<Team, String> {
}
