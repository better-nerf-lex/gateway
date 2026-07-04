package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.Document;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class DocumentPdfService {

    private static final String PDF_FONT_FAMILY = "LexaiPdfFont";

    private static final List<FontFace> FONT_FACES = List.of(
            new FontFace(400, BaseRendererBuilder.FontStyle.NORMAL, List.of(
                    Path.of("/usr/share/fonts/dejavu/DejaVuSans.ttf"),
                    Path.of("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf"),
                    Path.of("/usr/share/fonts/liberation/LiberationSans-Regular.ttf"),
                    Path.of("/System/Library/Fonts/Supplemental/Arial Unicode.ttf"),
                    Path.of("/System/Library/Fonts/Supplemental/Arial.ttf"),
                    Path.of("/Library/Fonts/Arial Unicode.ttf"),
                    Path.of("/Library/Fonts/Arial.ttf"))),
            new FontFace(700, BaseRendererBuilder.FontStyle.NORMAL, List.of(
                    Path.of("/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf"),
                    Path.of("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans-Bold.ttf"),
                    Path.of("/System/Library/Fonts/Supplemental/Arial Bold.ttf"),
                    Path.of("/Library/Fonts/Arial Bold.ttf"))),
            new FontFace(400, BaseRendererBuilder.FontStyle.ITALIC, List.of(
                    Path.of("/usr/share/fonts/dejavu/DejaVuSans-Oblique.ttf"),
                    Path.of("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans-Oblique.ttf"),
                    Path.of("/System/Library/Fonts/Supplemental/Arial Italic.ttf"),
                    Path.of("/Library/Fonts/Arial Italic.ttf"))),
            new FontFace(700, BaseRendererBuilder.FontStyle.ITALIC, List.of(
                    Path.of("/usr/share/fonts/dejavu/DejaVuSans-BoldOblique.ttf"),
                    Path.of("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans-BoldOblique.ttf"),
                    Path.of("/System/Library/Fonts/Supplemental/Arial Bold Italic.ttf"),
                    Path.of("/Library/Fonts/Arial Bold Italic.ttf"))));

    private static final List<Path> REGULAR_FONT_CANDIDATES = List.of(
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
        List<Path> registeredFonts = new ArrayList<>();

        FONT_FACES.forEach(face -> face.findExistingFont().ifPresent(font -> {
            builder.useFont(font.toFile(), PDF_FONT_FAMILY, face.weight(), face.style(), true);
            registeredFonts.add(font);
        }));

        if (registeredFonts.isEmpty()) {
            REGULAR_FONT_CANDIDATES.stream()
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .ifPresent(font -> {
                        builder.useFont(font.toFile(), PDF_FONT_FAMILY);
                        registeredFonts.add(font);
                    });
        }

        if (registeredFonts.isEmpty()) {
            log.warn("No PDF font candidate found; generated PDFs may miss non-ASCII glyphs");
        } else {
            log.debug("Registered PDF fonts: {}", registeredFonts);
        }
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
                            font-family: "LexaiPdfFont", "DejaVu Sans", Arial, sans-serif !important;
                        }
                        body {
                            margin: 0;
                            color: #111827;
                            font-family: "LexaiPdfFont", "DejaVu Sans", Arial, sans-serif !important;
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
                .replaceAll("(?i)font-family\\s*:[^;]+;?", "")
                .replaceAll("(?i)(position|behavior|-moz-binding)\\s*:[^;]+;?", "");
    }

    private String escape(String value) {
        return Entities.escape(value);
    }

    private record FontFace(int weight, BaseRendererBuilder.FontStyle style, List<Path> candidates) {
        private java.util.Optional<Path> findExistingFont() {
            return candidates.stream()
                    .filter(Files::isRegularFile)
                    .findFirst();
        }
    }
}
