package com.argus.rag.document.readiness.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("document_readiness_batches")
public class DocumentReadinessBatchEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long ownerUserId;
    private String batchName;
    private String status;
    private Integer totalCount;
    private Integer readyCount;
    private Integer warningCount;
    private Integer rejectedCount;
    private BigDecimal avgReadinessScore;
    private String failureReason;
    private Long approvedByUserId;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
