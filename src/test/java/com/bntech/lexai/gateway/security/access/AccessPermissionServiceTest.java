package com.bntech.lexai.gateway.security.access;

import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessPermissionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private LegalEntityRepository legalEntityRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private Authentication authentication;

    private AccessPermissionService accessPermissionService;

    @BeforeEach
    void setUp() {
        accessPermissionService = new AccessPermissionService(
                userRepository,
                teamMemberRepository,
                documentRepository,
                legalEntityRepository,
                caseRepository,
                eventRepository
        );
    }

    @Test
    void isOwnerOrAdminOfDocument_ShouldReturnTrue_WhenUserIsOwner() {
        String username = "testuser";
        Long userId = 1L;
        UUID documentId = UUID.randomUUID();

        User user = User.builder().id(userId).username(username).build();
        Document document = Document.builder().id(documentId).ownerId(userId).build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(documentRepository.findById(documentId)).thenReturn(Mono.just(document));

        StepVerifier.create(accessPermissionService.isOwnerOrAdminOfDocument(authentication, documentId.toString()))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void isOwnerOrAdminOfDocument_ShouldReturnFalse_WhenUserIsNotOwner() {
        String username = "testuser";
        Long userId = 1L;
        Long otherUserId = 2L;
        UUID documentId = UUID.randomUUID();

        User user = User.builder().id(userId).username(username).build();
        Document document = Document.builder().id(documentId).ownerId(otherUserId).build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(documentRepository.findById(documentId)).thenReturn(Mono.just(document));

        StepVerifier.create(accessPermissionService.isOwnerOrAdminOfDocument(authentication, documentId.toString()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isOwnerOrAdminOfDocument_ShouldReturnTrue_WhenUserIsAdmin() {
        String username = "admin";
        UUID documentId = UUID.randomUUID();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation ->
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        StepVerifier.create(accessPermissionService.isOwnerOrAdminOfDocument(authentication, documentId.toString()))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void isOwnerOrAdminOfDocument_ShouldReturnFalse_WhenDocumentNotFound() {
        String username = "testuser";
        Long userId = 1L;
        UUID documentId = UUID.randomUUID();

        User user = User.builder().id(userId).username(username).build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(documentRepository.findById(documentId)).thenReturn(Mono.empty());

        StepVerifier.create(accessPermissionService.isOwnerOrAdminOfDocument(authentication, documentId.toString()))
                .expectNext(false)
                .verifyComplete();
    }
}
