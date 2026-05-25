package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.exception.ServiceException;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentGenerationQualityServiceTest {

    private DocumentGenerationQualityService qualityService;

    @BeforeEach
    void setUp() {
        qualityService = new DocumentGenerationQualityService();
    }

    @Test
    void cleanHtmlFragmentRemovesWrapperAndUnsafeTags() {
        String cleaned = qualityService.cleanHtmlFragment("""
                <!DOCTYPE html>
                <html>
                <head><title>Draft</title><script>alert('x')</script></head>
                <body><div><strong>Pozew o zapłatę</strong></div></body>
                </html>
                """);

        assertThat(cleaned).contains("<div>");
        assertThat(cleaned).contains("<strong>Pozew o zapłatę</strong>");
        assertThat(cleaned).doesNotContain("<html", "<body", "<head", "<script");
    }

    @Test
    void validateAcceptsStructuredDocumentWithProvidedAnswers() {
        GenerateDocumentRequest request = GenerateDocumentRequest.builder()
                .title("Pozew o zapłatę")
                .answers(Map.of(
                        "claimant", "Jan Kowalski",
                        "defendant", "ABC Sp. z o.o.",
                        "amount", "12 000 zł",
                        "date", "12 maja 2026 r."))
                .build();

        String content = """
                <div style="font-family: Arial; font-size: 12pt; line-height: 1.5;">
                  <h1 style="text-align: center;">Pozew o zapłatę</h1>
                  <p><strong>Powód:</strong> Jan Kowalski wnosi o zasądzenie należności od ABC Sp. z o.o.</p>
                  <p>Przedmiotem żądania jest kwota 12 000 zł wynikająca z niewykonanego zobowiązania.</p>
                  <p>W dniu 12 maja 2026 r. pozwany pozostawał w opóźnieniu z zapłatą, mimo wezwania do dobrowolnego spełnienia świadczenia.</p>
                  <p>Powód wnosi o zasądzenie dochodzonej kwoty wraz z odsetkami ustawowymi oraz kosztami postępowania według norm przepisanych.</p>
                  <p>Uzasadnienie obejmuje okoliczności zawarcia zobowiązania, wymagalność roszczenia, brak zapłaty oraz potrzebę ochrony prawnej.</p>
                  <p>Załączniki obejmują dokumenty potwierdzające istnienie zobowiązania, wezwanie do zapłaty oraz dowód doręczenia wezwania.</p>
                </div>
                """;

        assertThat(qualityService.validate(content, request)).isEmpty();
    }

    @Test
    void validateRejectsDraftMarkersAndMissingAnswers() {
        GenerateDocumentRequest request = GenerateDocumentRequest.builder()
                .title("Wniosek o alimenty")
                .answers(Map.of(
                        "child", "Anna Kowalska",
                        "amount", "2 000 zł",
                        "parent", "Piotr Nowak"))
                .build();

        String content = """
                ```html
                <html><body>
                  <h1>Wniosek o alimenty</h1>
                  <p>TODO: [uzupełnić dane]. Lorem ipsum.</p>
                </body></html>
                ```
                """;

        assertThat(qualityService.validate(content, request))
                .contains(
                        "document contains full-page wrapper tags instead of an HTML fragment",
                        "document contains markdown code fences",
                        "document contains unresolved placeholders or draft markers",
                        "document omits too many provided answers");
    }

    @Test
    void validateOrThrowUsesServiceException() {
        GenerateDocumentRequest request = GenerateDocumentRequest.builder()
                .title("Pozew o zapłatę")
                .build();

        assertThatThrownBy(() -> qualityService.validateOrThrow("", request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Generated document failed quality checks");
    }

    @Test
    void buildRevisionUserMessageIncludesIssuesAndFailedDraft() {
        String revision = qualityService.buildRevisionUserMessage(
                "{\"name\":\"Pozew o zapłatę\"}",
                "<p>TODO</p>",
                java.util.List.of("document contains unresolved placeholders"));

        assertThat(revision).contains("document contains unresolved placeholders");
        assertThat(revision).contains("{\"name\":\"Pozew o zapłatę\"}");
        assertThat(revision).contains("<p>TODO</p>");
    }
}
