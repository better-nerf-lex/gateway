package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.Document;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentPdfServiceTest {

    private final DocumentPdfService service = new DocumentPdfService();

    @Test
    void generatesPdfBytesFromPolishLegalHtml() {
        Document document = Document.builder()
                .title("Wniosek o zażalenie")
                .text("""
                        <h1>Wniosek o zażalenie</h1>
                        <p>Wnoszę o zmianę zaskarżonego postanowienia oraz uwzględnienie żądania strony.</p>
                        <ol>
                            <li>załączenie dowodu uiszczenia opłaty,</li>
                            <li>doręczenie odpisu stronie przeciwnej.</li>
                        </ol>
                        """)
                .build();

        byte[] pdf = service.generate(document).block(Duration.ofSeconds(10));

        assertThat(pdf).isNotNull();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(1_000);
    }
}
