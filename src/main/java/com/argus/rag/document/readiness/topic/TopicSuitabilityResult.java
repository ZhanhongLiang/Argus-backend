package com.argus.rag.document.readiness.topic;

import java.math.BigDecimal;
import java.util.List;

public record TopicSuitabilityResult(
        TopicSuitabilityStatus status,
        BigDecimal finalTopicFitScore,
        BigDecimal confidence,
        String reason,
        List<String> groupTopicKeywords,
        List<String> documentTopicKeywords,
        BigDecimal keywordOverlapScore,
        BigDecimal semanticSimilarityScore,
        List<String> mismatchIndicators,
        Long suggestedTargetGroupId,
        String suggestedTargetGroupName
) {
    public static TopicSuitabilityResult unknown(String reason, TopicProfile groupProfile, TopicProfile documentProfile) {
        return new TopicSuitabilityResult(
                TopicSuitabilityStatus.UNKNOWN,
                null,
                BigDecimal.valueOf(0.20),
                reason,
                groupProfile == null ? List.of() : groupProfile.topKeywords(),
                documentProfile == null ? List.of() : documentProfile.topKeywords(),
                null,
                null,
                List.of(),
                null,
                null
        );
    }
}
