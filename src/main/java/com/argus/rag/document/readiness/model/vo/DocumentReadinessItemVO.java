package com.argus.rag.document.readiness.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DocumentReadinessItemVO(
        Long id,
        Long batchId,
        Long groupId,
        String originalFileName,
        String fileExt,
        Long fileSize,
        String parseStatus,
        String readinessStatus,
        Long recommendedGroupId,
        String recommendedAction,
        BigDecimal readinessScore,
        String readinessGrade,
        Boolean parseable,
        Integer chunkCount,
        Integer avgChunkLength,
        Integer shortChunkCount,
        Integer longChunkCount,
        Integer emptyChunkCount,
        Integer duplicateChunkCount,
        Long duplicateDocumentId,
        Integer issueCount,
        String analysisSummary,
        String failureReason,
        Long importedDocumentId,
        String decisionReason,
        String topicFitStatus,
        BigDecimal topicFitScore,
        BigDecimal topicConfidence,
        String topicReason,
        List<String> groupTopicKeywords,
        List<String> documentTopicKeywords,
        BigDecimal keywordOverlapScore,
        BigDecimal semanticSimilarityScore,
        List<String> mismatchIndicators,
        Long suggestedTargetGroupId,
        String suggestedTargetGroupName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DocumentReadinessIssueVO> issues
) {
}
