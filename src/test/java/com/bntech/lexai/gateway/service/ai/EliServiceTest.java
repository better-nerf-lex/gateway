package com.bntech.lexai.gateway.service.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EliServiceTest {

    @Test
    void resolvesOriginalEliIdentifierToCurrentTextAndFreshnessMetadata() {
        EliService eliService = new EliService(webClientForEli("""
                {
                  "title": "Ustawa z dnia 6 czerwca 1997 r. - Kodeks karny wykonawczy.",
                  "status": "akt posiada tekst jednolity",
                  "changeDate": "2026-02-03T15:12:07",
                  "inForce": "IN_FORCE",
                  "textHTML": true,
                  "textPDF": true,
                  "references": {
                    "Inf. o tekście jednolitym": [{"id": "DU/2025/911"}, {"id": "DU/2024/706"}],
                    "Akty zmieniające": [
                      {"id": "DU/2025/621", "date": "2025-06-01"},
                      {"id": "DU/2025/1423", "date": "2026-01-01"},
                      {"id": "DU/2024/1907", "date": "2025-01-01"}
                    ]
                  }
                }
                """, """
                <html><body>
                  <h1>Kodeks karny wykonawczy</h1>
                  <p>Przepisy dotyczące systemu dozoru elektronicznego oraz wykonywania kary pozbawienia wolności.</p>
                </body></html>
                """));

        List<EliDocument> documents = eliService
                .fetchLegalDocuments(List.of("eli/DU/1997/557/ogl"),
                        List.of("wniosek o SDE", "system dozoru elektronicznego"))
                .collectList()
                .block();

        assertThat(documents).hasSize(1);
        EliDocument document = documents.getFirst();
        assertThat(document.uri()).isEqualTo("eli/DU/1997/557/ogl");
        assertThat(document.url()).isEqualTo("https://eli.gov.pl/api/acts/DU/1997/557/text.html");
        assertThat(document.originalUrl()).isEqualTo("https://eli.gov.pl/api/acts/DU/1997/557/text/I/D19970557.pdf");
        assertThat(document.pdfUrl()).isEqualTo("https://eli.gov.pl/api/acts/DU/1997/557/text/U/D19970557Lj.pdf");
        assertThat(document.status()).isEqualTo("akt posiada tekst jednolity");
        assertThat(document.changeDate()).isEqualTo("2026-02-03T15:12:07");
        assertThat(document.latestConsolidatedTexts()).containsExactly("DU/2025/911", "DU/2024/706");
        assertThat(document.recentAmendments()).containsExactly(
                "DU/2025/1423 (od 2026-01-01)",
                "DU/2025/621 (od 2025-06-01)",
                "DU/2024/1907 (od 2025-01-01)");
        assertThat(document.textSnippets()).hasSize(1);
        assertThat(document.textSnippets().getFirst()).contains("systemu dozoru elektronicznego");
    }

    @Test
    void contextTellsGeneratorToUseCurrentSourcesInsteadOfOriginalPublicationOnly() {
        EliService eliService = new EliService();
        List<EliDocument> docs = List.of(new EliDocument(
                "eli/DU/1964/93/ogl",
                "https://eli.gov.pl/api/acts/DU/1964/93/text.html",
                "https://eli.gov.pl/api/acts/DU/1964/93/text/I/D19640093.pdf",
                "https://eli.gov.pl/api/acts/DU/1964/93/text/U/D19640093Lj.pdf",
                "Kodeks cywilny",
                "akt posiada tekst jednolity",
                "IN_FORCE",
                "2026-06-25T09:52:09",
                List.of("DU/2026/795"),
                List.of("DU/2025/123 (od 2025-06-01)"),
                List.of("Art. 471. Dłużnik obowiązany jest do naprawienia szkody...")));

        String context = eliService.buildLegalDocumentsContext(docs);

        assertThat(context).contains("--- AKTUALNE ŹRÓDŁA PRAWA Z ELI");
        assertThat(context).contains("Current text HTML: https://eli.gov.pl/api/acts/DU/1964/93/text.html");
        assertThat(context).contains("Original publication text: https://eli.gov.pl/api/acts/DU/1964/93/text/I/D19640093.pdf");
        assertThat(context).contains("Last ELI metadata update: 2026-06-25T09:52:09");
        assertThat(context).contains("Recent amendments by effective date");
        assertThat(context).contains("Do not rely only on the original publication year");
    }

    @Test
    void fallsBackToOriginalTextUrlWhenMetadataLookupFails() {
        ExchangeFunction failingExchange = request -> Mono.just(ClientResponse
                .create(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{}")
                .build());
        EliService eliService = new EliService(WebClient.builder()
                .baseUrl("https://eli.gov.pl")
                .exchangeFunction(failingExchange)
                .build());

        List<EliDocument> documents = eliService.fetchLegalDocuments(List.of("eli/DU/1964/93/ogl"))
                .collectList()
                .block();

        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().url())
                .isEqualTo("https://eli.gov.pl/api/acts/DU/1964/93/text/I/D19640093.pdf");
    }

    private WebClient webClientForEli(String metadataJson, String htmlText) {
        ExchangeFunction exchange = request -> {
            if (request.url().getPath().endsWith("/text.html")) {
                return response(HttpStatus.OK, MediaType.TEXT_HTML_VALUE, htmlText);
            }
            return response(HttpStatus.OK, MediaType.APPLICATION_JSON_VALUE, metadataJson);
        };

        return WebClient.builder()
                .baseUrl("https://eli.gov.pl")
                .exchangeFunction(exchange)
                .build();
    }

    private Mono<ClientResponse> response(HttpStatus status, String contentType, String body) {
        return Mono.just(ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(body)
                .build());
    }
}
