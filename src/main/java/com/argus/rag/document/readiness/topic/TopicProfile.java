package com.argus.rag.document.readiness.topic;

import java.util.List;

public record TopicProfile(
        Long ownerId,
        String name,
        String description,
        String profileText,
        List<String> topKeywords,
        List<String> sourceSignals,
        double profileQuality
) {
    public boolean insufficient() {
        return profileQuality < 0.25 || topKeywords == null || topKeywords.isEmpty();
    }
}
