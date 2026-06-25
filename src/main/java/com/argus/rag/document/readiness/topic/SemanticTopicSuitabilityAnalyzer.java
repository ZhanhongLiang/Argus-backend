package com.argus.rag.document.readiness.topic;

import org.springframework.stereotype.Service;

@Service
public class SemanticTopicSuitabilityAnalyzer {
    private final GroupTopicProfileService groupTopicProfileService;
    private final DocumentTopicProfileService documentTopicProfileService;
    private final KeywordTopicSimilarityAnalyzer keywordTopicSimilarityAnalyzer;

    public SemanticTopicSuitabilityAnalyzer(GroupTopicProfileService groupTopicProfileService,
                                            DocumentTopicProfileService documentTopicProfileService,
                                            KeywordTopicSimilarityAnalyzer keywordTopicSimilarityAnalyzer) {
        this.groupTopicProfileService = groupTopicProfileService;
        this.documentTopicProfileService = documentTopicProfileService;
        this.keywordTopicSimilarityAnalyzer = keywordTopicSimilarityAnalyzer;
    }

    public TopicSuitabilityResult analyze(Long groupId, Long itemId, String fileName, String parsedText) {
        TopicProfile groupProfile = groupTopicProfileService.build(groupId);
        TopicProfile documentProfile = documentTopicProfileService.build(itemId, fileName, parsedText);
        return keywordTopicSimilarityAnalyzer.analyze(groupProfile, documentProfile);
    }
}
