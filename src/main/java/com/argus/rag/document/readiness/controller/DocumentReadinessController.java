package com.argus.rag.document.readiness.controller;

import com.argus.rag.common.api.ApiResponse;
import com.argus.rag.document.readiness.model.dto.AddReadinessItemRequest;
import com.argus.rag.document.readiness.model.dto.CreateReadinessBatchRequest;
import com.argus.rag.document.readiness.model.dto.ReadinessBatchQuery;
import com.argus.rag.document.readiness.model.dto.ReadinessItemDecisionRequest;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessBatchVO;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessReportVO;
import com.argus.rag.document.readiness.service.DocumentReadinessApprovalService;
import com.argus.rag.document.readiness.service.DocumentReadinessBatchService;
import com.argus.rag.document.readiness.service.DocumentReadinessPrecheckService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents/readiness")
public class DocumentReadinessController {
    private final DocumentReadinessBatchService batchService;
    private final DocumentReadinessPrecheckService precheckService;
    private final DocumentReadinessApprovalService approvalService;

    public DocumentReadinessController(DocumentReadinessBatchService batchService,
                                       DocumentReadinessPrecheckService precheckService,
                                       DocumentReadinessApprovalService approvalService) {
        this.batchService = batchService;
        this.precheckService = precheckService;
        this.approvalService = approvalService;
    }

    @PostMapping("/batches")
    public ApiResponse<Long> createBatch(@RequestBody CreateReadinessBatchRequest request) {
        return ApiResponse.success(batchService.createBatch(request));
    }

    @GetMapping("/batches")
    public ApiResponse<List<DocumentReadinessBatchVO>> listBatches(@ModelAttribute ReadinessBatchQuery query) {
        return ApiResponse.success(batchService.listBatches(query));
    }

    @GetMapping("/batches/{batchId}")
    public ApiResponse<DocumentReadinessReportVO> getBatch(@PathVariable Long batchId) {
        return ApiResponse.success(batchService.getReport(batchId));
    }

    @PostMapping(path = "/batches/{batchId}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> addItem(@PathVariable Long batchId, @ModelAttribute AddReadinessItemRequest request) {
        return ApiResponse.success(precheckService.addItem(batchId, request));
    }

    @PostMapping("/batches/{batchId}/analyze")
    public ApiResponse<Void> analyzeBatch(@PathVariable Long batchId) {
        precheckService.analyzeBatch(batchId);
        return ApiResponse.success(null);
    }

    @GetMapping("/batches/{batchId}/report")
    public ApiResponse<DocumentReadinessReportVO> getReport(@PathVariable Long batchId) {
        return ApiResponse.success(batchService.getReport(batchId));
    }

    @PostMapping("/batches/{batchId}/approve")
    public ApiResponse<Void> approveBatch(@PathVariable Long batchId) {
        approvalService.approveBatch(batchId);
        return ApiResponse.success(null);
    }

    @PostMapping("/items/{itemId}/decision")
    public ApiResponse<Void> decideItem(@PathVariable Long itemId, @RequestBody ReadinessItemDecisionRequest request) {
        approvalService.decideItem(itemId, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/batches/{batchId}")
    public ApiResponse<Void> cancelBatch(@PathVariable Long batchId) {
        batchService.cancelBatch(batchId);
        return ApiResponse.success(null);
    }
}
