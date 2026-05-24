package com.bntech.lexai.gateway.rest.controller.security;

import com.bntech.lexai.gateway.rest.dto.team.CreateTeamRequest;
import com.bntech.lexai.gateway.rest.dto.team.InvitationDto;
import com.bntech.lexai.gateway.rest.dto.team.InviteMemberRequest;
import com.bntech.lexai.gateway.rest.dto.team.TeamDto;
import com.bntech.lexai.gateway.rest.dto.team.TeamMemberDto;
import com.bntech.lexai.gateway.security.access.AccessLevel;
import com.bntech.lexai.gateway.service.security.InvitationService;
import com.bntech.lexai.gateway.service.security.TeamService;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final InvitationService invitationService;

    @GetMapping
    public Flux<TeamDto> getMyTeams() {
        return teamService.getMyTeams();
    }

    @PostMapping
    public Mono<TeamDto> createTeam(@RequestBody CreateTeamRequest request) {
        return teamService.createTeam(request);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("@access.teamRead(authentication, #id)")
    public Flux<TeamMemberDto> getTeamMembers(@PathVariable java.util.UUID id) {
        return teamService.getTeamMembers(id);
    }

    @PutMapping("/{id}/members/{userId}")
    @PreAuthorize("@access.teamAdmin(authentication, #id)")
    public Mono<Void> updateMemberRole(@PathVariable java.util.UUID id, @PathVariable Long userId,
            @RequestBody AccessLevel newLevel) {
        return teamService.updateMemberRole(id, userId, newLevel);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("@access.teamAdmin(authentication, #id)")
    public Mono<Void> removeMember(@PathVariable java.util.UUID id, @PathVariable Long userId) {
        return teamService.removeMember(id, userId);
    }

    @PostMapping("/{id}/invitations")
    @PreAuthorize("@access.teamAdmin(authentication, #id)")
    public Mono<InvitationDto> inviteMember(@PathVariable java.util.UUID id,
            @RequestBody InviteMemberRequest request) {
        return invitationService.inviteMember(id, request.getEmail(), request.getAccessLevel())
                .map(invitation -> InvitationDto.builder()
                        .id(invitation.getId())
                        .teamId(invitation.getTeamId())
                        .email(invitation.getEmail())
                        .accessLevel(invitation.getAccessLevel())
                        .token(invitation.getToken())
                        .status(invitation.getStatus())
                        .invitedBy(invitation.getInvitedBy())
                        .createdAt(invitation.getCreatedAt())
                        .expiresAt(invitation.getExpiresAt())
                        .build());
    }

    @GetMapping("/{id}/invitations")
    @PreAuthorize("@access.teamAdmin(authentication, #id)")
    public Flux<InvitationDto> getTeamInvitations(@PathVariable java.util.UUID id) {
        return invitationService.getTeamInvitations(id);
    }

    @GetMapping("/invitations/my")
    public Flux<InvitationDto> getMyInvitations() {
        return invitationService.getMyInvitations();
    }

    @PostMapping("/invitations/{token}/accept")
    public Mono<TeamMemberDto> acceptInvitation(@PathVariable String token) {
        return invitationService.acceptInvitation(token)
                .flatMap(member -> teamService.getTeamMembers(member.getTeamId())
                        .filter(dto -> dto.getUserId().equals(member.getUserId()))
                        .next());
    }

    @PostMapping("/invitations/{token}/reject")
    public Mono<Void> rejectInvitation(@PathVariable String token) {
        return invitationService.rejectInvitation(token);
    }

    @DeleteMapping("/invitations/{invitationId}")
    @PreAuthorize("@access.teamAdmin(authentication, #invitationId)")
    public Mono<Void> cancelInvitation(@PathVariable java.util.UUID invitationId) {
        return invitationService.cancelInvitation(invitationId);
    }
}
