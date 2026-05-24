package com.bntech.lexai.gateway.data.repository.security;

import com.bntech.lexai.gateway.data.entity.security.AuthorizationCode;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AuthorizationCodeRepository extends ReactiveCrudRepository<AuthorizationCode, String> {
    Mono<AuthorizationCode> findByCodeAndUsedFalse(String code);
}
