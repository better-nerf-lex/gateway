package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface TeamMemberRepository extends R2dbcRepository<TeamMember, UUID> {

    @Query("SELECT * FROM team_members WHERE user_id = :userId")
    Flux<TeamMember> findByUserId(Long userId);

    @Query("SELECT * FROM team_members WHERE user_id = :userId AND team_id = :teamId")
    Mono<TeamMember> findByUserIdAndTeamId(Long userId, UUID teamId);

    @Query("SELECT * FROM team_members WHERE team_id = :teamId")
    Flux<TeamMember> findByTeamId(UUID teamId);
}
