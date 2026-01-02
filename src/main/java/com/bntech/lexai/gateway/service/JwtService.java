package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret:mySecretKey}")
    private String secret;

    @Value("${jwt.expiration:3600}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("type", "access");
        return createToken(claims, user.getUsername(), expiration);
    }

    public String generateTokenFromOAuth2User(OAuth2User oauth2User, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", oauth2User.getAttribute("email"));
        claims.put("name", oauth2User.getAttribute("name"));
        claims.put("provider", "google");
        claims.put("type", "access");
        return createToken(claims, username, expiration);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "refresh");
        return createToken(claims, user.getUsername(), refreshExpiration);
    }

    public String generateRefreshTokenFromOAuth2User(OAuth2User oauth2User, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", oauth2User.getAttribute("email"));
        claims.put("provider", "google");
        claims.put("type", "refresh");
        return createToken(claims, username, refreshExpiration);
    }

    public String generateAuthorizationCode() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "authorization_code");
        return createToken(claims, "auth_code", 600L); // 10 minutes
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Mono<String> validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (claims.getExpiration().before(new Date())) {
                return Mono.error(new RuntimeException("Token expired"));
            }

            return Mono.just(claims.getSubject());
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Invalid token"));
        }
    }

    public Mono<String> validateRefreshToken(String token) {
        return validateAndGetClaims(token)
                .filter(claims -> "refresh".equals(claims.get("type")))
                .map(Claims::getSubject)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid refresh token type")));
    }

    public Mono<Claims> validateAndGetClaims(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (claims.getExpiration().before(new Date())) {
                return Mono.error(new RuntimeException("Token expired"));
            }

            return Mono.just(claims);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Invalid token"));
        }
    }

    public Mono<Boolean> isRefreshToken(String token) {
        return validateAndGetClaims(token)
                .map(claims -> "refresh".equals(claims.get("type")))
                .onErrorReturn(false);
    }

    public Mono<Void> invalidateToken(String token) {
        // Implement token blacklisting logic here
        // For now, just return empty
        return Mono.empty();
    }
}
