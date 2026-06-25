package com.argus.rag.document.readiness.model.enums;

public enum DocumentReadinessAction {
    IMPORT_TO_TARGET_GROUP,
    IMPORT_TO_RECOMMENDED_GROUP,
    CLEAN_BEFORE_IMPORT,
    NEEDS_OCR,
    SPLIT_DOCUMENT,
    MERGE_DUPLICATES,
    ARCHIVE_ONLY,
    REJECT
}
