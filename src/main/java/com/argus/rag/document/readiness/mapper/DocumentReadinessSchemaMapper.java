package com.argus.rag.document.readiness.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentReadinessSchemaMapper {
    void createBatchesTable();

    void createItemsTable();

    void createIssuesTable();

    void createBatchesGroupStatusIndex();

    void createBatchesOwnerCreatedIndex();

    void createItemsBatchStatusIndex();

    void createItemsGroupHashIndex();

    void createIssuesItemIndex();

    void addTopicFitStatusColumn();

    void addTopicFitScoreColumn();

    void addTopicConfidenceColumn();

    void addTopicReasonColumn();

    void addGroupTopicKeywordsColumn();

    void addDocumentTopicKeywordsColumn();

    void addKeywordOverlapScoreColumn();

    void addSemanticSimilarityScoreColumn();

    void addMismatchIndicatorsColumn();

    void addSuggestedTargetGroupIdColumn();

    void addSuggestedTargetGroupNameColumn();
}