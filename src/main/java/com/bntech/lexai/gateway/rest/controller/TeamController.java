package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.team.CreateTeamRequest;
import com.bntech.lexai.gateway.rest.dto.team.TeamDto;
import com.bntech.lexai.gateway.service.TeamService;
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
    public Flux<com.bntech.lexai.gateway.rest.dto.team.TeamMemberDto> getTeamMembers(@PathVariable java.util.UUID id) {
        return teamService.getTeamMembers(id);
    }

    @PutMapping("/{id}/members/{userId}")
    @PreAuthorize("@access.teamAdmin(authentication, #id)")
    public Mono<Void> updateMemberRole(@PathVariable java.util.UUID id, @PathVariable Long userId,
            @RequestBody com.bntech.lexai.gateway.security.access.AccessLevel newLevel) {
        return teamService.updateMemberRole(id, userId, newLevel);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("@access.teamAdmin(authentication, #id)")
    public Mono<Void> removeMember(@PathVariable java.util.UUID id, @PathVariable Long userId) {
        return teamService.removeMember(id, userId);
    }
}
