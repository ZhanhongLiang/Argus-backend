package com.argus.rag.document.readiness.topic;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TopicKeywordExtractor {
    private static final Pattern LATIN_TOKEN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_+#.-]{1,30}");
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "this", "that", "from", "are", "was", "were",
            "知识库", "资料库", "文档", "文件", "问题", "常见", "说明", "介绍", "指南", "内容"
    );
    private static final List<String> DOMAIN_TERMS = List.of(
            "简历", "求职", "项目经历", "技术栈", "实习经历", "实习", "论文成果", "论文", "教育背景", "教育", "工作经历",
            "面试", "岗位", "学历", "证书", "Java", "Spring", "Vue", "前端", "后端", "开发经验",
            "陈皮", "新会陈皮", "新会", "广东特产", "功效", "冲泡方式", "冲泡", "茶饮", "食品", "养生", "商品",
            "售后", "产地", "柑皮", "保存", "选购", "客户问答", "店铺", "产品"
    );

    public List<String> extract(String text, int limit) {
        if (!StringUtils.hasText(text) || limit <= 0) {
            return List.of();
        }
        Map<String, Integer> weights = new LinkedHashMap<>();
        String normalized = text.trim();
        for (String term : DOMAIN_TERMS) {
            int count = countOccurrences(normalized, term);
            if (count > 0) {
                weights.merge(term, count * 8 + Math.min(term.length(), 6), Integer::sum);
            }
        }
        Matcher matcher = LATIN_TOKEN.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group();
            String canonical = canonical(token);
            if (!STOPWORDS.contains(canonical)) {
                weights.merge(token, 2, Integer::sum);
            }
        }
        addCjkBigrams(normalized, weights);
        return weights.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()))
                .filter(entry -> !STOPWORDS.contains(entry.getKey()))
                .sorted((left, right) -> {
                    int score = Integer.compare(right.getValue(), left.getValue());
                    return score != 0 ? score : left.getKey().compareTo(right.getKey());
                })
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    public boolean hasResumeDomain(List<String> keywords) {
        return containsAny(keywords, Set.of("简历", "求职", "项目经历", "技术栈", "实习经历", "实习", "论文成果", "教育背景", "面试", "岗位", "学历"));
    }

    public boolean hasChenpiDomain(List<String> keywords) {
        return containsAny(keywords, Set.of("陈皮", "新会陈皮", "新会", "广东特产", "功效", "冲泡", "冲泡方式", "茶饮", "食品", "养生", "商品", "售后", "产地", "柑皮"));
    }

    private boolean containsAny(List<String> keywords, Set<String> domainTerms) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (domainTerms.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int countOccurrences(String text, String term) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) >= 0) {
            count++;
            index += term.length();
        }
        return count;
    }

    private void addCjkBigrams(String text, Map<String, Integer> weights) {
        String cjkOnly = text.replaceAll("[^\\p{IsHan}]", "");
        if (cjkOnly.length() < 2) {
            return;
        }
        int max = Math.min(cjkOnly.length() - 1, 3000);
        for (int i = 0; i < max; i++) {
            String token = cjkOnly.substring(i, i + 2);
            if (!STOPWORDS.contains(token)) {
                weights.merge(token, 1, Integer::sum);
            }
        }
    }

    private String canonical(String token) {
        return token == null ? "" : token.toLowerCase(Locale.ROOT);
    }

    public String joinKeywords(List<String> keywords) {
        return keywords == null || keywords.isEmpty() ? "" : String.join(",", new ArrayList<>(keywords));
    }
}
