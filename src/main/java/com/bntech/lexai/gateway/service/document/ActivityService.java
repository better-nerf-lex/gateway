package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.Activity;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.object.ActionType;
import com.bntech.lexai.gateway.data.repository.document.ActivityRepository;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.mapper.document.ActivityMapper;
import com.bntech.lexai.gateway.rest.dto.ActivityDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ActivityMapper activityMapper;

    public void logActivity(Long userId, UUID teamId, ActionType action, UUID entityId, String entityType,
            String details) {
        if (teamId == null) {
            log.warn("Attempting to log activity without teamId. Action: {}, Entity: {}", action, entityId);
            return;
        }

        Activity activity = Activity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .teamId(teamId)
                .action(action)
                .entityId(entityId)
                .entityType(entityType)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();

        activityRepository.save(activity)
                .subscribe(
                        saved -> log.debug("Logged activity: {}", saved.getId()),
                        error -> log.error("Error logging activity", error));
    }

    public Flux<ActivityDto> getActivities(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return Flux.empty();
        }

        return activityRepository.findByTeamIdOrderByCreatedAtDesc(UUID.fromString(teamId))
                .flatMap(this::toDto);
    }

    private Mono<ActivityDto> toDto(Activity activity) {
        Mono<String> usernameMono = activity.getUserId() != null
                ? userRepository.findById(activity.getUserId())
                        .map(User::getUsername)
                        .defaultIfEmpty("Unknown")
                : Mono.just("System");

        return usernameMono.map(username -> {
            ActivityDto dto = activityMapper.toDto(activity);
            dto.setUsername(username);
            return dto;
        });
    }
}
