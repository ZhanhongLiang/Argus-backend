package com.argus.rag.document.readiness.model.vo;

import java.time.LocalDateTime;

public record DocumentReadinessIssueVO(
        Long id,
        Long itemId,
        String issueType,
        String severity,
        String message,
        String suggestion,
        LocalDateTime createdAt
) {
}
