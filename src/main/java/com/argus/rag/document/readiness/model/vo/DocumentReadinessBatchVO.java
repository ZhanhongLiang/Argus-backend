package com.argus.rag.document.readiness.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DocumentReadinessBatchVO(
        Long id,
        Long groupId,
        String groupName,
        Long ownerUserId,
        String ownerDisplayName,
        String batchName,
        String status,
        Integer totalCount,
        Integer readyCount,
        Integer warningCount,
        Integer rejectedCount,
        BigDecimal avgReadinessScore,
        String failureReason,
        Long approvedByUserId,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
