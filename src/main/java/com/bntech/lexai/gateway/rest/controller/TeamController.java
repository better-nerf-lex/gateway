package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.team.CreateTeamRequest;
import com.bntech.lexai.gateway.rest.dto.team.TeamDto;
import com.bntech.lexai.gateway.service.TeamService;
import com.bntech.lexai.gateway.security.access.TeamAccess;
import static com.bntech.lexai.gateway.security.access.AccessLevel.READ;
import static com.bntech.lexai.gateway.security.access.AccessLevel.ADMIN;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
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
    @TeamAccess(level = READ)
    public Flux<com.bntech.lexai.gateway.rest.dto.team.TeamMemberDto> getTeamMembers(@PathVariable String id) {
        return teamService.getTeamMembers(id);
    }

    @PutMapping("/{id}/members/{userId}")
    @TeamAccess(level = ADMIN)
    public Mono<Void> updateMemberRole(@PathVariable String id, @PathVariable Long userId,
            @RequestBody com.bntech.lexai.gateway.security.access.AccessLevel newLevel) {
        return teamService.updateMemberRole(id, userId, newLevel);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @TeamAccess(level = ADMIN)
    public Mono<Void> removeMember(@PathVariable String id, @PathVariable Long userId) {
        return teamService.removeMember(id, userId);
    }
}
