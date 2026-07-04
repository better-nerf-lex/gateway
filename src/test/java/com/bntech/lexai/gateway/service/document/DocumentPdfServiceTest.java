package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentPdfServiceTest {

    private final DocumentPdfService service = new DocumentPdfService();

    @Test
    void generatesPdfBytesFromPolishLegalHtml() {
        Document document = Document.builder()
                .title("Wniosek o zażalenie: ąćęłńóśźż")
                .text("""
                        <h1 style="font-family: Arial, sans-serif;">Wniosek o zażalenie: ąćęłńóśźż</h1>
                        <p style="font-family: Times New Roman, serif;">
                            Wnoszę o zmianę zaskarżonego postanowienia oraz uwzględnienie żądania strony.
                        </p>
                        <ol>
                            <li><strong>załączenie</strong> dowodu uiszczenia opłaty,</li>
                            <li><em>doręczenie</em> odpisu stronie przeciwnej.</li>
                        </ol>
                        """)
                .build();

        byte[] pdf = service.generate(document).block(Duration.ofSeconds(10));

        assertThat(pdf).isNotNull();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(1_000);

        assertThat(extractText(pdf)).contains(
                "Wniosek o zażalenie",
                "ąćęłńóśźż",
                "Wnoszę",
                "zaskarżonego",
                "uwzględnienie",
                "żądania",
                "załączenie",
                "doręczenie");
    }

    private String extractText(byte[] pdf) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(document);
        } catch (IOException error) {
            throw new AssertionError("Could not extract PDF text", error);
        }
    }
}
