package com.argus.rag.document.readiness.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("document_readiness_items")
public class DocumentReadinessItemEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long groupId;
    private String originalFileName;
    private String fileExt;
    private Long fileSize;
    private String contentType;
    private String fileHash;
    private String storageBucket;
    private String storageObjectKey;
    private String parseStatus;
    private String readinessStatus;
    private Long recommendedGroupId;
    private String recommendedAction;
    private BigDecimal readinessScore;
    private String readinessGrade;
    private Boolean parseable;
    private Integer chunkCount;
    private Integer avgChunkLength;
    private Integer shortChunkCount;
    private Integer longChunkCount;
    private Integer emptyChunkCount;
    private Integer duplicateChunkCount;
    private Long duplicateDocumentId;
    private Integer issueCount;
    private String analysisSummary;
    private String failureReason;
    private Long approvedByUserId;
    private LocalDateTime approvedAt;
    private Long importedDocumentId;
    private String decisionReason;
    private String topicFitStatus;
    private BigDecimal topicFitScore;
    private BigDecimal topicConfidence;
    private String topicReason;
    private String groupTopicKeywords;
    private String documentTopicKeywords;
    private BigDecimal keywordOverlapScore;
    private BigDecimal semanticSimilarityScore;
    private String mismatchIndicators;
    private Long suggestedTargetGroupId;
    private String suggestedTargetGroupName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
