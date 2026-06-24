package com.argus.rag.qa.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Calculates estimated LLM cost from token usage. Values are estimates, not provider bills. */
@Component
public class LlmCostCalculator {

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000L);
    private static final BigDecimal DEFAULT_PROMPT_CNY_PER_MILLION = BigDecimal.valueOf(2.0);
    private static final BigDecimal DEFAULT_COMPLETION_CNY_PER_MILLION = BigDecimal.valueOf(6.0);

    public BigDecimal estimate(String modelName, Integer promptTokens, Integer completionTokens) {
        BigDecimal promptPrice = promptPrice(modelName);
        BigDecimal completionPrice = completionPrice(modelName);
        BigDecimal promptCost = BigDecimal.valueOf(safe(promptTokens)).multiply(promptPrice).divide(MILLION, 8, RoundingMode.HALF_UP);
        BigDecimal completionCost = BigDecimal.valueOf(safe(completionTokens)).multiply(completionPrice).divide(MILLION, 8, RoundingMode.HALF_UP);
        return promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal promptPrice(String modelName) {
        if (!StringUtils.hasText(modelName)) {
            return DEFAULT_PROMPT_CNY_PER_MILLION;
        }
        String normalized = modelName.toLowerCase();
        if (normalized.contains("turbo") || normalized.contains("plus")) {
            return BigDecimal.valueOf(4.0);
        }
        return DEFAULT_PROMPT_CNY_PER_MILLION;
    }

    private BigDecimal completionPrice(String modelName) {
        if (!StringUtils.hasText(modelName)) {
            return DEFAULT_COMPLETION_CNY_PER_MILLION;
        }
        String normalized = modelName.toLowerCase();
        if (normalized.contains("turbo") || normalized.contains("plus")) {
            return BigDecimal.valueOf(12.0);
        }
        return DEFAULT_COMPLETION_CNY_PER_MILLION;
    }

    private long safe(Integer value) {
        return value == null ? 0L : Math.max(0, value);
    }
}
