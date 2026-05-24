package com.bntech.lexai.gateway.service.security;

import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.data.repository.security.UserRoleRepository;
import com.bntech.lexai.gateway.mapper.security.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository users;
    private final UserRoleRepository roles;
    private final UserMapper userMapper;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return users.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .flatMap(user -> roles.findByUserId(user.getId())
                        .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole()))
                        .collect(Collectors.toList())
                        .doOnNext(authorities -> log.info("Found authorities for user {}: {}",
                                username, authorities))
                        .map(authorities -> userMapper.toUserPrincipal(user, authorities)));
    }
}
