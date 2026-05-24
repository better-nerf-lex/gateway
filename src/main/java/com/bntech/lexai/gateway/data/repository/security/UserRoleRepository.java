package com.bntech.lexai.gateway.data.repository.security;

import com.bntech.lexai.gateway.data.entity.security.UserRole;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface UserRoleRepository extends R2dbcRepository<UserRole, Long> {
    Flux<UserRole> findByUserId(Long userId);
}
