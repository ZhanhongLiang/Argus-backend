package com.argus.rag.document.readiness.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_readiness_issues")
public class DocumentReadinessIssueEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long itemId;
    private String issueType;
    private String severity;
    private String message;
    private String suggestion;
    private LocalDateTime createdAt;
}
