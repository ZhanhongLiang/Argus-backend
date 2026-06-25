package com.argus.rag.document.readiness.service;

import com.argus.rag.auth.CurrentUserService;
import com.argus.rag.common.exception.BusinessException;
import com.argus.rag.document.readiness.mapper.DocumentReadinessBatchMapper;
import com.argus.rag.document.readiness.mapper.DocumentReadinessItemMapper;
import com.argus.rag.document.readiness.model.dto.ReadinessItemDecisionRequest;
import com.argus.rag.document.readiness.model.entity.DocumentReadinessBatchEntity;
import com.argus.rag.document.readiness.model.entity.DocumentReadinessItemEntity;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessBatchStatus;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessItemStatus;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessBatchVO;
import com.argus.rag.document.service.DocumentUploadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentReadinessApprovalService {
    private final DocumentReadinessBatchMapper batchMapper;
    private final DocumentReadinessItemMapper itemMapper;
    private final DocumentReadinessBatchService batchService;
    private final DocumentReadinessPrecheckService precheckService;
    private final DocumentReadinessPermissionService permissionService;
    private final DocumentUploadService documentUploadService;

    public DocumentReadinessApprovalService(DocumentReadinessBatchMapper batchMapper,
                                            DocumentReadinessItemMapper itemMapper,
                                            DocumentReadinessBatchService batchService,
                                            DocumentReadinessPrecheckService precheckService,
                                            DocumentReadinessPermissionService permissionService,
                                            DocumentUploadService documentUploadService) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.batchService = batchService;
        this.precheckService = precheckService;
        this.permissionService = permissionService;
        this.documentUploadService = documentUploadService;
    }

    @Transactional
    public void decideItem(Long itemId, ReadinessItemDecisionRequest request) {
        DocumentReadinessItemEntity item = itemMapper.selectById(requireItemId(itemId));
        if (item == null) {
            throw new BusinessException("体检条目不存在");
        }
        permissionService.requireManager(item.getGroupId());
        String decision = request == null || request.decision() == null ? "" : request.decision().trim().toUpperCase();
        DocumentReadinessItemStatus status = switch (decision) {
            case "APPROVE", "APPROVED" -> DocumentReadinessItemStatus.APPROVED;
            case "REJECT", "REJECTED" -> DocumentReadinessItemStatus.REJECTED;
            default -> throw new BusinessException("decision 非法");
        };
        CurrentUserService.CurrentUser user = permissionService.currentUser();
        item.setReadinessStatus(status.name());
        item.setDecisionReason(normalizeReason(request == null ? null : request.reason()));
        item.setApprovedByUserId(status == DocumentReadinessItemStatus.APPROVED ? user.userId() : null);
        item.setApprovedAt(status == DocumentReadinessItemStatus.APPROVED ? LocalDateTime.now() : null);
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
        precheckService.refreshBatchStats(item.getBatchId());
    }

    @Transactional
    public void approveBatch(Long batchId) {
        DocumentReadinessBatchVO batch = batchService.getReadableBatch(batchId);
        CurrentUserService.CurrentUser user = permissionService.requireManager(batch.groupId());
        updateBatch(batch.id(), DocumentReadinessBatchStatus.IMPORTING, user.userId(), LocalDateTime.now(), null);
        List<DocumentReadinessItemEntity> importableItems = itemMapper.selectImportableItems(batch.id());
        for (DocumentReadinessItemEntity item : importableItems) {
            Long targetGroupId = item.getRecommendedGroupId() == null ? item.getGroupId() : item.getRecommendedGroupId();
            Long documentId = documentUploadService.finalizeUploadedDocument(
                    targetGroupId,
                    user.userId(),
                    item.getOriginalFileName(),
                    item.getFileExt(),
                    item.getContentType(),
                    item.getFileSize(),
                    item.getFileHash(),
                    item.getStorageBucket(),
                    item.getStorageObjectKey()
            );
            item.setReadinessStatus(DocumentReadinessItemStatus.IMPORTED.name());
            item.setImportedDocumentId(documentId);
            item.setApprovedByUserId(user.userId());
            item.setApprovedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        precheckService.refreshBatchStats(batch.id());
        updateBatch(batch.id(), DocumentReadinessBatchStatus.IMPORTED, user.userId(), LocalDateTime.now(), null);
    }

    private void updateBatch(Long batchId, DocumentReadinessBatchStatus status, Long approvedByUserId,
                             LocalDateTime approvedAt, String failureReason) {
        DocumentReadinessBatchEntity update = new DocumentReadinessBatchEntity();
        update.setId(batchId);
        update.setStatus(status.name());
        update.setApprovedByUserId(approvedByUserId);
        update.setApprovedAt(approvedAt);
        update.setFailureReason(failureReason);
        update.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(update);
    }

    private Long requireItemId(Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException("itemId 非法");
        }
        return itemId;
    }

    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "";
        }
        String normalized = reason.trim();
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }
}
