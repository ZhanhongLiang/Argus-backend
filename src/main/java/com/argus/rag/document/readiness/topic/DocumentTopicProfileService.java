package com.argus.rag.document.readiness.topic;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DocumentTopicProfileService {
    private static final int PROFILE_TEXT_LIMIT = 12000;

    private final TopicKeywordExtractor keywordExtractor;

    public DocumentTopicProfileService(TopicKeywordExtractor keywordExtractor) {
        this.keywordExtractor = keywordExtractor;
    }

    public TopicProfile build(Long itemId, String fileName, String parsedText) {
        String sample = sample(parsedText);
        String profileText = String.join("\n", safe(fileName), sample);
        List<String> keywords = keywordExtractor.extract(profileText, 20);
        double quality = scoreQuality(fileName, sample, keywords);
        return new TopicProfile(itemId, fileName, "", profileText, keywords, List.of("fileName", "parsedTextPreview"), quality);
    }

    private double scoreQuality(String fileName, String text, List<String> keywords) {
        if (keywords.isEmpty()) {
            return 0.0;
        }
        double score = 0.15;
        if (keywordExtractor.hasResumeDomain(keywords) || keywordExtractor.hasChenpiDomain(keywords)) {
            score += 0.45;
        }
        if (StringUtils.hasText(fileName) && fileName.length() >= 4) {
            score += 0.10;
        }
        if (StringUtils.hasText(text) && text.length() >= 80) {
            score += 0.20;
        }
        if (StringUtils.hasText(text) && text.length() >= 300) {
            score += 0.10;
        }
        return Math.min(1.0, score);
    }

    private String sample(String parsedText) {
        if (!StringUtils.hasText(parsedText)) {
            return "";
        }
        String trimmed = parsedText.trim();
        return trimmed.length() <= PROFILE_TEXT_LIMIT ? trimmed : trimmed.substring(0, PROFILE_TEXT_LIMIT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
