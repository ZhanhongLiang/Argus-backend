package com.argus.rag.qa.service;

import com.argus.rag.common.exception.BusinessException;
import com.argus.rag.qa.mapper.QaAnalyticsMapper;
import com.argus.rag.qa.mapper.QaRecordCitationMapper;
import com.argus.rag.qa.model.dto.QaRecordCleanupRequest;
import com.argus.rag.qa.model.dto.QaRecordSearchRequest;
import com.argus.rag.qa.model.entity.QaRecordCitationEntity;
import com.argus.rag.qa.model.entity.QaRecordEntity;
import com.argus.rag.qa.model.vo.QaCleanupPreviewVO;
import com.argus.rag.qa.model.vo.QaCleanupResultVO;
import com.argus.rag.qa.model.vo.QaRecordGovernanceItemVO;
import com.argus.rag.qa.model.vo.QaRecordGovernancePageVO;
import com.argus.rag.qa.support.LlmCostCalculator;
import com.argus.rag.qa.support.QaEvidenceQualityScorer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Search, export, preview and cleanup for persisted QA records. */
@Service
public class QaRecordGovernanceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EXPORT_SIZE = 5000;
    private static final int MAX_BATCH_DELETE = 500;

    private final QaRecordScopeResolver scopeResolver;
    private final QaAnalyticsMapper qaAnalyticsMapper;
    private final QaRecordCitationMapper qaRecordCitationMapper;
    private final QaEvidenceQualityScorer evidenceQualityScorer;
    private final LlmCostCalculator costCalculator;
    private final ObjectMapper objectMapper;

    public QaRecordGovernanceService(QaRecordScopeResolver scopeResolver,
                                      QaAnalyticsMapper qaAnalyticsMapper,
                                      QaRecordCitationMapper qaRecordCitationMapper,
                                      QaEvidenceQualityScorer evidenceQualityScorer,
                                      LlmCostCalculator costCalculator,
                                      ObjectMapper objectMapper) {
        this.scopeResolver = scopeResolver;
        this.qaAnalyticsMapper = qaAnalyticsMapper;
        this.qaRecordCitationMapper = qaRecordCitationMapper;
        this.evidenceQualityScorer = evidenceQualityScorer;
        this.costCalculator = costCalculator;
        this.objectMapper = objectMapper;
    }

    public QaRecordGovernancePageVO search(QaRecordSearchRequest request) {
        QaRecordScopeResolver.ScopeContext scopeContext = scopeResolver.resolve(request.getScope(), request.getGroupId());
        int page = normalizePage(request.getPage());
        int pageSize = normalizePageSize(request.getPageSize());
        long offset = (long) (page - 1) * pageSize;
        List<QaRecordGovernanceItemVO> items = qaAnalyticsMapper.selectSearchRecords(scopeContext, request, offset, pageSize)
                .stream()
                .map(this::toItem)
                .toList();
        Long total = qaAnalyticsMapper.countSearchRecords(scopeContext, request);
        return new QaRecordGovernancePageVO(items, total == null ? 0L : total, page, pageSize);
    }

    public byte[] export(QaRecordSearchRequest request, String format) {
        QaRecordScopeResolver.ScopeContext scopeContext = scopeResolver.resolve(request.getScope(), request.getGroupId());
        List<QaRecordGovernanceItemVO> items = qaAnalyticsMapper.selectSearchRecords(scopeContext, request, 0, MAX_EXPORT_SIZE)
                .stream()
                .map(this::toItem)
                .toList();
        if ("csv".equalsIgnoreCase(format)) {
            return toCsv(items).getBytes(StandardCharsets.UTF_8);
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(items);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("QA record export failed");
        }
    }

    public QaCleanupPreviewVO preview(QaRecordCleanupRequest request) {
        validateRecordIds(request.recordIds());
        QaRecordScopeResolver.ScopeContext scopeContext = scopeResolver.resolve(request.scope(), request.groupId());
        List<QaRecordGovernanceItemVO> samples = qaAnalyticsMapper.selectRecordsByIds(scopeContext, request.recordIds(), 20)
                .stream()
                .map(this::toItem)
                .toList();
        return new QaCleanupPreviewVO(request.recordIds().size(), samples.size(), samples);
    }

    @Transactional
    public QaCleanupResultVO softDelete(QaRecordCleanupRequest request) {
        validateRecordIds(request.recordIds());
        QaRecordScopeResolver.ScopeContext scopeContext = scopeResolver.resolve(request.scope(), request.groupId());
        int deleted = qaAnalyticsMapper.softDeleteByIds(scopeContext, request.recordIds(), normalizeReason(request.deleteReason()));
        return new QaCleanupResultVO(deleted);
    }

    private QaRecordGovernanceItemVO toItem(QaRecordEntity record) {
        List<QaRecordCitationEntity> citations = qaRecordCitationMapper.selectByRecordId(record.getId());
        BigDecimal estimatedCost = costCalculator.estimate(record.getModelName(), record.getPromptTokens(), record.getCompletionTokens());
        return new QaRecordGovernanceItemVO(
                record.getId(),
                record.getUserId(),
                record.getGroupId(),
                record.getQuestion(),
                preview(record),
                record.getAnswered(),
                record.getSuccess(),
                record.getReasonCode(),
                record.getEvidenceLevel(),
                record.getCitationCount(),
                record.getPromptTokens(),
                record.getCompletionTokens(),
                record.getTotalTokens(),
                record.getIsEstimated(),
                estimatedCost,
                record.getLatencyMs(),
                record.getModelName(),
                record.getEndpoint(),
                record.getErrorMessage(),
                record.getCreatedAt(),
                evidenceQualityScorer.score(record, citations)
        );
    }

    private String toCsv(List<QaRecordGovernanceItemVO> items) {
        StringBuilder csv = new StringBuilder("id,userId,groupId,question,answered,success,reasonCode,evidenceLevel,citationCount,promptTokens,completionTokens,totalTokens,isEstimated,estimatedCost,latencyMs,modelName,endpoint,createdAt,evidenceScore,evidenceQuality\n");
        for (QaRecordGovernanceItemVO item : items) {
            csv.append(item.id()).append(',')
                    .append(item.userId()).append(',')
                    .append(item.groupId()).append(',')
                    .append(escape(item.question())).append(',')
                    .append(item.answered()).append(',')
                    .append(item.success()).append(',')
                    .append(escape(item.reasonCode())).append(',')
                    .append(escape(item.evidenceLevel())).append(',')
                    .append(item.citationCount()).append(',')
                    .append(item.promptTokens()).append(',')
                    .append(item.completionTokens()).append(',')
                    .append(item.totalTokens()).append(',')
                    .append(item.isEstimated()).append(',')
                    .append(item.estimatedCost()).append(',')
                    .append(item.latencyMs()).append(',')
                    .append(escape(item.modelName())).append(',')
                    .append(escape(item.endpoint())).append(',')
                    .append(item.createdAt()).append(',')
                    .append(item.evidenceQuality().score()).append(',')
                    .append(escape(item.evidenceQuality().level())).append('\n');
        }
        return csv.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
        return "\"" + escaped + "\"";
    }

    private String preview(QaRecordEntity record) {
        String source = StringUtils.hasText(record.getAnswer()) ? record.getAnswer() : record.getReasonMessage();
        if (!StringUtils.hasText(source)) {
            return "";
        }
        String normalized = source.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private void validateRecordIds(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            throw new BusinessException("QA record ids are required");
        }
        if (recordIds.size() > MAX_BATCH_DELETE) {
            throw new BusinessException("Too many QA records in one cleanup request");
        }
        if (recordIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException("QA record id is invalid");
        }
    }

    private String normalizeReason(String reason) {
        return StringUtils.hasText(reason) ? reason.trim() : "V4.2 QA record cleanup";
    }
}
