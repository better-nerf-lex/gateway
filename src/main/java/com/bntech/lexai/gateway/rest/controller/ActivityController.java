package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.ActivityDto;
import com.bntech.lexai.gateway.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @PreAuthorize("@access.teamAdmin(authentication, #teamId)")
    public Flux<ActivityDto> getActivities(@RequestParam String teamId) {
        return activityService.getActivities(teamId);
    }
}
