package com.argus.rag.qa.model.vo;

import java.util.List;

/** Evidence support quality score derived from saved QA record signals. */
public record QaEvidenceQualityVO(
        int score,
        String level,
        List<String> factors
) {
}
