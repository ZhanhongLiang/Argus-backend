package com.argus.rag.qa.model.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** Permission-aware QA record search and analytics filter. */
@Data
public class QaRecordSearchRequest {

    private String scope = "SELF";
    private Long groupId;
    private Long userId;
    private String keyword;
    private Boolean answered;
    private Boolean success;
    private String evidenceLevel;
    private String modelName;
    private String endpoint;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;
    private Integer page = 1;
    private Integer pageSize = 20;
}
