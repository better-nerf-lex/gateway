package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.data.repository.UserRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Service
public class DatabaseUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public DatabaseUserDetailsService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .flatMap(user -> userRoleRepository.findByUserId(user.getId())
                        .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole()))
                        .collect(Collectors.toList())
                        .map(authorities -> org.springframework.security.core.userdetails.User
                                .withUsername(user.getUsername())
                                .password(user.getPassword())
                                .authorities(authorities)
                                .disabled(!user.isEnabled())
                                .accountExpired(false)
                                .credentialsExpired(false)
                                .accountLocked(false)
                                .build()
                        )
                );
    }
}
