package com.argus.rag.qa.controller;

import com.argus.rag.common.api.ApiResponse;
import com.argus.rag.common.log.OperationLog;
import com.argus.rag.qa.model.dto.QaRecordSearchRequest;
import com.argus.rag.qa.model.vo.QaAnalyticsDashboardVO;
import com.argus.rag.qa.model.vo.QaScopeOptionVO;
import com.argus.rag.qa.service.QaAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** QA analytics dashboard APIs for V4.2. */
@OperationLog
@RestController
@RequestMapping("/api/qa/analytics")
public class QaAnalyticsController {

    private final QaAnalyticsService qaAnalyticsService;

    public QaAnalyticsController(QaAnalyticsService qaAnalyticsService) {
        this.qaAnalyticsService = qaAnalyticsService;
    }

    @GetMapping("/scopes")
    public ApiResponse<List<QaScopeOptionVO>> scopes() {
        return ApiResponse.success(qaAnalyticsService.listScopeOptions());
    }

    @GetMapping("/dashboard")
    public ApiResponse<QaAnalyticsDashboardVO> dashboard(@ModelAttribute QaRecordSearchRequest request) {
        return ApiResponse.success(qaAnalyticsService.dashboard(request));
    }
}
