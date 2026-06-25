package com.argus.rag.document.readiness.model.enums;

public enum DocumentReadinessBatchStatus {
    CREATED,
    ANALYZING,
    READY_FOR_REVIEW,
    APPROVED,
    IMPORTING,
    IMPORTED,
    FAILED,
    CANCELED
}
