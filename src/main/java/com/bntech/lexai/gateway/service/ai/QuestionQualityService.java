package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.rest.dto.document.QuestionDto;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import org.springframework.stereotype.Service;

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
        if (response == null || response.getQuestions() == null) {
            return response;
        }

        response.getQuestions().forEach(question -> {
            if (question.getText() != null && hasConditionalFollowUp(question)) {
                question.setText(removeConditionalTail(question.getText()));
                question.setFollowup(true);
            }
        });
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

    private String normalize(String text) {
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
