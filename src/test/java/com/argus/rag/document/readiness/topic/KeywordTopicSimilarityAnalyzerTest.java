package com.argus.rag.document.readiness.topic;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordTopicSimilarityAnalyzerTest {
    private final TopicKeywordExtractor keywordExtractor = new TopicKeywordExtractor();
    private final KeywordTopicSimilarityAnalyzer analyzer = new KeywordTopicSimilarityAnalyzer(
            keywordExtractor,
            new TopicSuitabilityScorePolicy()
    );

    @Test
    void chenpiDocumentShouldMismatchResumeKnowledgeBase() {
        TopicProfile group = profile(1L,
                "\u7b80\u5386\u8d44\u6599\u5e93",
                "\u6c42\u804c\u7b80\u5386\u3001\u9879\u76ee\u7ecf\u5386\u3001\u6280\u672f\u6808\u3001\u5b9e\u4e60\u7ecf\u5386\u3001\u8bba\u6587\u6210\u679c\u3001\u6559\u80b2\u80cc\u666f");
        TopicProfile document = profile(10L,
                "\u65b0\u4f1a\u9648\u76ae\u4ea7\u54c1FAQ.md",
                "\u65b0\u4f1a\u9648\u76ae\uff0c\u5e7f\u4e1c\u7279\u4ea7\uff0c\u9648\u76ae\u529f\u6548\uff0c\u51b2\u6ce1\u65b9\u5f0f\uff0c\u5546\u54c1\u552e\u540e\uff0c\u8336\u996e\uff0c\u98df\u54c1\uff0c\u517b\u751f");

        TopicSuitabilityResult result = analyzer.analyze(group, document);

        assertThat(result.status()).isEqualTo(TopicSuitabilityStatus.MISMATCH);
        assertThat(result.finalTopicFitScore()).isLessThan(BigDecimal.valueOf(0.45));
        assertThat(result.reason()).contains("\u9648\u76ae").contains("\u7b80\u5386");
    }

    @Test
    void chenpiDocumentShouldFitChenpiKnowledgeBase() {
        TopicProfile group = profile(2L,
                "\u9648\u76ae\u5546\u5e97\u77e5\u8bc6\u5e93",
                "\u65b0\u4f1a\u9648\u76ae\u3001\u5546\u54c1\u4ecb\u7ecd\u3001\u552e\u540e\u89c4\u5219\u3001\u51b2\u6ce1\u65b9\u6cd5\u3001\u5ba2\u6237\u95ee\u7b54");
        TopicProfile document = profile(10L,
                "\u65b0\u4f1a\u9648\u76ae\u4ea7\u54c1FAQ.md",
                "\u65b0\u4f1a\u9648\u76ae\uff0c\u5e7f\u4e1c\u7279\u4ea7\uff0c\u9648\u76ae\u529f\u6548\uff0c\u51b2\u6ce1\u65b9\u5f0f\uff0c\u5546\u54c1\u552e\u540e\uff0c\u8336\u996e\uff0c\u98df\u54c1\uff0c\u517b\u751f");

        TopicSuitabilityResult result = analyzer.analyze(group, document);

        assertThat(result.status()).isEqualTo(TopicSuitabilityStatus.FIT);
        assertThat(result.finalTopicFitScore()).isGreaterThanOrEqualTo(BigDecimal.valueOf(0.70));
    }

    @Test
    void emptyGroupProfileShouldBeUnknownNotMismatch() {
        TopicProfile group = new TopicProfile(3L, "default", "", "default", List.of(), List.of("groupName"), 0.0);
        TopicProfile document = profile(10L, "normal.md", "normal document text for an uncertain profile");

        TopicSuitabilityResult result = analyzer.analyze(group, document);

        assertThat(result.status()).isEqualTo(TopicSuitabilityStatus.UNKNOWN);
    }

    private TopicProfile profile(Long id, String name, String text) {
        List<String> keywords = keywordExtractor.extract(name + "\n" + text, 20);
        return new TopicProfile(id, name, text, name + "\n" + text, keywords, List.of("test"), 1.0);
    }
}