package com.argus.rag.qa.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/** Daily QA analytics trend point. */
@Data
public class QaTrendPointVO {
    private String date;
    private Long totalQuestions = 0L;
    private Long answeredCount = 0L;
    private Long refusalCount = 0L;
    private Long failureCount = 0L;
    private Long totalTokens = 0L;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private Double avgLatencyMs = 0D;
}
