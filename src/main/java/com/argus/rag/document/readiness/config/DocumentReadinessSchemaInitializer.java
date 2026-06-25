package com.argus.rag.document.readiness.config;

import com.argus.rag.document.readiness.mapper.DocumentReadinessSchemaMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocumentReadinessSchemaInitializer implements ApplicationRunner {
    private final DocumentReadinessSchemaMapper schemaMapper;

    public DocumentReadinessSchemaInitializer(DocumentReadinessSchemaMapper schemaMapper) {
        this.schemaMapper = schemaMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        schemaMapper.createBatchesTable();
        schemaMapper.createItemsTable();
        schemaMapper.createIssuesTable();
        schemaMapper.createBatchesGroupStatusIndex();
        schemaMapper.createBatchesOwnerCreatedIndex();
        schemaMapper.createItemsBatchStatusIndex();
        schemaMapper.createItemsGroupHashIndex();
        schemaMapper.createIssuesItemIndex();
        schemaMapper.addTopicFitStatusColumn();
        schemaMapper.addTopicFitScoreColumn();
        schemaMapper.addTopicConfidenceColumn();
        schemaMapper.addTopicReasonColumn();
        schemaMapper.addGroupTopicKeywordsColumn();
        schemaMapper.addDocumentTopicKeywordsColumn();
        schemaMapper.addKeywordOverlapScoreColumn();
        schemaMapper.addSemanticSimilarityScoreColumn();
        schemaMapper.addMismatchIndicatorsColumn();
        schemaMapper.addSuggestedTargetGroupIdColumn();
        schemaMapper.addSuggestedTargetGroupNameColumn();
        log.info("V5.1 document readiness topic columns initialized");
    }
}