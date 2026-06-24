package com.argus.rag.qa.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** QA record row used by governance search and export previews. */
public record QaRecordGovernanceItemVO(
        Long id,
        Long userId,
        Long groupId,
        String question,
        String answerPreview,
        Boolean answered,
        Boolean success,
        String reasonCode,
        String evidenceLevel,
        Integer citationCount,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Boolean isEstimated,
        BigDecimal estimatedCost,
        Long latencyMs,
        String modelName,
        String endpoint,
        String errorMessage,
        LocalDateTime createdAt,
        QaEvidenceQualityVO evidenceQuality
) {
}
