package com.argus.rag.qa.model.vo;

import java.util.List;

/** Preview result before a QA record cleanup operation. */
public record QaCleanupPreviewVO(
        long matchedCount,
        long deletableCount,
        List<QaRecordGovernanceItemVO> samples
) {
}
