package com.argus.rag.document.readiness.support;

import com.argus.rag.document.readiness.model.enums.DocumentReadinessGrade;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessIssueSeverity;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessIssueType;
import com.argus.rag.document.readiness.model.entity.DocumentReadinessIssueEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DocumentReadinessScoreCalculator {
    public ScoreResult calculate(boolean parseable, int textLength, DryRunChunkAnalyzer.Metrics metrics,
                                 boolean duplicateDocument, List<DocumentReadinessIssueEntity> issues) {
        int parseabilityScore = parseable ? 20 : 0;
        int structureScore = Math.max(0, 20 - severityCount(issues, DocumentReadinessIssueSeverity.WARNING.name()) * 4
                - severityCount(issues, DocumentReadinessIssueSeverity.BLOCKER.name()) * 10);
        int chunkQualityScore = chunkQualityScore(metrics);
        int topicSuitabilityScore = hasIssue(issues, DocumentReadinessIssueType.LOW_TOPIC_SUITABILITY.name()) ? 7 : 15;
        int retrievalReadinessScore = textLength >= 800 && metrics.chunkCount() > 0 ? 15 : Math.min(15, textLength / 80);
        int duplicateRiskScore = duplicateDocument ? 0 : Math.max(0, 10 - metrics.duplicateChunkCount());
        int total = clamp(parseabilityScore + structureScore + chunkQualityScore + topicSuitabilityScore
                + retrievalReadinessScore + duplicateRiskScore, 0, 100);
        return new ScoreResult(BigDecimal.valueOf(total), grade(total));
    }

    private int chunkQualityScore(DryRunChunkAnalyzer.Metrics metrics) {
        if (metrics.chunkCount() == 0) {
            return 0;
        }
        int score = 20;
        double shortRatio = (double) metrics.shortChunkCount() / metrics.chunkCount();
        double emptyRatio = (double) metrics.emptyChunkCount() / metrics.chunkCount();
        double duplicateRatio = (double) metrics.duplicateChunkCount() / metrics.chunkCount();
        if (shortRatio > 0.35) score -= 6;
        if (emptyRatio > 0.1) score -= 8;
        if (duplicateRatio > 0.2) score -= 6;
        if (metrics.longChunkCount() > 0) score -= 2;
        return Math.max(0, score);
    }

    private int severityCount(List<DocumentReadinessIssueEntity> issues, String severity) {
        return (int) issues.stream().filter(issue -> severity.equals(issue.getSeverity())).count();
    }

    private boolean hasIssue(List<DocumentReadinessIssueEntity> issues, String issueType) {
        return issues.stream().anyMatch(issue -> issueType.equals(issue.getIssueType()));
    }

    private DocumentReadinessGrade grade(int total) {
        if (total >= 85) return DocumentReadinessGrade.RECOMMENDED;
        if (total >= 70) return DocumentReadinessGrade.ACCEPTABLE;
        if (total >= 50) return DocumentReadinessGrade.NEEDS_REVIEW;
        return DocumentReadinessGrade.NOT_RECOMMENDED;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record ScoreResult(BigDecimal score, DocumentReadinessGrade grade) {
    }
}
