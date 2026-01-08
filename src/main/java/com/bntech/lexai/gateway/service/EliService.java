package com.bntech.lexai.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class EliService {

    private static final String ELI_BASE_URL = "https://eli.gov.pl";

    public record EliDocument(String uri, String url) {}

    public Flux<EliDocument> fetchLegalDocuments(List<String> eliUris) {
        if (eliUris == null || eliUris.isEmpty()) {
            return Flux.empty();
        }

        log.info("Generating URLs for {} ELI documents", eliUris.size());

        return Flux.fromIterable(eliUris)
                .filter(this::isValidEliUri)
                .map(uri -> {
                    String url = buildEliUrl(uri);
                    log.debug("Generated URL for {}: {}", uri, url);
                    return new EliDocument(uri, url);
                })
                .doOnComplete(() -> log.info("Generated URLs for all ELI documents"));
    }

    private String buildEliUrl(String eliUri) {
        if (eliUri.startsWith("http")) {
            return eliUri;
        }

        try {
            String cleanUri = eliUri.startsWith("/") ? eliUri.substring(1) : eliUri;
            String[] parts = cleanUri.split("/");

            if (parts.length >= 4 && parts[0].equalsIgnoreCase("eli")) {
                String journal = parts[1].toUpperCase();
                String year = parts[2];
                String position = parts[3];
                String status = parts.length > 4 ? parts[4].toLowerCase() : "ogl";

                String journalLetter = journal.equals("MP") ? "M" : "D";
                String paddedPosition = position.matches("\\d+")
                        ? String.format("%04d", Integer.parseInt(position))
                        : position;

                String statusMapped = status.equals("ogl") ? "I" : "U";
                String filename = String.format("%s%s%s.pdf", journalLetter, year, paddedPosition);

                return String.format("%s/api/acts/%s/%s/%s/text/%s/%s",
                        ELI_BASE_URL, journal, year, position, statusMapped, filename);
            }
        } catch (Exception e) {
            log.warn("Failed to build API URL for {}, using fallback format", eliUri);
        }

        String cleanUri = eliUri.startsWith("/") ? eliUri : "/" + eliUri;
        if (!cleanUri.contains("/pol/")) {
            cleanUri = cleanUri + "/pol";
        }
        if (!cleanUri.endsWith("/html") && !cleanUri.endsWith("/pdf")) {
            cleanUri = cleanUri + "/html";
        }

        return ELI_BASE_URL + cleanUri;
    }

    private boolean isValidEliUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }

        Pattern eliPattern = Pattern.compile(
            "^(https?://eli\\.gov\\.pl)?/(eli|api/acts)/[A-Z]+/\\d{4}/\\d+.*$",
            Pattern.CASE_INSENSITIVE
        );

        return eliPattern.matcher(uri).matches() || uri.startsWith("eli/") || uri.startsWith("/eli/");
    }

    public String buildLegalDocumentsContext(List<EliDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n\n--- PODSTAWY PRAWNE (Legal Basis) ---\n\n");
        context.append("Poniższe akty prawne są dostępne pod następującymi linkami:\n");
        context.append("(The following legal acts are available at these links:)\n\n");

        for (EliDocument doc : documents) {
            context.append("- ").append(doc.uri()).append("\n");
            context.append("  Link: ").append(doc.url()).append("\n\n");
        }

        context.append("UWAGA: Powyższe linki służą wyłącznie jako odniesienie do podstawy prawnej.\n");
        context.append("NOTE: The above links serve only as references to the legal basis.\n");

        return context.toString();
    }
}
