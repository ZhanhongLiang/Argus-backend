package com.argus.rag.qa.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Batch cleanup request for QA records. */
public record QaRecordCleanupRequest(
        String scope,
        Long groupId,
        @NotEmpty List<Long> recordIds,
        @Size(max = 255) String deleteReason
) {
}
