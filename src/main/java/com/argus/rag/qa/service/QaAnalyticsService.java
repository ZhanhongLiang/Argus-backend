package com.argus.rag.qa.service;

import com.argus.rag.qa.mapper.QaAnalyticsMapper;
import com.argus.rag.qa.model.QaRecordScope;
import com.argus.rag.qa.model.dto.QaRecordSearchRequest;
import com.argus.rag.qa.model.vo.QaAnalyticsDashboardVO;
import com.argus.rag.qa.model.vo.QaAnalyticsSummaryVO;
import com.argus.rag.qa.model.vo.QaScopeOptionVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Provides V4.2 QA analytics from persisted QA records. */
@Service
public class QaAnalyticsService {

    private final QaRecordScopeResolver scopeResolver;
    private final QaAnalyticsMapper qaAnalyticsMapper;

    public QaAnalyticsService(QaRecordScopeResolver scopeResolver,
                              QaAnalyticsMapper qaAnalyticsMapper) {
        this.scopeResolver = scopeResolver;
        this.qaAnalyticsMapper = qaAnalyticsMapper;
    }

    public List<QaScopeOptionVO> listScopeOptions() {
        return scopeResolver.listScopeOptions();
    }

    public QaAnalyticsDashboardVO dashboard(QaRecordSearchRequest request) {
        QaRecordScopeResolver.ScopeContext scopeContext = scopeResolver.resolve(request.getScope(), request.getGroupId());
        QaAnalyticsSummaryVO summary = qaAnalyticsMapper.selectSummary(scopeContext, request);
        normalizeRates(summary);
        return new QaAnalyticsDashboardVO(
                summary,
                qaAnalyticsMapper.selectTrends(scopeContext, request),
                qaAnalyticsMapper.selectEvidenceLevels(scopeContext, request),
                qaAnalyticsMapper.selectModels(scopeContext, request),
                qaAnalyticsMapper.selectEndpoints(scopeContext, request),
                qaAnalyticsMapper.selectRetrievalSources(scopeContext, request),
                scopeContext.scope() == QaRecordScope.SELF ? List.of() : qaAnalyticsMapper.selectUsers(scopeContext, request)
        );
    }

    private void normalizeRates(QaAnalyticsSummaryVO summary) {
        long total = summary.getTotalQuestions() == null ? 0L : summary.getTotalQuestions();
        if (summary.getEstimatedCost() == null) {
            summary.setEstimatedCost(BigDecimal.ZERO);
        }
        if (total <= 0) {
            summary.setAnsweredRate(0D);
            summary.setRefusalRate(0D);
            summary.setFailureRate(0D);
            return;
        }
        summary.setAnsweredRate(rate(summary.getAnsweredCount(), total));
        summary.setRefusalRate(rate(summary.getRefusalCount(), total));
        summary.setFailureRate(rate(summary.getFailureCount(), total));
    }

    private double rate(Long count, long total) {
        return BigDecimal.valueOf(count == null ? 0L : count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
