package com.bntech.lexai.gateway.security.access;

import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.entity.security.TeamMember;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.DocumentRepository;
import com.bntech.lexai.gateway.data.repository.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Component("access")
@RequiredArgsConstructor
public class AccessPermissionService {

    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final DocumentRepository documentRepository;
    private final LegalEntityRepository legalEntityRepository;

    public boolean canListDocuments(Authentication authentication, String teamId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        if (teamId == null || teamId.isBlank()) return true;
        return hasTeamAccess(authentication, teamId, AccessLevel.READ.name());
    }

    public boolean canListLegalEntities(Authentication authentication, String teamId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        if (teamId == null || teamId.isBlank()) return true;
        return hasTeamAccess(authentication, teamId, AccessLevel.READ.name());
    }

    public boolean hasTeamAccess(Authentication authentication, String teamId, String level) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        if (teamId == null || teamId.isBlank()) return false;

        AccessLevel required = parseLevel(level);
        if (isAdmin(authentication) && required == AccessLevel.ADMIN) return true;

        Optional<User> userOpt = userRepository.findByUsername(authentication.getName())
                .blockOptional(Duration.ofSeconds(2));
        if (userOpt.isEmpty()) return false;

        return teamMemberRepository.findByUserIdAndTeamId(userOpt.get().getId(), teamId)
                .map(tm -> tm.getAccessLevel().ordinal() >= required.ordinal())
                .defaultIfEmpty(false)
                .block(Duration.ofSeconds(2));
    }

    public boolean hasTeamAccessByDocument(Authentication authentication, String id, String level) {
        if (id == null || id.isBlank()) return false;
        Optional<Document> doc = documentRepository.findById(id).blockOptional(Duration.ofSeconds(2));
        String teamId = doc.map(Document::getTeamId).orElse(null);
        if (teamId == null || teamId.isBlank()) return false;
        return hasTeamAccess(authentication, teamId, level);
    }

    public boolean hasTeamAccessByLegalEntity(Authentication authentication, String id, String level) {
        if (id == null || id.isBlank()) return false;
        Optional<LegalEntity> le = legalEntityRepository.findById(id).blockOptional(Duration.ofSeconds(2));
        String teamId = le.map(LegalEntity::getTeamId).orElse(null);
        if (teamId == null || teamId.isBlank()) return false;
        return hasTeamAccess(authentication, teamId, level);
    }

    public boolean isOwnerOrAdminOfDocument(Authentication authentication, String id) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        if (id == null || id.isBlank()) return false;
        if (isAdmin(authentication)) return true;

        Optional<User> userOpt = userRepository.findByUsername(authentication.getName())
                .blockOptional(Duration.ofSeconds(2));
        if (userOpt.isEmpty()) return false;
        Long uid = userOpt.get().getId();
        return documentRepository.findById(id)
                .map(doc -> String.valueOf(uid).equals(doc.getUserId()))
                .defaultIfEmpty(false)
                .block(Duration.ofSeconds(2));
    }

    public boolean isOwnerOrAdminOfLegalEntity(Authentication authentication, String id) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        if (id == null || id.isBlank()) return false;
        if (isAdmin(authentication)) return true;

        Optional<User> userOpt = userRepository.findByUsername(authentication.getName())
                .blockOptional(Duration.ofSeconds(2));
        if (userOpt.isEmpty()) return false;
        Long uid = userOpt.get().getId();
        return legalEntityRepository.findById(id)
                .map(e -> String.valueOf(uid).equals(e.getUserId()))
                .defaultIfEmpty(false)
                .block(Duration.ofSeconds(2));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private AccessLevel parseLevel(String level) {
        try {
            return AccessLevel.valueOf(level);
        } catch (Exception e) {
            return AccessLevel.READ;
        }
    }
}
