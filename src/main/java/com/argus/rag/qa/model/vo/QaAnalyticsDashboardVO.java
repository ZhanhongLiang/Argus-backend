package com.argus.rag.qa.model.vo;

import java.util.List;

/** Full QA analytics dashboard payload. */
public record QaAnalyticsDashboardVO(
        QaAnalyticsSummaryVO summary,
        List<QaTrendPointVO> trends,
        List<QaDimensionItemVO> evidenceLevels,
        List<QaDimensionItemVO> models,
        List<QaDimensionItemVO> endpoints,
        List<QaDimensionItemVO> retrievalSources,
        List<QaDimensionItemVO> users
) {
}
