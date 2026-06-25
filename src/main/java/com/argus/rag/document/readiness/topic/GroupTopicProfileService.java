package com.argus.rag.document.readiness.topic;

import com.argus.rag.document.readiness.mapper.TopicProfileMapper;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GroupTopicProfileService {
    private final TopicProfileMapper topicProfileMapper;
    private final TopicKeywordExtractor keywordExtractor;

    public GroupTopicProfileService(TopicProfileMapper topicProfileMapper, TopicKeywordExtractor keywordExtractor) {
        this.topicProfileMapper = topicProfileMapper;
        this.keywordExtractor = keywordExtractor;
    }

    public TopicProfile build(Long groupId) {
        Map<String, Object> row = topicProfileMapper.selectGroupProfile(groupId);
        if (row == null || row.isEmpty()) {
            return new TopicProfile(groupId, "", "", "", List.of(), List.of(), 0.0);
        }
        String groupName = asString(row.get("groupName"));
        String description = asString(row.get("description"));
        List<String> readyDocumentNames = topicProfileMapper.selectReadyDocumentNames(groupId);
        List<String> sourceSignals = new ArrayList<>();
        if (StringUtils.hasText(groupName)) {
            sourceSignals.add("groupName");
        }
        if (StringUtils.hasText(description)) {
            sourceSignals.add("description");
        }
        if (readyDocumentNames != null && !readyDocumentNames.isEmpty()) {
            sourceSignals.add("readyDocumentNames");
        }
        String profileText = String.join("\n", groupName, description,
                readyDocumentNames == null ? "" : String.join("\n", readyDocumentNames));
        List<String> keywords = keywordExtractor.extract(profileText, 16);
        double quality = scoreQuality(groupName, description, readyDocumentNames, keywords);
        return new TopicProfile(groupId, groupName, description, profileText, keywords, sourceSignals, quality);
    }

    private double scoreQuality(String groupName, String description, List<String> readyDocumentNames, List<String> keywords) {
        if (keywords.isEmpty()) {
            return 0.0;
        }
        double score = 0.15;
        if (keywordExtractor.hasResumeDomain(keywords) || keywordExtractor.hasChenpiDomain(keywords)) {
            score += 0.45;
        }
        if (StringUtils.hasText(description) && description.length() >= 8) {
            score += 0.25;
        }
        if (readyDocumentNames != null && !readyDocumentNames.isEmpty()) {
            score += 0.15;
        }
        if (StringUtils.hasText(groupName) && groupName.length() >= 3) {
            score += 0.10;
        }
        return Math.min(1.0, score);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
