package com.bntech.lexai.gateway.security.access;

import com.bntech.lexai.gateway.data.entity.OwnableEntity;
import com.bntech.lexai.gateway.data.repository.DocumentRepository;
import com.bntech.lexai.gateway.data.repository.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.rest.controller.DocumentController;
import com.bntech.lexai.gateway.rest.controller.LegalEntityController;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.reactivestreams.Publisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AccessAspect {

    private final LegalEntityRepository legalEntityRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Around("@annotation(com.bntech.lexai.gateway.security.access.TeamAccess) || @annotation(com.bntech.lexai.gateway.security.access.UserAccess)")
    public Object checkAccess(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        TeamAccess teamAccess = method.getAnnotation(TeamAccess.class);
        UserAccess userAccess = method.getAnnotation(UserAccess.class);

        Object[] args = joinPoint.getArgs();
        String arg0 = (args.length > 0 && args[0] instanceof String) ? (String) args[0] : null;
        Object target = joinPoint.getTarget();

        Mono<Boolean> teamCheck = Mono.just(false);
        if (teamAccess != null) {
            Mono<String> teamIdMono;
            if (target instanceof LegalEntityController || target instanceof DocumentController) {
                Mono<? extends OwnableEntity> entityMono = null;
                if (target instanceof LegalEntityController && arg0 != null) {
                    entityMono = legalEntityRepository.findById(arg0);
                } else if (target instanceof DocumentController && arg0 != null) {
                    entityMono = documentRepository.findById(arg0);
                }
                if (entityMono != null) {
                    teamIdMono = entityMono
                            .flatMap(entity -> entity.getTeamId() != null ? Mono.just(entity.getTeamId()) : Mono.empty())
                            .switchIfEmpty(Mono.justOrEmpty(arg0));
                } else {
                    teamIdMono = Mono.justOrEmpty(arg0);
                }
            } else {
                teamIdMono = Mono.justOrEmpty(arg0);
            }

            teamCheck = teamIdMono
                    .flatMap(teamId -> ReactiveSecurityContextHolder.getContext()
                            .map(SecurityContext::getAuthentication)
                            .flatMap(auth -> userRepository.findByUsername(auth.getName())
                                    .switchIfEmpty(Mono.error(new AccessDeniedException("User not found")))
                                    .flatMap(user -> {
                                        boolean isAdmin = auth.getAuthorities().stream()
                                                .map(GrantedAuthority::getAuthority)
                                                .anyMatch(a -> a.equals("ROLE_ADMIN"));
                                        if (isAdmin && teamAccess.level() == AccessLevel.ADMIN) {
                                            return Mono.just(true);
                                        }
                                        if (teamId == null || teamId.isBlank()) {
                                            return Mono.just(true);
                                        }
                                        return teamMemberRepository.findByUserIdAndTeamId(user.getId(), teamId)
                                                .map(member -> member.getAccessLevel().ordinal() >= teamAccess.level().ordinal())
                                                .defaultIfEmpty(false);
                                    })))
                    .defaultIfEmpty(true);
        }

        Mono<Boolean> userCheck = Mono.just(false);
        if (userAccess != null) {
            String id = arg0;
            Mono<? extends OwnableEntity> entityMono;
            if (target instanceof LegalEntityController) {
                entityMono = legalEntityRepository.findById(id);
            } else if (target instanceof DocumentController) {
                entityMono = documentRepository.findById(id);
            } else {
                entityMono = Mono.empty();
            }

            userCheck = ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .flatMap(auth -> userRepository.findByUsername(auth.getName())
                            .switchIfEmpty(Mono.error(new AccessDeniedException("User not found")))
                            .flatMap(user -> entityMono
                                    .flatMap(entity -> {
                                        boolean isOwner = String.valueOf(user.getId()).equals(entity.getUserId());
                                        boolean isAdmin = auth.getAuthorities().stream()
                                                .map(GrantedAuthority::getAuthority)
                                                .anyMatch(a -> a.equals("ROLE_ADMIN"));
                                        return Mono.just(isOwner || isAdmin);
                                    })
                                    .defaultIfEmpty(false)));
        }

        Mono<Boolean> grantedMono = Mono.zip(teamCheck.defaultIfEmpty(false), userCheck.defaultIfEmpty(false))
                .map(tuple -> tuple.getT1() || tuple.getT2());

        Class<?> returnType = signature.getReturnType();
        boolean returnsFlux = Flux.class.isAssignableFrom(returnType);
        boolean returnsMono = Mono.class.isAssignableFrom(returnType);

        if (returnsFlux) {
            return Flux.defer(() -> grantedMono.flatMapMany(granted -> {
                if (!granted && (method.isAnnotationPresent(TeamAccess.class) || method.isAnnotationPresent(UserAccess.class))) {
                    return Flux.error(new AccessDeniedException("Access Denied"));
                }
                try {
                    Object proceeded = joinPoint.proceed();
                    if (proceeded instanceof Publisher) {
                        return Flux.from((Publisher<?>) proceeded);
                    }
                    return Flux.error(new IllegalStateException("Advised method did not return a Publisher"));
                } catch (Throwable e) {
                    return Flux.error(e);
                }
            }));
        }

        // Default to Mono for single-result reactive endpoints
        return Mono.defer(() -> grantedMono.flatMap(granted -> {
            if (!granted && (method.isAnnotationPresent(TeamAccess.class) || method.isAnnotationPresent(UserAccess.class))) {
                return Mono.error(new AccessDeniedException("Access Denied"));
            }
            try {
                Object proceeded = joinPoint.proceed();
                if (proceeded instanceof Mono) {
                    return (Mono<?>) proceeded;
                }
                if (proceeded instanceof Flux) {
                    return Mono.from((Flux<?>) proceeded);
                }
                if (proceeded instanceof Publisher) {
                    return Mono.from((Publisher<?>) proceeded);
                }
                return Mono.error(new IllegalStateException("Advised method did not return a reactive type"));
            } catch (Throwable e) {
                return Mono.error(e);
            }
        }));
    }
}
