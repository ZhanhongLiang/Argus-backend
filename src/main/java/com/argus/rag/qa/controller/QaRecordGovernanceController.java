package com.argus.rag.qa.controller;

import com.argus.rag.common.api.ApiResponse;
import com.argus.rag.common.log.OperationLog;
import com.argus.rag.qa.model.dto.QaRecordCleanupRequest;
import com.argus.rag.qa.model.dto.QaRecordSearchRequest;
import com.argus.rag.qa.model.vo.QaCleanupPreviewVO;
import com.argus.rag.qa.model.vo.QaCleanupResultVO;
import com.argus.rag.qa.model.vo.QaRecordGovernancePageVO;
import com.argus.rag.qa.service.QaRecordGovernanceService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** QA record governance APIs for V4.2. */
@OperationLog
@RestController
@RequestMapping("/api/qa/record-governance")
public class QaRecordGovernanceController {

    private final QaRecordGovernanceService qaRecordGovernanceService;

    public QaRecordGovernanceController(QaRecordGovernanceService qaRecordGovernanceService) {
        this.qaRecordGovernanceService = qaRecordGovernanceService;
    }

    @GetMapping("/search")
    public ApiResponse<QaRecordGovernancePageVO> search(@ModelAttribute QaRecordSearchRequest request) {
        return ApiResponse.success(qaRecordGovernanceService.search(request));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute QaRecordSearchRequest request,
                                         @RequestParam(defaultValue = "json") String format) {
        boolean csv = "csv".equalsIgnoreCase(format);
        byte[] body = qaRecordGovernanceService.export(request, format);
        String filename = "qa-records-" + LocalDate.now() + (csv ? ".csv" : ".json");
        MediaType mediaType = csv ? new MediaType("text", "csv", StandardCharsets.UTF_8) : MediaType.APPLICATION_JSON;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(body);
    }

    @PostMapping("/cleanup/preview")
    public ApiResponse<QaCleanupPreviewVO> preview(@Valid @RequestBody QaRecordCleanupRequest request) {
        return ApiResponse.success(qaRecordGovernanceService.preview(request));
    }

    @PostMapping("/cleanup/soft-delete")
    public ApiResponse<QaCleanupResultVO> softDelete(@Valid @RequestBody QaRecordCleanupRequest request) {
        return ApiResponse.success(qaRecordGovernanceService.softDelete(request));
    }
}
