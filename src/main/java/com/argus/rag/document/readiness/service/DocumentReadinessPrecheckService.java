package com.argus.rag.document.readiness.service;

import com.argus.rag.auth.CurrentUserService;
import com.argus.rag.common.exception.BusinessException;
import com.argus.rag.document.model.entity.DocumentEntity;
import com.argus.rag.document.readiness.mapper.DocumentReadinessBatchMapper;
import com.argus.rag.document.readiness.mapper.DocumentReadinessIssueMapper;
import com.argus.rag.document.readiness.mapper.DocumentReadinessItemMapper;
import com.argus.rag.document.readiness.model.dto.AddReadinessItemRequest;
import com.argus.rag.document.readiness.model.entity.DocumentReadinessBatchEntity;
import com.argus.rag.document.readiness.model.entity.DocumentReadinessIssueEntity;
import com.argus.rag.document.readiness.model.entity.DocumentReadinessItemEntity;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessAction;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessBatchStatus;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessGrade;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessIssueSeverity;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessIssueType;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessItemStatus;
import com.argus.rag.document.readiness.model.enums.DocumentReadinessParseStatus;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessBatchVO;
import com.argus.rag.document.readiness.support.DocumentReadinessScoreCalculator;
import com.argus.rag.document.readiness.support.DryRunChunkAnalyzer;
import com.argus.rag.document.readiness.support.DuplicateDocumentDetector;
import com.argus.rag.document.readiness.support.TopicSuitabilityAnalyzer;
import com.argus.rag.document.readiness.topic.SemanticTopicSuitabilityAnalyzer;
import com.argus.rag.document.readiness.topic.TopicSuitabilityResult;
import com.argus.rag.document.readiness.topic.TopicSuitabilityStatus;
import com.argus.rag.engine.storage.ObjectStorageService;
import com.argus.rag.ingestion.service.pipeline.parser.DocumentParserFactory;
import com.argus.rag.ingestion.service.pipeline.reader.StoredObjectDocumentReader;
import com.argus.rag.ingestion.service.pipeline.transformer.StructureAwareChunkTransformer;
import com.argus.rag.ingestion.service.pipeline.transformer.TextCleanupTransformer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentReadinessPrecheckService {
    private static final long MAX_FILE_SIZE = 256L * 1024 * 1024;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "md", "pdf", "docx");

    private final DocumentReadinessBatchMapper batchMapper;
    private final DocumentReadinessItemMapper itemMapper;
    private final DocumentReadinessIssueMapper issueMapper;
    private final DocumentReadinessBatchService batchService;
    private final DocumentReadinessPermissionService permissionService;
    private final ObjectStorageService objectStorageService;
    private final DocumentParserFactory parserFactory;
    private final StructureAwareChunkTransformer chunkTransformer;
    private final DryRunChunkAnalyzer chunkAnalyzer;
    private final DuplicateDocumentDetector duplicateDetector;
    private final TopicSuitabilityAnalyzer topicSuitabilityAnalyzer;
    private final SemanticTopicSuitabilityAnalyzer semanticTopicSuitabilityAnalyzer;
    private final DocumentReadinessScoreCalculator scoreCalculator;
    private final DocumentReadinessRecommendationService recommendationService;

    public DocumentReadinessPrecheckService(DocumentReadinessBatchMapper batchMapper,
                                            DocumentReadinessItemMapper itemMapper,
                                            DocumentReadinessIssueMapper issueMapper,
                                            DocumentReadinessBatchService batchService,
                                            DocumentReadinessPermissionService permissionService,
                                            ObjectStorageService objectStorageService,
                                            DocumentParserFactory parserFactory,
                                            StructureAwareChunkTransformer chunkTransformer,
                                            DryRunChunkAnalyzer chunkAnalyzer,
                                            DuplicateDocumentDetector duplicateDetector,
                                            TopicSuitabilityAnalyzer topicSuitabilityAnalyzer,
                                            SemanticTopicSuitabilityAnalyzer semanticTopicSuitabilityAnalyzer,
                                            DocumentReadinessScoreCalculator scoreCalculator,
                                            DocumentReadinessRecommendationService recommendationService) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.issueMapper = issueMapper;
        this.batchService = batchService;
        this.permissionService = permissionService;
        this.objectStorageService = objectStorageService;
        this.parserFactory = parserFactory;
        this.chunkTransformer = chunkTransformer;
        this.chunkAnalyzer = chunkAnalyzer;
        this.duplicateDetector = duplicateDetector;
        this.topicSuitabilityAnalyzer = topicSuitabilityAnalyzer;
        this.semanticTopicSuitabilityAnalyzer = semanticTopicSuitabilityAnalyzer;
        this.scoreCalculator = scoreCalculator;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public Long addItem(Long batchId, AddReadinessItemRequest request) {
        DocumentReadinessBatchVO batch = batchService.getReadableBatch(batchId);
        CurrentUserService.CurrentUser user = permissionService.requireMemberUploader(batch.groupId());
        MultipartFile file = requireFile(request);
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String fileExt = extractFileExt(fileName);
        String bucket = objectStorageService.getDefaultBucket();
        String objectKey = "readiness/%d/%d/%s.%s".formatted(
                batch.groupId(), batch.id(), UUID.randomUUID().toString().replace("-", ""), fileExt);
        String fileHash = calculateSha256(file);
        try {
            objectStorageService.putObject(bucket, objectKey, file.getInputStream(), file.getSize(),
                    normalizeContentType(file.getContentType()));
        } catch (IOException exception) {
            throw new BusinessException("读取候选文件失败");
        }

        LocalDateTime now = LocalDateTime.now();
        DocumentReadinessItemEntity item = new DocumentReadinessItemEntity();
        item.setBatchId(batch.id());
        item.setGroupId(batch.groupId());
        item.setOriginalFileName(fileName);
        item.setFileExt(fileExt);
        item.setFileSize(file.getSize());
        item.setContentType(normalizeContentType(file.getContentType()));
        item.setFileHash(fileHash);
        item.setStorageBucket(bucket);
        item.setStorageObjectKey(objectKey);
        item.setParseStatus(SUPPORTED_EXTENSIONS.contains(fileExt)
                ? DocumentReadinessParseStatus.PENDING.name()
                : DocumentReadinessParseStatus.UNSUPPORTED.name());
        item.setReadinessStatus(DocumentReadinessItemStatus.PENDING.name());
        item.setRecommendedGroupId(batch.groupId());
        item.setParseable(false);
        item.setChunkCount(0);
        item.setAvgChunkLength(0);
        item.setShortChunkCount(0);
        item.setLongChunkCount(0);
        item.setEmptyChunkCount(0);
        item.setDuplicateChunkCount(0);
        item.setIssueCount(0);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        itemMapper.insert(item);
        refreshBatchStats(batch.id());
        return item.getId();
    }

    @Transactional
    public void analyzeBatch(Long batchId) {
        DocumentReadinessBatchVO batch = batchService.getReadableBatch(batchId);
        permissionService.requireManager(batch.groupId());
        updateBatchStatus(batch.id(), DocumentReadinessBatchStatus.ANALYZING, null);
        List<DocumentReadinessItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<DocumentReadinessItemEntity>()
                .eq(DocumentReadinessItemEntity::getBatchId, batch.id())
                .orderByAsc(DocumentReadinessItemEntity::getId));
        for (DocumentReadinessItemEntity item : items) {
            analyzeItem(item);
        }
        refreshBatchStats(batch.id());
        updateBatchStatus(batch.id(), DocumentReadinessBatchStatus.READY_FOR_REVIEW, null);
    }

    private void analyzeItem(DocumentReadinessItemEntity item) {
        LocalDateTime now = LocalDateTime.now();
        issueMapper.delete(new LambdaQueryWrapper<DocumentReadinessIssueEntity>()
                .eq(DocumentReadinessIssueEntity::getItemId, item.getId()));
        item.setReadinessStatus(DocumentReadinessItemStatus.ANALYZING.name());
        item.setUpdatedAt(now);
        itemMapper.updateById(item);

        List<DocumentReadinessIssueEntity> issues = new ArrayList<>();
        try {
            if (!SUPPORTED_EXTENSIONS.contains(item.getFileExt())) {
                issues.add(issue(item, DocumentReadinessIssueType.UNSUPPORTED_EXTENSION,
                        DocumentReadinessIssueSeverity.BLOCKER, "暂不支持该文件类型", "请上传 txt、md、pdf 或 docx 文件"));
                finishFailedItem(item, issues, DocumentReadinessParseStatus.UNSUPPORTED, "文件类型不支持");
                return;
            }

            List<Document> parsed = readAndClean(item);
            String text = parsed.isEmpty() ? "" : parsed.getFirst().getText();
            List<Document> chunks = chunkTransformer.apply(parsed);
            DryRunChunkAnalyzer.Metrics metrics = chunkAnalyzer.analyze(chunks);
            DocumentEntity duplicate = duplicateDetector.findDuplicate(item.getGroupId(), item.getFileHash());

            addTextIssues(item, text, metrics, duplicate, issues);
            if (topicSuitabilityAnalyzer.lowSuitability(item.getOriginalFileName(), text)) {
                issues.add(issue(item, DocumentReadinessIssueType.LOW_TOPIC_SUITABILITY,
                        DocumentReadinessIssueSeverity.WARNING, "文档主题可能不适合当前知识库", "建议 OWNER 人工确认目标知识库"));
            }
            TopicSuitabilityResult topicResult = semanticTopicSuitabilityAnalyzer.analyze(
                    item.getGroupId(), item.getId(), item.getOriginalFileName(), text);
            applyTopicResult(item, topicResult, issues);
            persistIssues(issues);

            DocumentReadinessScoreCalculator.ScoreResult score = scoreCalculator.calculate(
                    true, text.length(), metrics, duplicate != null, issues);
            DocumentReadinessGrade grade = score.grade();
            BigDecimal readinessScore = score.score();
            DocumentReadinessAction action = recommendationService.recommend(grade, issues);
            if (topicResult.status() == TopicSuitabilityStatus.MISMATCH) {
                grade = DocumentReadinessGrade.NOT_RECOMMENDED;
                readinessScore = minScore(readinessScore, BigDecimal.valueOf(40));
                action = DocumentReadinessAction.REJECT;
            } else if (topicResult.status() == TopicSuitabilityStatus.WARNING || topicResult.status() == TopicSuitabilityStatus.UNKNOWN) {
                grade = DocumentReadinessGrade.NEEDS_REVIEW;
                readinessScore = minScore(readinessScore, BigDecimal.valueOf(65));
                if (action == DocumentReadinessAction.IMPORT_TO_TARGET_GROUP || action == DocumentReadinessAction.IMPORT_TO_RECOMMENDED_GROUP) {
                    action = DocumentReadinessAction.CLEAN_BEFORE_IMPORT;
                }
            }
            item.setParseStatus(DocumentReadinessParseStatus.SUCCESS.name());
            item.setReadinessStatus(resolveStatus(grade, action).name());
            item.setRecommendedAction(action.name());
            item.setReadinessScore(readinessScore);
            item.setReadinessGrade(grade.name());
            item.setParseable(true);
            item.setChunkCount(metrics.chunkCount());
            item.setAvgChunkLength(metrics.avgChunkLength());
            item.setShortChunkCount(metrics.shortChunkCount());
            item.setLongChunkCount(metrics.longChunkCount());
            item.setEmptyChunkCount(metrics.emptyChunkCount());
            item.setDuplicateChunkCount(metrics.duplicateChunkCount());
            item.setDuplicateDocumentId(duplicate == null ? null : duplicate.getId());
            item.setIssueCount(issues.size());
            item.setAnalysisSummary("dry-run 已完成：解析文本 %d 字符，切片 %d 个。".formatted(text.length(), metrics.chunkCount()));
            item.setFailureReason(null);
            item.setUpdatedAt(LocalDateTime.now());
            itemMapper.updateById(item);
        } catch (RuntimeException exception) {
            issues.add(issue(item, DocumentReadinessIssueType.PARSE_FAILED,
                    DocumentReadinessIssueSeverity.BLOCKER, "文档解析失败", "请检查文件是否损坏或转换为支持格式后重试"));
            finishFailedItem(item, issues, DocumentReadinessParseStatus.FAILED, exception.getMessage());
        }
    }

    private void applyTopicResult(DocumentReadinessItemEntity item, TopicSuitabilityResult result,
                                  List<DocumentReadinessIssueEntity> issues) {
        item.setTopicFitStatus(result.status().name());
        item.setTopicFitScore(result.finalTopicFitScore());
        item.setTopicConfidence(result.confidence());
        item.setTopicReason(result.reason());
        item.setGroupTopicKeywords(joinValues(result.groupTopicKeywords()));
        item.setDocumentTopicKeywords(joinValues(result.documentTopicKeywords()));
        item.setKeywordOverlapScore(result.keywordOverlapScore());
        item.setSemanticSimilarityScore(result.semanticSimilarityScore());
        item.setMismatchIndicators(joinValues(result.mismatchIndicators()));
        item.setSuggestedTargetGroupId(result.suggestedTargetGroupId());
        item.setSuggestedTargetGroupName(result.suggestedTargetGroupName());
        if (result.status() == TopicSuitabilityStatus.MISMATCH) {
            issues.add(issue(item, DocumentReadinessIssueType.LOW_TOPIC_SUITABILITY,
                    DocumentReadinessIssueSeverity.BLOCKER, "文档主题不适合当前知识库", result.reason()));
        } else if (result.status() == TopicSuitabilityStatus.WARNING || result.status() == TopicSuitabilityStatus.UNKNOWN) {
            issues.add(issue(item, DocumentReadinessIssueType.LOW_TOPIC_SUITABILITY,
                    DocumentReadinessIssueSeverity.WARNING, "文档主题适配度需要复核", result.reason()));
        }
    }

    private BigDecimal minScore(BigDecimal score, BigDecimal maxScore) {
        if (score == null) {
            return maxScore;
        }
        return score.compareTo(maxScore) > 0 ? maxScore : score;
    }

    private String joinValues(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(",", values);
    }
    private List<Document> readAndClean(DocumentReadinessItemEntity item) {
        DocumentEntity document = new DocumentEntity();
        document.setId(item.getId());
        document.setGroupId(item.getGroupId());
        document.setFileName(item.getOriginalFileName());
        document.setFileExt(item.getFileExt());
        document.setStorageBucket(item.getStorageBucket());
        document.setStorageObjectKey(item.getStorageObjectKey());
        StoredObjectDocumentReader reader = new StoredObjectDocumentReader(objectStorageService, parserFactory, document);
        return new TextCleanupTransformer().apply(reader.get());
    }

    private void addTextIssues(DocumentReadinessItemEntity item, String text, DryRunChunkAnalyzer.Metrics metrics,
                               DocumentEntity duplicate, List<DocumentReadinessIssueEntity> issues) {
        if (!StringUtils.hasText(text)) {
            issues.add(issue(item, DocumentReadinessIssueType.NO_EXTRACTED_TEXT,
                    DocumentReadinessIssueSeverity.BLOCKER, "未提取到有效文本", "请确认文件不是图片扫描件或空文档"));
        } else if (text.trim().length() < 300) {
            issues.add(issue(item, DocumentReadinessIssueType.VERY_SHORT_TEXT,
                    DocumentReadinessIssueSeverity.WARNING, "提取文本过短", "建议合并更多上下文或人工确认内容价值"));
        }
        if ("pdf".equals(item.getFileExt()) && text.length() < 200 && item.getFileSize() > 1024 * 1024) {
            issues.add(issue(item, DocumentReadinessIssueType.SUSPECTED_SCANNED_PDF,
                    DocumentReadinessIssueSeverity.BLOCKER, "疑似扫描版 PDF", "V5.0 MVP 不执行 OCR，仅标记为需要 OCR"));
        }
        if (metrics.chunkCount() > 0 && (double) metrics.emptyChunkCount() / metrics.chunkCount() > 0.1) {
            issues.add(issue(item, DocumentReadinessIssueType.TOO_MANY_EMPTY_CHUNKS,
                    DocumentReadinessIssueSeverity.WARNING, "空切片比例偏高", "建议清洗文档格式后再入库"));
        }
        if (metrics.chunkCount() > 0 && (double) metrics.shortChunkCount() / metrics.chunkCount() > 0.35) {
            issues.add(issue(item, DocumentReadinessIssueType.TOO_MANY_SHORT_CHUNKS,
                    DocumentReadinessIssueSeverity.WARNING, "短切片比例偏高", "建议补充上下文或调整文档结构"));
        }
        if (metrics.chunkCount() > 0 && (double) metrics.duplicateChunkCount() / metrics.chunkCount() > 0.2) {
            issues.add(issue(item, DocumentReadinessIssueType.EXCESSIVE_DUPLICATE_CHUNKS,
                    DocumentReadinessIssueSeverity.WARNING, "重复切片比例偏高", "建议去除页眉页脚或重复段落"));
        }
        if (duplicate != null) {
            issues.add(issue(item, DocumentReadinessIssueType.DUPLICATE_FILE_HASH,
                    DocumentReadinessIssueSeverity.WARNING, "目标知识库中已有相同哈希文档", "建议合并重复文档或跳过导入"));
        }
        if (item.getOriginalFileName().matches(".*(旧版|old|v\\d+|副本|copy).*")) {
            issues.add(issue(item, DocumentReadinessIssueType.POSSIBLE_OLD_VERSION,
                    DocumentReadinessIssueSeverity.INFO, "文件名显示可能是旧版或副本", "请确认是否为最新版本"));
        }
    }

    private DocumentReadinessItemStatus resolveStatus(DocumentReadinessGrade grade, DocumentReadinessAction action) {
        if (action == DocumentReadinessAction.REJECT || action == DocumentReadinessAction.NEEDS_OCR) {
            return DocumentReadinessItemStatus.REJECTED;
        }
        if (grade == DocumentReadinessGrade.RECOMMENDED || grade == DocumentReadinessGrade.ACCEPTABLE) {
            return DocumentReadinessItemStatus.RECOMMENDED;
        }
        return DocumentReadinessItemStatus.NEEDS_REVIEW;
    }

    private void finishFailedItem(DocumentReadinessItemEntity item, List<DocumentReadinessIssueEntity> issues,
                                  DocumentReadinessParseStatus parseStatus, String failureReason) {
        persistIssues(issues);
        item.setParseStatus(parseStatus.name());
        item.setReadinessStatus(DocumentReadinessItemStatus.FAILED.name());
        item.setRecommendedAction(DocumentReadinessAction.REJECT.name());
        item.setReadinessScore(java.math.BigDecimal.ZERO);
        item.setReadinessGrade(DocumentReadinessGrade.NOT_RECOMMENDED.name());
        item.setParseable(false);
        item.setIssueCount(issues.size());
        item.setFailureReason(failureReason);
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
    }

    private DocumentReadinessIssueEntity issue(DocumentReadinessItemEntity item, DocumentReadinessIssueType type,
                                               DocumentReadinessIssueSeverity severity, String message, String suggestion) {
        DocumentReadinessIssueEntity issue = new DocumentReadinessIssueEntity();
        issue.setBatchId(item.getBatchId());
        issue.setItemId(item.getId());
        issue.setIssueType(type.name());
        issue.setSeverity(severity.name());
        issue.setMessage(message);
        issue.setSuggestion(suggestion);
        issue.setCreatedAt(LocalDateTime.now());
        return issue;
    }

    private void persistIssues(List<DocumentReadinessIssueEntity> issues) {
        for (DocumentReadinessIssueEntity issue : issues) {
            issueMapper.insert(issue);
        }
    }

    public void refreshBatchStats(Long batchId) {
        List<DocumentReadinessItemEntity> items = itemMapper.selectList(new LambdaQueryWrapper<DocumentReadinessItemEntity>()
                .eq(DocumentReadinessItemEntity::getBatchId, batchId));
        int total = items.size();
        int ready = (int) items.stream().filter(item -> DocumentReadinessItemStatus.RECOMMENDED.name().equals(item.getReadinessStatus())
                || DocumentReadinessItemStatus.APPROVED.name().equals(item.getReadinessStatus())
                || DocumentReadinessItemStatus.IMPORTED.name().equals(item.getReadinessStatus())).count();
        int rejected = (int) items.stream().filter(item -> DocumentReadinessItemStatus.REJECTED.name().equals(item.getReadinessStatus())
                || DocumentReadinessItemStatus.FAILED.name().equals(item.getReadinessStatus())).count();
        int warning = (int) items.stream().filter(item -> DocumentReadinessItemStatus.NEEDS_REVIEW.name().equals(item.getReadinessStatus())).count();
        java.math.BigDecimal avg = items.stream()
                .filter(item -> item.getReadinessScore() != null)
                .map(DocumentReadinessItemEntity::getReadinessScore)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        if (items.stream().anyMatch(item -> item.getReadinessScore() != null)) {
            long scored = items.stream().filter(item -> item.getReadinessScore() != null).count();
            avg = avg.divide(java.math.BigDecimal.valueOf(scored), 2, java.math.RoundingMode.HALF_UP);
        } else {
            avg = null;
        }
        DocumentReadinessBatchEntity update = new DocumentReadinessBatchEntity();
        update.setId(batchId);
        update.setTotalCount(total);
        update.setReadyCount(ready);
        update.setWarningCount(warning);
        update.setRejectedCount(rejected);
        update.setAvgReadinessScore(avg);
        update.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(update);
    }

    private void updateBatchStatus(Long batchId, DocumentReadinessBatchStatus status, String failureReason) {
        DocumentReadinessBatchEntity update = new DocumentReadinessBatchEntity();
        update.setId(batchId);
        update.setStatus(status.name());
        update.setFailureReason(failureReason);
        update.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(update);
    }

    private MultipartFile requireFile(AddReadinessItemRequest request) {
        MultipartFile file = request == null ? null : request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("候选文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("候选文件超过大小限制");
        }
        return file;
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException("文件名不能为空");
        }
        String sanitized = fileName.replace("\\", "/");
        sanitized = sanitized.substring(sanitized.lastIndexOf('/') + 1).trim();
        if (!StringUtils.hasText(sanitized) || sanitized.length() > 255) {
            throw new BusinessException("文件名非法");
        }
        return sanitized;
    }

    private String extractFileExt(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType.trim() : "application/octet-stream";
    }

    private String calculateSha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
        } catch (IOException exception) {
            throw new BusinessException("读取候选文件失败");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}

