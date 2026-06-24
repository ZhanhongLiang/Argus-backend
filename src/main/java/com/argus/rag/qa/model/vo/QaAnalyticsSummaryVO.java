package com.argus.rag.qa.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/** Top-level QA analytics metrics for a resolved visibility scope. */
@Data
public class QaAnalyticsSummaryVO {
    private Long totalQuestions = 0L;
    private Long answeredCount = 0L;
    private Long refusalCount = 0L;
    private Long failureCount = 0L;
    private Double answeredRate = 0D;
    private Double refusalRate = 0D;
    private Double failureRate = 0D;
    private Double avgLatencyMs = 0D;
    private Double p95LatencyMs = 0D;
    private Long totalPromptTokens = 0L;
    private Long totalCompletionTokens = 0L;
    private Long totalTokens = 0L;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private String currency = "CNY";
}
