package com.bntech.lexai.gateway.service.security;

import com.bntech.lexai.gateway.data.entity.security.Team;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.security.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.security.TeamRepository;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.rest.dto.team.CreateTeamRequest;
import com.bntech.lexai.gateway.rest.dto.team.TeamDto;
import com.bntech.lexai.gateway.rest.dto.team.TeamMemberDto;
import com.bntech.lexai.gateway.security.access.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

        private final TeamRepository teamRepository;
        private final TeamMemberRepository teamMemberRepository;
        private final UserRepository userRepository;
        private final CognitoUserSyncService cognitoUserSyncService;

        public Flux<TeamDto> getMyTeams() {
                return getCurrentUser()
                                .flatMapMany(user -> teamMemberRepository.findByUserId(user.getId())
                                                .flatMap(member -> teamRepository.findById(member.getTeamId()))
                                                .map(this::toDto));
        }

        public Mono<TeamDto> createTeam(CreateTeamRequest request) {
                return getCurrentUser()
                                .flatMap(user -> {
                                        Team team = new Team();
                                        team.setId(UUID.randomUUID());
                                        team.setName(request.getName());
                                        team.setCreatedAt(LocalDateTime.now());
                                        team.setNew(true);

                                        return teamRepository.save(team)
                                                        .flatMap(savedTeam -> {
                                                                TeamMember member = new TeamMember();
                                                                member.setId(UUID.randomUUID());
                                                                member.setUserId(user.getId());
                                                                member.setTeamId(savedTeam.getId());
                                                                member.setAccessLevel(AccessLevel.ADMIN);
                                                                return teamMemberRepository.save(member)
                                                                                .thenReturn(toDto(savedTeam));
                                                        });
                                });
        }

        public Mono<TeamDto> ensureDefaultTeam(User user) {
                return teamMemberRepository.findByUserId(user.getId())
                                .next()
                                .flatMap(member -> teamRepository.findById(member.getTeamId()).map(this::toDto))
                                .switchIfEmpty(Mono.defer(() -> {
                                        log.info("Creating default team for user: {}", user.getEmail());
                                        Team team = new Team();
                                        team.setId(UUID.randomUUID());
                                        team.setName(user.getEmail() + "'s Team");
                                        team.setCreatedAt(LocalDateTime.now());
                                        team.setNew(true);

                                        return teamRepository.save(team)
                                                        .flatMap(savedTeam -> {
                                                                TeamMember member = new TeamMember();
                                                                member.setId(UUID.randomUUID());
                                                                member.setUserId(user.getId());
                                                                member.setTeamId(savedTeam.getId());
                                                                member.setAccessLevel(AccessLevel.ADMIN);
                                                                return teamMemberRepository.save(member)
                                                                                .thenReturn(toDto(savedTeam));
                                                        });
                                }));
        }

        public Flux<TeamMemberDto> getTeamMembers(UUID teamId) {
                return teamMemberRepository.findByTeamId(teamId)
                                .flatMap(member -> userRepository.findById(member.getUserId())
                                                .map(user -> TeamMemberDto.builder()
                                                                .userId(user.getId())
                                                                .username(user.getUsername())
                                                                .email(user.getEmail())
                                                                .teamId(member.getTeamId())
                                                                .accessLevel(member.getAccessLevel())
                                                                .joinedAt(member.getCreatedAt())
                                                                .build()));
        }

        public Mono<Void> updateMemberRole(UUID teamId, Long userId, AccessLevel newLevel) {
                return teamMemberRepository.findByUserIdAndTeamId(userId, teamId)
                                .flatMap(member -> {
                                        member.setAccessLevel(newLevel);
                                        member.setNotNew();
                                        return teamMemberRepository.save(member);
                                })
                                .then();
        }

        public Mono<Void> removeMember(UUID teamId, Long userId) {
                return teamMemberRepository.findByUserIdAndTeamId(userId, teamId)
                                .flatMap(teamMemberRepository::delete);
        }

        private TeamDto toDto(Team team) {
                return TeamDto.builder()
                                .id(team.getId())
                                .name(team.getName())
                                .createdAt(team.getCreatedAt())
                                .build();
        }

        public Mono<Void> deleteTeam(String id) {
                return teamRepository.softDelete(UUID.fromString(id))
                                .doOnSuccess(v -> log.info("✅ Team soft deleted: {}", id));
        }

        public Flux<TeamDto> getDeletedTeams() {
                return teamRepository.findAllDeleted()
                                .map(this::toDto);
        }

        public Mono<TeamDto> restoreTeam(String id) {
                UUID uuid = UUID.fromString(id);
                return teamRepository.restore(uuid)
                                .then(teamRepository.findById(uuid))
                                .map(restored -> {
                                        restored.setNotNew();
                                        return toDto(restored);
                                })
                                .doOnSuccess(result -> log.info("✅ Team restored: {}", id));
        }

        public Mono<Void> permanentlyDeleteTeam(String id) {
                return teamRepository.deleteById(UUID.fromString(id))
                                .doOnSuccess(v -> log.info("✅ Team permanently deleted: {}", id));
        }

        private Mono<User> getCurrentUser() {
                return cognitoUserSyncService.getCurrentUser()
                                .onErrorResume(e -> ReactiveSecurityContextHolder.getContext()
                                                .map(SecurityContext::getAuthentication)
                                                .flatMap(auth -> userRepository.findByUsername(auth.getName())));
        }
}
