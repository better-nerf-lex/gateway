package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.rest.dto.document.QuestionDto;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        response.setQuestions(removeDuplicateQuestions(response.getQuestions()));
        return response;
    }

    public boolean hasConditionalFollowUp(QuestionDto question) {
        if (question == null || question.getText() == null) {
            return false;
        }
        String normalized = normalize(question.getText());
        return CONDITIONAL_FOLLOW_UP_MARKERS.stream().anyMatch(normalized::contains);
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
            return "Jakie konkretne okoliczności faktyczne mają znaczenie dla wniosku: "
                    + String.join(", ", legalFrame.getUserFactTargets()) + "?";
        }
        return "Jakie konkretne okoliczności faktyczne mają uzasadniać pismo (co się wydarzyło, kiedy, jakie są skutki, dowody i sytuacja osoby składającej pismo)?";
    }

    private String buildLegalConditionsFactsQuestion(LegalFrame legalFrame) {
        if (legalFrame != null && legalFrame.getUserFactTargets() != null
                && !legalFrame.getUserFactTargets().isEmpty()) {
            return "Jakie znane fakty i dokumenty mogą mieć znaczenie dla oceny warunków przez sąd lub organ: "
                    + String.join(", ", legalFrame.getUserFactTargets()) + "?";
        }
        return "Jakie znane fakty i dokumenty mogą mieć znaczenie dla oceny warunków przez sąd lub organ?";
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
