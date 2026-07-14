package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.rest.dto.document.DocumentExampleDto;
import com.bntech.lexai.gateway.data.object.LegalFrame;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentExampleServiceTest {

    private final DocumentExampleService service = new DocumentExampleService(null, null, null);

    @Test
    void ranksTemplatesUsingLegalFrameAndFiltersWrongLegalArea() {
        LegalFrame frame = LegalFrame.builder()
                .documentType("Wniosek o odbycie kary w systemie dozoru elektronicznego")
                .legalArea("postępowanie wykonawcze karne")
                .legalBasis(List.of("eli/DU/1997/557/ogl"))
                .searchPhrases(List.of("sde", "dozór elektroniczny", "sąd penitencjarny"))
                .build();

        DocumentExampleDto alimony = DocumentExampleDto.builder()
                .id("alimony")
                .title("Wniosek o alimenty")
                .description("Wniosek o zasądzenie świadczeń alimentacyjnych")
                .base(List.of("eli/DU/1964/59/ogl"))
                .build();
        DocumentExampleDto sde = DocumentExampleDto.builder()
                .id("sde")
                .title("Wniosek o odbycie kary w systemie dozoru elektronicznego (SDE)")
                .description("Wniosek do sądu penitencjarnego w postępowaniu wykonawczym")
                .base(List.of("eli/DU/1997/557/ogl"))
                .build();

        List<DocumentExampleDto> ranked = service.rankTemplatesForIntent(
                "wniosek o sde",
                frame,
                List.of(alimony, sde));

        assertThat(ranked).extracting(DocumentExampleDto::getId).containsExactly("sde");
    }

    @Test
    void returnsEmptyWhenOnlyTemplateIsUnrelatedToFramedIntent() {
        LegalFrame frame = LegalFrame.builder()
                .documentType("Wniosek o odbycie kary w systemie dozoru elektronicznego")
                .legalBasis(List.of("eli/DU/1997/557/ogl"))
                .searchPhrases(List.of("sde", "dozór elektroniczny"))
                .build();

        DocumentExampleDto alimony = DocumentExampleDto.builder()
                .id("alimony")
                .title("Wniosek o alimenty")
                .description("Wniosek o zasądzenie świadczeń alimentacyjnych")
                .base(List.of("eli/DU/1964/59/ogl"))
                .build();

        List<DocumentExampleDto> ranked = service.rankTemplatesForIntent(
                "wniosek o sde",
                frame,
                List.of(alimony));

        assertThat(ranked).isEmpty();
    }
}
