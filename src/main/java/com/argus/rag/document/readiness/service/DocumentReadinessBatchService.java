package com.argus.rag.document.readiness.service;

import com.argus.rag.auth.CurrentUserService;
import com.argus.rag.common.exception.BusinessException;
import com.argus.rag.document.readiness.mapper.DocumentReadinessBatchMapper;
import com.argus.rag.document.readiness.mapper.DocumentReadinessIssueMapper;
import com.argus.rag.document.readiness.mapper.DocumentReadinessItemMapper;
import com.argus.rag.document.readiness.model.dto.CreateReadinessBatchRequest;
import com.argus.rag.document.readiness.model.dto.ReadinessBatchQuery;
import com.argus.rag.document.readiness.model.entity.DocumentReadinessBatchEntity;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessBatchStatus;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessBatchVO;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessIssueVO;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessItemVO;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessReportVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentReadinessBatchService {
    private static final String SCORE_NOTICE = "RAG 入库适配度用于评估文档入库前质量，不代表问答正确率。";

    private final DocumentReadinessBatchMapper batchMapper;
    private final DocumentReadinessItemMapper itemMapper;
    private final DocumentReadinessIssueMapper issueMapper;
    private final DocumentReadinessPermissionService permissionService;

    public DocumentReadinessBatchService(DocumentReadinessBatchMapper batchMapper,
                                         DocumentReadinessItemMapper itemMapper,
                                         DocumentReadinessIssueMapper issueMapper,
                                         DocumentReadinessPermissionService permissionService) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.issueMapper = issueMapper;
        this.permissionService = permissionService;
    }

    @Transactional
    public Long createBatch(CreateReadinessBatchRequest request) {
        if (request == null || request.groupId() == null || request.groupId() <= 0) {
            throw new BusinessException("groupId 非法");
        }
        CurrentUserService.CurrentUser user = permissionService.requireBatchCreator(request.groupId());
        LocalDateTime now = LocalDateTime.now();
        DocumentReadinessBatchEntity batch = new DocumentReadinessBatchEntity();
        batch.setGroupId(request.groupId());
        batch.setOwnerUserId(user.userId());
        batch.setBatchName(normalizeBatchName(request.batchName()));
        batch.setStatus(DocumentReadinessBatchStatus.CREATED.name());
        batch.setTotalCount(0);
        batch.setReadyCount(0);
        batch.setWarningCount(0);
        batch.setRejectedCount(0);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        batchMapper.insert(batch);
        return batch.getId();
    }

    public List<DocumentReadinessBatchVO> listBatches(ReadinessBatchQuery query) {
        CurrentUserService.CurrentUser user = permissionService.currentUser();
        return batchMapper.selectReadableBatches(
                query == null ? new ReadinessBatchQuery() : query,
                user.userId(),
                permissionService.isSystemAdmin(user)
        );
    }

    public DocumentReadinessBatchVO getReadableBatch(Long batchId) {
        CurrentUserService.CurrentUser user = permissionService.currentUser();
        DocumentReadinessBatchVO batch = batchMapper.selectReadableBatchDetail(
                requireBatchId(batchId),
                user.userId(),
                permissionService.isSystemAdmin(user)
        );
        if (batch == null) {
            throw new BusinessException("体检批次不存在或无权访问");
        }
        return batch;
    }

    public DocumentReadinessReportVO getReport(Long batchId) {
        DocumentReadinessBatchVO batch = getReadableBatch(batchId);
        List<DocumentReadinessIssueVO> issues = issueMapper.selectByBatchId(batch.id());
        Map<Long, List<DocumentReadinessIssueVO>> issueMap = issues.stream()
                .collect(Collectors.groupingBy(DocumentReadinessIssueVO::itemId));
        List<DocumentReadinessItemVO> items = itemMapper.selectItemsByBatchId(batch.id()).stream()
                .map(item -> new DocumentReadinessItemVO(
                        item.getId(), item.getBatchId(), item.getGroupId(), item.getOriginalFileName(), item.getFileExt(),
                        item.getFileSize(), item.getParseStatus(), item.getReadinessStatus(), item.getRecommendedGroupId(),
                        item.getRecommendedAction(), item.getReadinessScore(), item.getReadinessGrade(), item.getParseable(),
                        item.getChunkCount(), item.getAvgChunkLength(), item.getShortChunkCount(), item.getLongChunkCount(),
                        item.getEmptyChunkCount(), item.getDuplicateChunkCount(), item.getDuplicateDocumentId(),
                        item.getIssueCount(), item.getAnalysisSummary(), item.getFailureReason(), item.getImportedDocumentId(),
                        item.getDecisionReason(), item.getTopicFitStatus(), item.getTopicFitScore(), item.getTopicConfidence(),
                        item.getTopicReason(), splitKeywords(item.getGroupTopicKeywords()), splitKeywords(item.getDocumentTopicKeywords()),
                        item.getKeywordOverlapScore(), item.getSemanticSimilarityScore(), splitKeywords(item.getMismatchIndicators()),
                        item.getSuggestedTargetGroupId(), item.getSuggestedTargetGroupName(), item.getCreatedAt(), item.getUpdatedAt(),
                        issueMap.getOrDefault(item.getId(), List.of())
                ))
                .toList();
        return new DocumentReadinessReportVO(batch, items, SCORE_NOTICE);
    }

    private List<String> splitKeywords(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    @Transactional
    public void cancelBatch(Long batchId) {
        DocumentReadinessBatchVO batch = getReadableBatch(batchId);
        permissionService.requireManager(batch.groupId());
        DocumentReadinessBatchEntity update = new DocumentReadinessBatchEntity();
        update.setId(batch.id());
        update.setStatus(DocumentReadinessBatchStatus.CANCELED.name());
        update.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(update);
    }

    Long requireBatchId(Long batchId) {
        if (batchId == null || batchId <= 0) {
            throw new BusinessException("batchId 非法");
        }
        return batchId;
    }

    private String normalizeBatchName(String batchName) {
        if (!StringUtils.hasText(batchName)) {
            return "入库体检批次";
        }
        String normalized = batchName.trim();
        if (normalized.length() > 128) {
            throw new BusinessException("批次名称过长");
        }
        return normalized;
    }
}
