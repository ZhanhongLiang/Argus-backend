package com.argus.rag.qa.service;

import com.argus.rag.auth.CurrentUserService;
import com.argus.rag.common.enums.SystemRole;
import com.argus.rag.common.exception.BusinessException;
import com.argus.rag.qa.mapper.QaRecordCitationMapper;
import com.argus.rag.qa.mapper.QaRecordMapper;
import com.argus.rag.qa.model.entity.QaRecordCitationEntity;
import com.argus.rag.qa.model.entity.QaRecordEntity;
import com.argus.rag.qa.model.vo.QaRecordDetailVO;
import com.argus.rag.qa.model.vo.QaRecordListItemVO;
import com.argus.rag.qa.model.vo.QaRecordPageVO;
import com.argus.rag.qa.support.EvidenceOverviewAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/** QA 闂瓟鍘嗗彶鏌ヨ鏈嶅姟锛岀粺涓€澶勭悊鏉冮檺杩囨护銆佸垎椤靛拰鍝嶅簲缁勮銆?*/
@Service
public class QaRecordQueryService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int PREVIEW_LIMIT = 120;

    private final CurrentUserService currentUserService;
    private final QaRecordMapper qaRecordMapper;
    private final QaRecordCitationMapper qaRecordCitationMapper;
    private final EvidenceOverviewAssembler evidenceOverviewAssembler;

    public QaRecordQueryService(CurrentUserService currentUserService,
                                QaRecordMapper qaRecordMapper,
                                QaRecordCitationMapper qaRecordCitationMapper,
                                EvidenceOverviewAssembler evidenceOverviewAssembler) {
        this.currentUserService = currentUserService;
        this.qaRecordMapper = qaRecordMapper;
        this.qaRecordCitationMapper = qaRecordCitationMapper;
        this.evidenceOverviewAssembler = evidenceOverviewAssembler;
    }

    /** 鍒嗛〉鏌ヨ褰撳墠鐢ㄦ埛鍙鐨?QA 鍘嗗彶璁板綍銆?*/
    public QaRecordPageVO list(Long groupId, Boolean answered, Integer page, Integer pageSize) {
        CurrentUserService.CurrentUser currentUser = currentUserService.getRequiredCurrentUser();
        boolean systemAdmin = currentUser.systemRole() == SystemRole.ADMIN;
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safePageSize = normalizePageSize(pageSize);
        long offset = (long) (safePage - 1) * safePageSize;

        List<QaRecordListItemVO> items = qaRecordMapper.selectVisibleRecords(
                currentUser.userId(),
                systemAdmin,
                groupId,
                answered,
                offset,
                safePageSize
        ).stream().map(this::toListItem).toList();
        Long total = qaRecordMapper.countVisibleRecords(
                currentUser.userId(),
                systemAdmin,
                groupId,
                answered);
        return new QaRecordPageVO(items, total == null ? 0L : total, safePage, safePageSize);
    }

    /** 鏌ヨ鍗曟潯 QA 璁板綍璇︽儏锛屽苟闄勫甫鍥炵瓟鏃朵繚瀛樼殑寮曠敤蹇収銆?*/
    public QaRecordDetailVO getDetail(Long recordId) {
        if (recordId == null || recordId <= 0) {
            throw new BusinessException("QA record id is invalid");
        }
        CurrentUserService.CurrentUser currentUser = currentUserService.getRequiredCurrentUser();
        boolean systemAdmin = currentUser.systemRole() == SystemRole.ADMIN;
        QaRecordEntity record = qaRecordMapper.selectVisibleRecord(recordId, currentUser.userId(), systemAdmin);
        if (record == null) {
            throw new BusinessException("QA record does not exist or is not visible");
        }
        List<QaRecordDetailVO.Citation> citations = qaRecordCitationMapper.selectByRecordId(recordId)
                .stream()
                .map(this::toCitation)
                .toList();
        return toDetail(record, citations);
    }

    /** Soft-deletes a visible QA history record while retaining citation snapshots for governance audit. */
    @Transactional
    public void delete(Long recordId) {
        if (recordId == null || recordId <= 0) {
            throw new BusinessException("QA record id is invalid");
        }
        CurrentUserService.CurrentUser currentUser = currentUserService.getRequiredCurrentUser();
        boolean systemAdmin = currentUser.systemRole() == SystemRole.ADMIN;
        QaRecordEntity record = qaRecordMapper.selectVisibleRecord(recordId, currentUser.userId(), systemAdmin);
        if (record == null) {
            throw new BusinessException("QA record does not exist or is not visible");
        }
        record.setDeletedAt(LocalDateTime.now());
        record.setDeletedBy(currentUser.userId());
        record.setDeleteReason("QA history delete");
        qaRecordMapper.updateById(record);
    }

    /** 瑙勮寖鍒嗛〉澶у皬锛岄伩鍏嶄竴娆¤姹傛媺鍙栬繃澶氬巻鍙茶褰曘€?*/
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /** 灏嗘暟鎹簱瀹炰綋杞崲涓哄垪琛ㄩ〉灞曠ず椤广€?*/
    private QaRecordListItemVO toListItem(QaRecordEntity record) {
        return new QaRecordListItemVO(
                record.getId(),
                record.getUserId(),
                record.getGroupId(),
                record.getQuestion(),
                record.getAnswered(),
                preview(record),
                record.getReasonCode(),
                record.getEvidenceLevel(),
                record.getCitationCount(),
                record.getLatencyMs(),
                record.getCreatedAt()
        );
    }

    /** 灏嗘暟鎹簱瀹炰綋鍜屽紩鐢ㄥ揩鐓х粍鍚堜负璇︽儏鍝嶅簲銆?*/
    private QaRecordDetailVO toDetail(QaRecordEntity record, List<QaRecordDetailVO.Citation> citations) {
        return new QaRecordDetailVO(
                record.getId(),
                record.getUserId(),
                record.getGroupId(),
                record.getQuestion(),
                record.getAnswer(),
                record.getAnswered(),
                record.getReasonCode(),
                record.getReasonMessage(),
                record.getEvidenceLevel(),
                record.getCitationCount(),
                record.getPromptTokens(),
                record.getCompletionTokens(),
                record.getTotalTokens(),
                record.getIsEstimated(),
                record.getLatencyMs(),
                record.getModelName(),
                record.getEndpoint(),
                record.getSuccess(),
                record.getErrorMessage(),
                record.getCreatedAt(),
                evidenceOverviewAssembler.assembleHistoryCitations(citations),
                citations
        );
    }

    /** 灏嗗紩鐢ㄥ揩鐓у疄浣撹浆鎹负璇︽儏椤靛紩鐢ㄩ」銆?*/
    private QaRecordDetailVO.Citation toCitation(QaRecordCitationEntity entity) {
        return new QaRecordDetailVO.Citation(
                entity.getDocumentId(),
                entity.getDocumentVersionId(),
                entity.getChunkId(),
                entity.getChunkIndex(),
                entity.getStartChunkIndex(),
                entity.getEndChunkIndex(),
                entity.getFileName(),
                entity.getScore(),
                entity.getRetrievalSource(),
                entity.getVectorScore(),
                entity.getKeywordScore(),
                entity.getHybridScore(),
                entity.getSnippet()
        );
    }

    /** 鐢熸垚鍒楄〃椤靛洖绛旈瑙堬紝浼樺厛鍙栧洖绛旀鏂囷紝鍏舵鍙栨嫆绛斿師鍥犮€?*/
    private String preview(QaRecordEntity record) {
        String source = StringUtils.hasText(record.getAnswer()) ? record.getAnswer() : record.getReasonMessage();
        if (!StringUtils.hasText(source)) {
            return "";
        }
        String normalized = source.replaceAll("\\s+", " ").trim();
        return normalized.length() <= PREVIEW_LIMIT ? normalized : normalized.substring(0, PREVIEW_LIMIT);
    }
}
