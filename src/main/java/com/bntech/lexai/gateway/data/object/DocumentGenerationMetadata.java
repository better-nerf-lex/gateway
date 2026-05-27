package com.bntech.lexai.gateway.data.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGenerationMetadata {
    @Builder.Default
    private List<String> legalBasis = List.of();

    @Builder.Default
    private List<RequiredAttachment> requiredAttachments = List.of();

    public static DocumentGenerationMetadata from(
            List<String> legalBasis,
            List<String> requirements,
            UUID documentId,
            UUID documentGroupId) {
        String documentIdValue = documentId != null ? documentId.toString() : null;
        String documentGroupIdValue = documentGroupId != null ? documentGroupId.toString() : null;
        List<String> safeRequirements = requirements != null ? requirements : List.of();

        return DocumentGenerationMetadata.builder()
                .legalBasis(legalBasis != null ? legalBasis : List.of())
                .requiredAttachments(IntStream.range(0, safeRequirements.size())
                        .mapToObj(index -> RequiredAttachment.builder()
                                .id(buildAttachmentId(documentIdValue, index, safeRequirements.get(index)))
                                .description(safeRequirements.get(index))
                                .documentId(documentIdValue)
                                .documentGroupId(documentGroupIdValue)
                                .build())
                        .toList())
                .build();
    }

    private static String buildAttachmentId(String documentId, int index, String description) {
        String seed = "%s:%d:%s".formatted(documentId != null ? documentId : "document", index, description);
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
