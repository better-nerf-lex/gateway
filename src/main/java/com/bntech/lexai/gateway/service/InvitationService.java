package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.TeamInvitation;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.object.InvitationStatus;
import com.bntech.lexai.gateway.data.repository.TeamInvitationRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.exception.BadRequestException;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.rest.dto.team.InvitationDto;
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
public class InvitationService {

    private final TeamInvitationRepository invitationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final CognitoUserSyncService cognitoUserSyncService;

    private static final int INVITATION_EXPIRY_DAYS = 7;

    public Mono<TeamInvitation> inviteMember(UUID teamId, String email, AccessLevel accessLevel) {
        return getCurrentUser()
                .flatMap(inviter -> {
                    String normalizedEmail = email.toLowerCase().trim();

                    return teamMemberRepository.findByUserIdAndTeamId(inviter.getId(), teamId)
                            .switchIfEmpty(Mono.error(new BadRequestException("You are not a member of this team")))
                            .flatMap(membership -> {
                                if (membership.getAccessLevel().ordinal() < AccessLevel.ADMIN.ordinal()) {
                                    return Mono.error(new BadRequestException("Only admins can invite members"));
                                }

                                return userRepository.findByEmail(normalizedEmail)
                                        .flatMap(existingUser -> teamMemberRepository
                                                .findByUserIdAndTeamId(existingUser.getId(), teamId)
                                                .flatMap(existingMember -> Mono.<TeamInvitation>error(
                                                        new BadRequestException("User is already a member of this team")))
                                                .switchIfEmpty(createInvitation(teamId, normalizedEmail, accessLevel, inviter.getId())))
                                        .switchIfEmpty(createInvitation(teamId, normalizedEmail, accessLevel, inviter.getId()));
                            });
                })
                .doOnSuccess(invitation -> log.info("Created invitation for {} to team {}", email, teamId))
                .doOnError(error -> log.error("Failed to create invitation for {} to team {}", email, teamId, error));
    }

    private Mono<TeamInvitation> createInvitation(UUID teamId, String email, AccessLevel accessLevel, Long inviterId) {
        return invitationRepository.findByEmailAndTeamIdAndStatus(email, teamId, InvitationStatus.PENDING)
                .flatMap(existingInvitation -> {
                    if (existingInvitation.getExpiresAt().isAfter(LocalDateTime.now())) {
                        return Mono.error(new BadRequestException("Active invitation already exists for this email"));
                    }
                    existingInvitation.setStatus(InvitationStatus.EXPIRED);
                    existingInvitation.setNotNew();
                    return invitationRepository.save(existingInvitation)
                            .then(saveNewInvitation(teamId, email, accessLevel, inviterId));
                })
                .switchIfEmpty(saveNewInvitation(teamId, email, accessLevel, inviterId));
    }

    private Mono<TeamInvitation> saveNewInvitation(UUID teamId, String email, AccessLevel accessLevel, Long inviterId) {
        TeamInvitation invitation = TeamInvitation.builder()
                .teamId(teamId)
                .email(email)
                .accessLevel(accessLevel)
                .token(generateToken())
                .status(InvitationStatus.PENDING)
                .invitedBy(inviterId)
                .expiresAt(LocalDateTime.now().plusDays(INVITATION_EXPIRY_DAYS))
                .isNew(true)
                .build();
        invitation.setId(UUID.randomUUID());

        return invitationRepository.save(invitation);
    }

    public Mono<TeamMember> acceptInvitation(String token) {
        return getCurrentUser()
                .flatMap(user -> invitationRepository.findByToken(token)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Invitation", token)))
                        .flatMap(invitation -> {
                            if (invitation.getStatus() != InvitationStatus.PENDING) {
                                return Mono.error(new BadRequestException("Invitation is no longer valid"));
                            }

                            if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
                                invitation.setStatus(InvitationStatus.EXPIRED);
                                invitation.setNotNew();
                                return invitationRepository.save(invitation)
                                        .then(Mono.error(new BadRequestException("Invitation has expired")));
                            }

                            if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
                                return Mono.error(new BadRequestException("This invitation is for a different email address"));
                            }

                            return teamMemberRepository.findByUserIdAndTeamId(user.getId(), invitation.getTeamId())
                                    .flatMap(existingMember -> Mono.<TeamMember>error(
                                            new BadRequestException("You are already a member of this team")))
                                    .switchIfEmpty(createTeamMember(user.getId(), invitation));
                        }))
                .doOnSuccess(member -> log.info("User {} accepted invitation and joined team {}", member.getUserId(), member.getTeamId()))
                .doOnError(error -> log.error("Failed to accept invitation with token {}", token, error));
    }

    private Mono<TeamMember> createTeamMember(Long userId, TeamInvitation invitation) {
        TeamMember member = TeamMember.builder()
                .userId(userId)
                .teamId(invitation.getTeamId())
                .accessLevel(invitation.getAccessLevel())
                .isNew(true)
                .build();
        member.setId(UUID.randomUUID());

        return teamMemberRepository.save(member)
                .flatMap(savedMember -> {
                    invitation.setStatus(InvitationStatus.ACCEPTED);
                    invitation.setNotNew();
                    return invitationRepository.save(invitation)
                            .thenReturn(savedMember);
                });
    }

    public Mono<Void> rejectInvitation(String token) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Invitation", token)))
                .flatMap(invitation -> {
                    if (invitation.getStatus() != InvitationStatus.PENDING) {
                        return Mono.error(new BadRequestException("Invitation is no longer valid"));
                    }

                    invitation.setStatus(InvitationStatus.REJECTED);
                    invitation.setNotNew();
                    return invitationRepository.save(invitation);
                })
                .then()
                .doOnSuccess(v -> log.info("Invitation with token {} was rejected", token))
                .doOnError(error -> log.error("Failed to reject invitation with token {}", token, error));
    }

    public Flux<InvitationDto> getTeamInvitations(UUID teamId) {
        return invitationRepository.findByTeamIdAndStatus(teamId, InvitationStatus.PENDING)
                .filter(invitation -> invitation.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toDto);
    }

    public Flux<InvitationDto> getMyInvitations() {
        return getCurrentUser()
                .flatMapMany(user -> invitationRepository.findByEmail(user.getEmail())
                        .filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
                        .filter(invitation -> invitation.getExpiresAt().isAfter(LocalDateTime.now()))
                        .map(this::toDto));
    }

    public Mono<Void> cancelInvitation(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Invitation", invitationId.toString())))
                .flatMap(invitation -> {
                    if (invitation.getStatus() != InvitationStatus.PENDING) {
                        return Mono.error(new BadRequestException("Invitation is no longer pending"));
                    }

                    invitation.setStatus(InvitationStatus.REJECTED);
                    invitation.setNotNew();
                    return invitationRepository.save(invitation);
                })
                .then()
                .doOnSuccess(v -> log.info("Invitation {} was cancelled", invitationId))
                .doOnError(error -> log.error("Failed to cancel invitation {}", invitationId, error));
    }

    private String generateToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    private InvitationDto toDto(TeamInvitation invitation) {
        return InvitationDto.builder()
                .id(invitation.getId())
                .teamId(invitation.getTeamId())
                .email(invitation.getEmail())
                .accessLevel(invitation.getAccessLevel())
                .token(invitation.getToken())
                .status(invitation.getStatus())
                .invitedBy(invitation.getInvitedBy())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    private Mono<User> getCurrentUser() {
        return cognitoUserSyncService.getCurrentUser()
                .onErrorResume(e -> ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .flatMap(auth -> userRepository.findByUsername(auth.getName())));
    }
}
