package com.argus.rag.document.readiness.support;

import org.springframework.stereotype.Component;

@Component
public class TopicSuitabilityAnalyzer {
    public boolean lowSuitability(String fileName, String text) {
        String normalized = ((fileName == null ? "" : fileName) + "\n" + (text == null ? "" : text)).toLowerCase();
        return normalized.contains("归档") || normalized.contains("archive") || normalized.contains("obsolete");
    }
}
