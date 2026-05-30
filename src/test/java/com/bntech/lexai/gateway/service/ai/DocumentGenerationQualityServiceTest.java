package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.exception.ServiceException;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

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
    void removeLegalBasisSectionRemovesTrailingBasisMetadata() {
        String cleaned = qualityService.removeLegalBasisSection("""
                <div>
                  <h1>Pozew o zapłatę</h1>
                  <p>Jan Kowalski wnosi o zasądzenie kwoty 12 000 zł od ABC Sp. z o.o. wraz z kosztami postępowania.</p>
                  <p>Uzasadnienie opisuje zawarcie umowy, wymagalność roszczenia i brak zapłaty mimo wezwania.</p>
                  <h2>Podstawa prawna</h2>
                  <ul><li>eli/DU/1964/93/ogl</li><li>eli/DU/1964/296/ogl</li></ul>
                </div>
                """, List.of("eli/DU/1964/93/ogl", "eli/DU/1964/296/ogl"));

        assertThat(cleaned).contains("Pozew o zapłatę");
        assertThat(cleaned).doesNotContain("Podstawa prawna", "eli/DU/1964/93/ogl", "eli/DU/1964/296/ogl");
    }

    @Test
    void validateRejectsLegalBasisMetadataInDocumentBody() {
        GenerateDocumentRequest request = GenerateDocumentRequest.builder()
                .title("Pozew o zapłatę")
                .base(List.of("eli/DU/1964/93/ogl"))
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
                  <p>W dniu 12 maja 2026 r. pozwany pozostawał w opóźnieniu z zapłatą, mimo wezwania.</p>
                  <p>Powód wnosi o zasądzenie dochodzonej kwoty wraz z odsetkami oraz kosztami postępowania.</p>
                  <p>Uzasadnienie obejmuje okoliczności zobowiązania, wymagalność roszczenia, brak zapłaty oraz potrzebę ochrony prawnej.</p>
                  <p>Załączniki obejmują dokumenty potwierdzające istnienie zobowiązania, wezwanie do zapłaty oraz dowód doręczenia wezwania.</p>
                  <h2>Podstawa prawna</h2>
                  <p>eli/DU/1964/93/ogl</p>
                </div>
                """;

        assertThat(qualityService.validate(content, request))
                .contains("document includes legal basis metadata that must be stored separately");
    }

    @Test
    void validateRejectsPrivateConversationTranscriptArtifacts() {
        GenerateDocumentRequest request = GenerateDocumentRequest.builder()
                .title("Odpowiedź na pozew")
                .answers(Map.of(
                        "participant", "Jan Kowalski",
                        "court", "Sąd Rejonowy w Warszawie",
                        "claim", "oddalenie pozwu"))
                .build();

        String content = """
                <div style="font-family: Arial; font-size: 12pt; line-height: 1.5;">
                  <h1 style="text-align: center;">Odpowiedź na pozew</h1>
                  <p><strong>Pozwany:</strong> Jan Kowalski wnosi do Sąd Rejonowy w Warszawie o oddalenie pozwu w całości.</p>
                  <p>Uzasadnienie przedstawia stanowisko strony pozwanej, wskazuje na brak zasadności roszczenia oraz potrzebę przeprowadzenia dowodów.</p>
                  <p>Strona pozwana kwestionuje twierdzenia powoda co do zasady i wysokości, podnosząc brak wykazania przesłanek odpowiedzialności.</p>
                  <p>Wniosek obejmuje oddalenie pozwu, zasądzenie kosztów postępowania oraz dopuszczenie dowodów z dokumentów i przesłuchania stron.</p>
                  <p>ChatGPT przygotował tę wersję po analizie rozmowy.</p>
                </div>
                """;

        assertThat(qualityService.validate(content, request))
                .contains("document includes private conversation transcript artifacts");
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
