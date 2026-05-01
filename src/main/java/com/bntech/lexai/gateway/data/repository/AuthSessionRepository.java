package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.security.AuthSession;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthSessionRepository extends ReactiveCrudRepository<AuthSession, String> {
}
