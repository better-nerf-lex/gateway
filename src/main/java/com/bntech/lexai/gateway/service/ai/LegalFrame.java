package com.bntech.lexai.gateway.service.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalFrame {
    private String documentType;
    private String legalArea;
    private String procedureType;
    private String applicantRole;
    private String requestedOutcome;
    private String authorityRule;
    private List<String> legalBasis;
    private List<String> searchPhrases;
    private List<String> legalInferences;
    private List<String> userFactTargets;
    private List<String> userShouldNotBeAsked;
    private List<String> draftingGuidance;
    private List<String> attachmentRequirements;

    public static LegalFrame empty() {
        return LegalFrame.builder()
                .legalBasis(List.of())
                .searchPhrases(List.of())
                .legalInferences(List.of())
                .userFactTargets(List.of())
                .userShouldNotBeAsked(List.of())
                .draftingGuidance(List.of())
                .attachmentRequirements(List.of())
                .build();
    }

    public boolean isEmptyFrame() {
        return isBlank(documentType)
                && isBlank(legalArea)
                && isBlank(procedureType)
                && isBlank(authorityRule)
                && isEmpty(legalBasis)
                && isEmpty(searchPhrases)
                && isEmpty(legalInferences)
                && isEmpty(userFactTargets)
                && isEmpty(userShouldNotBeAsked)
                && isEmpty(draftingGuidance)
                && isEmpty(attachmentRequirements);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isEmpty(List<String> value) {
        return value == null || value.isEmpty();
    }
}
