package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.data.object.LegalFrame;
import com.bntech.lexai.gateway.rest.dto.document.QuestionDto;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class QuestionQualityService {

    private static final List<String> CONDITIONAL_FOLLOW_UP_MARKERS = List.of(
            "jesli tak",
            "jezeli tak",
            "gdy tak",
            "w przypadku odpowiedzi twierdzacej",
            "w przypadku odpowiedzi pozytywnej",
            "if yes",
            "if so");

    private static final Pattern CONDITIONAL_TAIL = Pattern.compile(
            "(?iu)([\\s,;]*(?:a\\s+)?(?:jeśli|jezeli|jeżeli|gdy)\\s+tak[,:]?\\s*.*)$");

    public SubmitPromptResponse cleanFirstPassQuestions(SubmitPromptResponse response) {
        return cleanFirstPassQuestions(response, null);
    }

    public SubmitPromptResponse cleanFirstPassQuestions(SubmitPromptResponse response, LegalFrame legalFrame) {
        if (response == null || response.getQuestions() == null) {
            return response;
        }

        response.getQuestions().forEach(question -> {
            if (question.getText() != null && hasConditionalFollowUp(question)) {
                question.setText(removeConditionalTail(question.getText()));
                question.setFollowup(true);
            }
            rewriteVagueLegalQuestion(question, legalFrame);
        });
        response.setQuestions(removeDuplicateQuestions(response.getQuestions()).stream()
                .filter(question -> !isApplicationOwnedQuestion(question))
                .toList());
        return response;
    }

    public boolean hasConditionalFollowUp(QuestionDto question) {
        if (question == null || question.getText() == null) {
            return false;
        }
        String normalized = normalize(question.getText());
        return CONDITIONAL_FOLLOW_UP_MARKERS.stream().anyMatch(normalized::contains);
    }

    public SubmitPromptResponse retainVerifiedSuggestions(
            SubmitPromptResponse response,
            List<String> attachedContextFileNames) {
        if (response == null || response.getQuestions() == null) {
            return response;
        }

        Map<String, String> actualNames = new LinkedHashMap<>();
        if (attachedContextFileNames != null) {
            attachedContextFileNames.stream()
                    .filter(name -> name != null && !name.isBlank())
                    .forEach(name -> actualNames.putIfAbsent(normalize(name).strip(), name));
        }

        response.getQuestions().forEach(question -> {
            String suggestion = question.getSuggestedAnswer();
            if (suggestion == null || suggestion.isBlank()) {
                clearSuggestion(question);
                return;
            }

            LinkedHashSet<String> verifiedSources = new LinkedHashSet<>();
            if (question.getSuggestedAnswerSources() != null) {
                question.getSuggestedAnswerSources().stream()
                        .map(source -> source == null ? null : actualNames.get(normalize(source).strip()))
                        .filter(source -> source != null && !source.isBlank())
                        .forEach(verifiedSources::add);
            }

            if (verifiedSources.isEmpty()) {
                clearSuggestion(question);
                return;
            }

            question.setSuggestedAnswer(suggestion.strip());
            question.setSuggestedAnswerSources(new ArrayList<>(verifiedSources));
        });
        return response;
    }

    private void clearSuggestion(QuestionDto question) {
        question.setSuggestedAnswer(null);
        question.setSuggestedAnswerSources(List.of());
    }

    private String removeConditionalTail(String text) {
        String cleaned = CONDITIONAL_TAIL.matcher(text).replaceFirst("").trim();
        return cleaned.isBlank() ? text : cleaned;
    }

    private void rewriteVagueLegalQuestion(QuestionDto question, LegalFrame legalFrame) {
        if (question == null || question.getText() == null) {
            return;
        }

        String normalized = normalize(question.getText());
        if (normalized.matches(".*\\b(podaj|wskaz|opisz)\\s+uzasadnienie\\b.*")
                || normalized.matches(".*\\bjakie\\s+jest\\s+uzasadnienie\\b.*")) {
            question.setText(buildJustificationFactsQuestion(legalFrame));
            question.setFollowup(false);
            return;
        }

        if (normalized.matches(".*\\b(podaj|wskaz|opisz)\\s+przestepstwo\\b.*")) {
            question.setText("Z jakich artykułów został wydany wyrok lub postawiony zarzut i jak krótko opisać czyn wskazany w orzeczeniu lub dokumencie sprawy?");
            question.setFollowup(false);
            return;
        }

        if (normalized.matches(".*\\bczy\\s+(sa|zostaly|spelnione).*\\bwarunki\\b.*")
                || normalized.matches(".*\\bczy\\s+wnioskodawca\\s+spelnia\\b.*")) {
            question.setText(buildLegalConditionsFactsQuestion(legalFrame));
            question.setFollowup(false);
        }
    }

    private String buildJustificationFactsQuestion(LegalFrame legalFrame) {
        if (legalFrame != null && legalFrame.getUserFactTargets() != null
                && !legalFrame.getUserFactTargets().isEmpty()) {
            return "Napisz krótko, co wiesz o tych kwestiach: "
                    + String.join(", ", legalFrame.getUserFactTargets()) + ".";
        }
        return "Co się wydarzyło i dlaczego potrzebujesz tego pisma? Opisz krótko najważniejsze fakty, daty, skutki i dokumenty, które je potwierdzają.";
    }

    private String buildLegalConditionsFactsQuestion(LegalFrame legalFrame) {
        if (legalFrame != null && legalFrame.getUserFactTargets() != null
                && !legalFrame.getUserFactTargets().isEmpty()) {
            return "Napisz, co wiesz o tych kwestiach i jakie masz dokumenty: "
                    + String.join(", ", legalFrame.getUserFactTargets()) + ".";
        }
        return "Jakie fakty i dokumenty mogą pomóc sądowi lub urzędowi ocenić sprawę?";
    }

    private List<QuestionDto> removeDuplicateQuestions(List<QuestionDto> questions) {
        List<QuestionDto> unique = new ArrayList<>();
        for (QuestionDto question : questions) {
            String normalized = normalize(question.getText());
            boolean alreadyPresent = unique.stream()
                    .map(QuestionDto::getText)
                    .map(this::normalize)
                    .anyMatch(normalized::equals);
            if (!alreadyPresent) {
                unique.add(question);
            }
        }
        return unique;
    }

    private boolean isApplicationOwnedQuestion(QuestionDto question) {
        String text = normalize(question != null ? question.getText() : null);
        return text.matches(".*\\b(data|date|dzien)\\s+(sporzadzenia|utworzenia|przygotowania)\\s+(pisma|dokumentu|wniosku)\\b.*")
                || text.matches(".*\\b(miejsce|dane)\\s+(na|do)?\\s*podpis(u)?\\b.*")
                || text.matches(".*\\b(jaki|ktory|do jakiego)\\s+(sad|organ)\\s+(jest|bedzie)?\\s*wlasciw.*")
                || text.matches(".*\\b(jakie|ktore)\\s+przepisy\\s+(maja|powinny|beda)?\\s*(zastosowanie|wskazane)?.*")
                || text.matches(".*\\bwska(z|zcie|zac)\\s+podstawe\\s+prawna\\b.*");
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replace('ż', 'z')
                .replace('ź', 'z')
                .replace('ó', 'o')
                .replace('ł', 'l')
                .replace('ć', 'c')
                .replace('ń', 'n')
                .replace('ś', 's')
                .replace('ą', 'a')
                .replace('ę', 'e');
    }
}
