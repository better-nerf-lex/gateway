package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.Document;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class DocumentPdfService {

    private static final List<Path> FONT_CANDIDATES = List.of(
            Path.of("/usr/share/fonts/dejavu/DejaVuSans.ttf"),
            Path.of("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf"),
            Path.of("/usr/share/fonts/liberation/LiberationSans-Regular.ttf"),
            Path.of("/System/Library/Fonts/Supplemental/Arial Unicode.ttf"),
            Path.of("/System/Library/Fonts/Supplemental/Arial.ttf"),
            Path.of("/Library/Fonts/Arial Unicode.ttf"),
            Path.of("/Library/Fonts/Arial.ttf"));

    public Mono<byte[]> generate(Document document) {
        return Mono.fromCallable(() -> render(document))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private byte[] render(Document document) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFont(builder);
            builder.withHtmlContent(toPrintableHtml(document), null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("Could not generate document PDF", error);
        }
    }

    private void registerFont(PdfRendererBuilder builder) {
        FONT_CANDIDATES.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .map(Path::toFile)
                .ifPresentOrElse(
                        font -> builder.useFont(font, "LexaiPdfFont"),
                        () -> log.warn("No PDF font candidate found; generated PDFs may miss non-ASCII glyphs"));
    }

    private String toPrintableHtml(Document document) {
        String title = document.getTitle() == null || document.getTitle().isBlank()
                ? "Dokument"
                : document.getTitle().strip();
        String body = sanitizeBody(document.getText());

        return """
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        @page {
                            size: A4;
                            margin: 20mm;
                        }
                        * {
                            box-sizing: border-box;
                        }
                        body {
                            margin: 0;
                            color: #111827;
                            font-family: "LexaiPdfFont", "DejaVu Sans", Arial, sans-serif;
                            font-size: 12pt;
                            line-height: 1.5;
                        }
                        p {
                            margin: 0 0 8pt;
                        }
                        h1, h2, h3, h4 {
                            color: #111827;
                            line-height: 1.25;
                            page-break-after: avoid;
                        }
                        h1 {
                            font-size: 18pt;
                            margin: 0 0 14pt;
                        }
                        h2 {
                            font-size: 15pt;
                            margin: 14pt 0 8pt;
                        }
                        h3 {
                            font-size: 13pt;
                            margin: 12pt 0 6pt;
                        }
                        ul, ol {
                            margin: 6pt 0 10pt;
                            padding-left: 18pt;
                        }
                        li {
                            margin: 2pt 0;
                        }
                        table {
                            width: 100%%;
                            border-collapse: collapse;
                            margin: 10pt 0;
                        }
                        th, td {
                            border: 1px solid #d1d5db;
                            padding: 5pt;
                            vertical-align: top;
                        }
                        blockquote {
                            margin: 10pt 0;
                            padding-left: 10pt;
                            border-left: 3px solid #d1d5db;
                            color: #374151;
                        }
                        a {
                            color: #111827;
                            text-decoration: underline;
                        }
                        .text-left, .header-left {
                            text-align: left;
                        }
                        .text-center, .header-center {
                            text-align: center;
                        }
                        .text-right, .header-right {
                            text-align: right;
                        }
                        .text-justify {
                            text-align: justify;
                        }
                    </style>
                    <title>%s</title>
                </head>
                <body>%s</body>
                </html>
                """.formatted(escape(title), body).stripLeading();
    }

    private String sanitizeBody(String html) {
        String source = html == null ? "" : html;
        org.jsoup.nodes.Document parsed = Jsoup.parseBodyFragment(source);
        parsed.outputSettings()
                .charset(StandardCharsets.UTF_8)
                .escapeMode(Entities.EscapeMode.xhtml)
                .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                .prettyPrint(false);

        Safelist safelist = Safelist.relaxed()
                .addTags("section", "article", "header", "footer")
                .addAttributes(":all", "class", "style")
                .addProtocols("a", "href", "http", "https", "mailto");

        String cleaned = Jsoup.clean(parsed.body().html(), "", safelist, parsed.outputSettings());
        org.jsoup.nodes.Document cleanDocument = Jsoup.parseBodyFragment(cleaned);
        cleanDocument.outputSettings(parsed.outputSettings());

        cleanDocument.select("script, iframe, object, embed, link, meta, img").remove();
        cleanDocument.select("*").forEach(this::cleanAttributes);

        return cleanDocument.body().html();
    }

    private void cleanAttributes(Element element) {
        element.attributes().asList().forEach(attribute -> {
            String name = attribute.getKey().toLowerCase(Locale.ROOT);
            String value = attribute.getValue().trim().toLowerCase(Locale.ROOT);

            if (name.startsWith("on")) {
                element.removeAttr(attribute.getKey());
                return;
            }

            if (("href".equals(name) || "src".equals(name)) && value.startsWith("javascript:")) {
                element.removeAttr(attribute.getKey());
                return;
            }

            if ("style".equals(name)) {
                element.attr(attribute.getKey(), normalizeStyle(attribute.getValue()));
            }
        });
    }

    private String normalizeStyle(String style) {
        return style
                .replaceAll("(?i)\\b(?:oklch|oklab|lch|lab|color)\\([^)]*\\)", "#111827")
                .replaceAll("(?i)(position|behavior|-moz-binding)\\s*:[^;]+;?", "");
    }

    private String escape(String value) {
        return Entities.escape(value);
    }
}
