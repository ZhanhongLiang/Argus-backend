package com.argus.rag.document.readiness.service;

import com.argus.rag.document.readiness.model.enums.DocumentReadinessAction;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessGrade;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessIssueSeverity;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessIssueType;
import com.argus.rag.document.readiness.model.entity.DocumentReadinessIssueEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentReadinessRecommendationService {
    public DocumentReadinessAction recommend(DocumentReadinessGrade grade, List<DocumentReadinessIssueEntity> issues) {
        if (has(issues, DocumentReadinessIssueType.SUSPECTED_SCANNED_PDF.name())) {
            return DocumentReadinessAction.NEEDS_OCR;
        }
        if (hasBlocker(issues) || grade == DocumentReadinessGrade.NOT_RECOMMENDED) {
            return DocumentReadinessAction.REJECT;
        }
        if (has(issues, DocumentReadinessIssueType.DUPLICATE_FILE_HASH.name())) {
            return DocumentReadinessAction.MERGE_DUPLICATES;
        }
        if (has(issues, DocumentReadinessIssueType.TOO_MANY_SHORT_CHUNKS.name())) {
            return DocumentReadinessAction.SPLIT_DOCUMENT;
        }
        if (grade == DocumentReadinessGrade.NEEDS_REVIEW) {
            return DocumentReadinessAction.CLEAN_BEFORE_IMPORT;
        }
        return DocumentReadinessAction.IMPORT_TO_TARGET_GROUP;
    }

    private boolean hasBlocker(List<DocumentReadinessIssueEntity> issues) {
        return issues.stream().anyMatch(issue -> DocumentReadinessIssueSeverity.BLOCKER.name().equals(issue.getSeverity()));
    }

    private boolean has(List<DocumentReadinessIssueEntity> issues, String type) {
        return issues.stream().anyMatch(issue -> type.equals(issue.getIssueType()));
    }
}
