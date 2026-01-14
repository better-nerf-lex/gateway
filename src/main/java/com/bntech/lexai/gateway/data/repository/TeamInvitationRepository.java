package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.security.TeamInvitation;
import com.bntech.lexai.gateway.data.object.InvitationStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface TeamInvitationRepository extends R2dbcRepository<TeamInvitation, UUID> {

    Mono<TeamInvitation> findByToken(String token);

    Flux<TeamInvitation> findByTeamId(UUID teamId);

    Flux<TeamInvitation> findByEmail(String email);

    Flux<TeamInvitation> findByTeamIdAndStatus(UUID teamId, InvitationStatus status);

    @Query("SELECT * FROM team_invitations WHERE email = :email AND team_id = :teamId AND status = :status")
    Mono<TeamInvitation> findByEmailAndTeamIdAndStatus(String email, UUID teamId, InvitationStatus status);
}
