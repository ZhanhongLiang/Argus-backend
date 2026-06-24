package com.argus.rag.qa.mapper;

import com.argus.rag.qa.model.dto.QaRecordSearchRequest;
import com.argus.rag.qa.model.entity.QaRecordEntity;
import com.argus.rag.qa.model.vo.QaAnalyticsSummaryVO;
import com.argus.rag.qa.model.vo.QaDimensionItemVO;
import com.argus.rag.qa.model.vo.QaTrendPointVO;
import com.argus.rag.qa.service.QaRecordScopeResolver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Mapper for V4.2 QA analytics and governance queries. */
@Mapper
public interface QaAnalyticsMapper {

    QaAnalyticsSummaryVO selectSummary(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                                       @Param("q") QaRecordSearchRequest request);

    List<QaTrendPointVO> selectTrends(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                                      @Param("q") QaRecordSearchRequest request);

    List<QaDimensionItemVO> selectEvidenceLevels(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                                                 @Param("q") QaRecordSearchRequest request);

    List<QaDimensionItemVO> selectModels(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                                         @Param("q") QaRecordSearchRequest request);

    List<QaDimensionItemVO> selectEndpoints(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                                            @Param("q") QaRecordSearchRequest request);

    List<QaDimensionItemVO> selectRetrievalSources(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                                                   @Param("q") QaRecordSearchRequest request);

    List<QaDimensionItemVO> selectUsers(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                                        @Param("q") QaRecordSearchRequest request);

    List<QaRecordEntity> selectSearchRecords(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                                             @Param("q") QaRecordSearchRequest request,
                                             @Param("offset") long offset,
                                             @Param("limit") int limit);

    Long countSearchRecords(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                            @Param("q") QaRecordSearchRequest request);

    List<QaRecordEntity> selectRecordsByIds(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                                            @Param("recordIds") List<Long> recordIds,
                                            @Param("limit") int limit);

    int softDeleteByIds(@Param("scopeCtx") QaRecordScopeResolver.ScopeContext scopeContext,
                        @Param("recordIds") List<Long> recordIds,
                        @Param("deleteReason") String deleteReason);
}
