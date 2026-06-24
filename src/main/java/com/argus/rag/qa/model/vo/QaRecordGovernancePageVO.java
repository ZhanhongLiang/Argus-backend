package com.argus.rag.qa.model.vo;

import java.util.List;

/** Page response for QA record governance search. */
public record QaRecordGovernancePageVO(
        List<QaRecordGovernanceItemVO> items,
        long total,
        int page,
        int pageSize
) {
}
