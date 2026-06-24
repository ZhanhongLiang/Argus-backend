package com.argus.rag.qa.support;

import com.argus.rag.qa.model.entity.QaRecordCitationEntity;
import com.argus.rag.qa.model.entity.QaRecordEntity;
import com.argus.rag.qa.model.vo.QaEvidenceQualityVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Scores evidence support quality from persisted QA signals. This is not answer correctness. */
@Component
public class QaEvidenceQualityScorer {

    public QaEvidenceQualityVO score(QaRecordEntity record, List<QaRecordCitationEntity> citations) {
        List<String> factors = new ArrayList<>();
        if (Boolean.FALSE.equals(record.getSuccess())) {
            factors.add("系统失败，证据质量降权");
            return new QaEvidenceQualityVO(0, "FAILED", factors);
        }

        int score = evidenceLevelScore(record.getEvidenceLevel(), factors);
        int citationCount = citations == null ? 0 : citations.size();
        if (citationCount >= 5) {
            score += 15;
            factors.add("引用数量充足");
        } else if (citationCount >= 2) {
            score += 8;
            factors.add("引用数量基本可用");
        } else if (citationCount == 0) {
            score -= 10;
            factors.add("缺少引用快照");
        }

        long documentDiversity = citations == null ? 0L : citations.stream()
                .map(QaRecordCitationEntity::getDocumentId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        if (documentDiversity >= 3) {
            score += 10;
            factors.add("证据来自多个文档");
        } else if (documentDiversity == 1 && citationCount > 1) {
            factors.add("证据集中在单个文档");
        }

        boolean hasBoth = citations != null && citations.stream()
                .anyMatch(citation -> "BOTH".equalsIgnoreCase(citation.getRetrievalSource()));
        if (hasBoth) {
            score += 10;
            factors.add("包含向量和关键词共同命中的证据");
        }

        double bestScore = citations == null ? 0D : citations.stream()
                .map(QaRecordCitationEntity::getScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0D);
        if (bestScore >= 0.8D) {
            score += 10;
            factors.add("最高检索分较高");
        } else if (bestScore > 0D && bestScore < 0.45D) {
            score -= 8;
            factors.add("检索分偏低");
        }

        int bounded = Math.max(0, Math.min(100, score));
        return new QaEvidenceQualityVO(bounded, qualityLevel(bounded), factors);
    }

    private int evidenceLevelScore(String evidenceLevel, List<String> factors) {
        if (!StringUtils.hasText(evidenceLevel)) {
            factors.add("未记录证据等级");
            return 20;
        }
        return switch (evidenceLevel) {
            case "SUFFICIENT" -> {
                factors.add("证据等级充分");
                yield 60;
            }
            case "PARTIAL" -> {
                factors.add("证据等级部分支持");
                yield 42;
            }
            case "WEAK" -> {
                factors.add("证据等级较弱");
                yield 25;
            }
            case "NONE" -> {
                factors.add("无有效证据");
                yield 5;
            }
            default -> 20;
        };
    }

    private String qualityLevel(int score) {
        if (score >= 80) {
            return "HIGH";
        }
        if (score >= 55) {
            return "MEDIUM";
        }
        if (score >= 30) {
            return "LOW";
        }
        return "POOR";
    }
}
