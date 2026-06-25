package com.argus.rag.document.readiness.topic;

import org.springframework.stereotype.Component;

@Component
public class TopicSuitabilityScorePolicy {
    private static final double FIT_THRESHOLD = 0.70;
    private static final double MISMATCH_THRESHOLD = 0.45;

    public double finalScore(double keywordOverlapScore, double groupProfileQuality,
                             double documentProfileQuality, boolean hasMismatchIndicator) {
        if (hasMismatchIndicator) {
            return Math.min(0.20, keywordOverlapScore);
        }
        double quality = Math.min(groupProfileQuality, documentProfileQuality);
        return clamp(keywordOverlapScore * 0.85 + quality * 0.15);
    }

    public TopicSuitabilityStatus status(double finalScore) {
        if (finalScore >= FIT_THRESHOLD) {
            return TopicSuitabilityStatus.FIT;
        }
        if (finalScore >= MISMATCH_THRESHOLD) {
            return TopicSuitabilityStatus.WARNING;
        }
        return TopicSuitabilityStatus.MISMATCH;
    }

    public double confidence(double groupProfileQuality, double documentProfileQuality, boolean hasMismatchIndicator) {
        double confidence = Math.min(groupProfileQuality, documentProfileQuality);
        if (hasMismatchIndicator) {
            confidence = Math.min(1.0, confidence + 0.20);
        }
        return clamp(confidence);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
