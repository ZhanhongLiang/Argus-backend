package com.argus.rag.document.readiness.model.vo;

import java.util.List;

public record DocumentReadinessReportVO(
        DocumentReadinessBatchVO batch,
        List<DocumentReadinessItemVO> items,
        String scoreNotice
) {
}
