package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.DocumentGroupDto;
import com.bntech.lexai.gateway.service.DocumentGroupService;
import com.bntech.lexai.gateway.service.StorageFileService;
import com.bntech.lexai.gateway.service.JwtService;
import com.bntech.lexai.gateway.service.DatabaseUserDetailsService;
import com.bntech.lexai.gateway.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(DocumentGroupController.class)
class DocumentGroupControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DocumentGroupService documentGroupService;

    @MockitoBean
    private StorageFileService documentFileService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(jwtAuthenticationFilter.filter(any(ServerWebExchange.class), any(WebFilterChain.class)))
                .thenAnswer(invocation -> {
                    ServerWebExchange exchange = invocation.getArgument(0);
                    WebFilterChain chain = invocation.getArgument(1);
                    return chain.filter(exchange);
                });
    }

    @Test
    @WithMockUser
    void assignToCase_ShouldReturnOk() {
        String groupId = UUID.randomUUID().toString();
        String caseId = UUID.randomUUID().toString();
        DocumentGroupDto dto = DocumentGroupDto.builder()
                .id(groupId)
                .caseId(caseId)
                .name("Test Group")
                .build();

        when(documentGroupService.assignToCase(groupId, caseId)).thenReturn(Mono.just(dto));

        webTestClient.mutateWith(csrf())
                .put()
                .uri("/document-groups/{id}/case/{caseId}", groupId, caseId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(groupId)
                .jsonPath("$.caseId").isEqualTo(caseId);
    }

    @Test
    @WithMockUser
    void removeFromCase_ShouldReturnOk() {
        String groupId = UUID.randomUUID().toString();
        DocumentGroupDto dto = DocumentGroupDto.builder()
                .id(groupId)
                .caseId(null)
                .name("Test Group")
                .build();

        when(documentGroupService.removeFromCase(groupId)).thenReturn(Mono.just(dto));

        webTestClient.mutateWith(csrf())
                .delete()
                .uri("/document-groups/{id}/case", groupId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(groupId)
                .jsonPath("$.caseId").isEmpty();
    }
}
