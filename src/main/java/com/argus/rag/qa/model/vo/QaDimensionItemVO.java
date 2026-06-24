package com.argus.rag.qa.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/** Generic grouped QA analytics row. */
@Data
public class QaDimensionItemVO {
    private String name;
    private Long count = 0L;
    private Long totalTokens = 0L;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
}
