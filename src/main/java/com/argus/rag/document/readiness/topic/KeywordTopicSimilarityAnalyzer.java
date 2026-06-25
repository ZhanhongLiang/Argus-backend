package com.argus.rag.document.readiness.topic;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class KeywordTopicSimilarityAnalyzer {
    private final TopicKeywordExtractor keywordExtractor;
    private final TopicSuitabilityScorePolicy scorePolicy;

    public KeywordTopicSimilarityAnalyzer(TopicKeywordExtractor keywordExtractor,
                                          TopicSuitabilityScorePolicy scorePolicy) {
        this.keywordExtractor = keywordExtractor;
        this.scorePolicy = scorePolicy;
    }

    public TopicSuitabilityResult analyze(TopicProfile groupProfile, TopicProfile documentProfile) {
        if (groupProfile == null || documentProfile == null || groupProfile.insufficient() || documentProfile.insufficient()) {
            return TopicSuitabilityResult.unknown("知识库或文档主题画像信息不足，建议人工复核。", groupProfile, documentProfile);
        }
        List<String> groupKeywords = groupProfile.topKeywords();
        List<String> documentKeywords = documentProfile.topKeywords();
        double overlap = keywordOverlap(groupKeywords, documentKeywords);
        overlap = applySameDomainBoost(overlap, groupKeywords, documentKeywords);
        List<String> mismatchIndicators = mismatchIndicators(groupKeywords, documentKeywords);
        double finalScore = scorePolicy.finalScore(overlap, groupProfile.profileQuality(), documentProfile.profileQuality(), !mismatchIndicators.isEmpty());
        TopicSuitabilityStatus status = scorePolicy.status(finalScore);
        BigDecimal score = decimal(finalScore);
        return new TopicSuitabilityResult(
                status,
                score,
                decimal(scorePolicy.confidence(groupProfile.profileQuality(), documentProfile.profileQuality(), !mismatchIndicators.isEmpty())),
                reason(status, groupProfile, documentProfile, mismatchIndicators, score),
                groupKeywords,
                documentKeywords,
                decimal(overlap),
                null,
                mismatchIndicators,
                null,
                null
        );
    }

    private double keywordOverlap(List<String> groupKeywords, List<String> documentKeywords) {
        Set<String> group = new LinkedHashSet<>(groupKeywords);
        Set<String> document = new LinkedHashSet<>(documentKeywords);
        group.retainAll(document);
        int denominator = Math.min(groupKeywords.size(), documentKeywords.size());
        if (denominator == 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) group.size() / denominator);
    }

    private double applySameDomainBoost(double overlap, List<String> groupKeywords, List<String> documentKeywords) {
        boolean sameResumeDomain = keywordExtractor.hasResumeDomain(groupKeywords) && keywordExtractor.hasResumeDomain(documentKeywords);
        boolean sameChenpiDomain = keywordExtractor.hasChenpiDomain(groupKeywords) && keywordExtractor.hasChenpiDomain(documentKeywords);
        if (sameResumeDomain || sameChenpiDomain) {
            return Math.max(overlap, 0.78);
        }
        return overlap;
    }
    private List<String> mismatchIndicators(List<String> groupKeywords, List<String> documentKeywords) {
        boolean groupResume = keywordExtractor.hasResumeDomain(groupKeywords);
        boolean groupChenpi = keywordExtractor.hasChenpiDomain(groupKeywords);
        boolean documentResume = keywordExtractor.hasResumeDomain(documentKeywords);
        boolean documentChenpi = keywordExtractor.hasChenpiDomain(documentKeywords);
        if (groupResume && documentChenpi && !documentResume) {
            return List.of("目标知识库偏向简历/求职/项目经历，候选文档偏向陈皮/商品/食品。");
        }
        if (groupChenpi && documentResume && !documentChenpi) {
            return List.of("目标知识库偏向陈皮/商品/客户问答，候选文档偏向简历/求职/项目经历。");
        }
        return List.of();
    }

    private String reason(TopicSuitabilityStatus status, TopicProfile groupProfile, TopicProfile documentProfile,
                          List<String> mismatchIndicators, BigDecimal score) {
        if (!mismatchIndicators.isEmpty()) {
            return mismatchIndicators.getFirst() + "主题适配度 " + score + "，建议拒绝或改投更合适的知识库。";
        }
        return switch (status) {
            case FIT -> "候选文档关键词与目标知识库主题匹配，可保留原 V5.0 入库建议。";
            case WARNING -> "候选文档与目标知识库存在部分主题重合，但置信度不足，建议 OWNER 人工复核。";
            case MISMATCH -> "候选文档主题与目标知识库关键词重合较低，建议拒绝或人工复核。";
            case UNKNOWN -> "知识库或文档主题画像不足，无法可靠判断主题适配度。";
        };
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
