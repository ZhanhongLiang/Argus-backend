package com.argus.rag.document.readiness.mapper;

import com.argus.rag.document.readiness.model.dto.ReadinessBatchQuery;
import com.argus.rag.document.readiness.model.entity.DocumentReadinessBatchEntity;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessBatchVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentReadinessBatchMapper extends BaseMapper<DocumentReadinessBatchEntity> {
    List<DocumentReadinessBatchVO> selectReadableBatches(
            @Param("query") ReadinessBatchQuery query,
            @Param("currentUserId") Long currentUserId,
            @Param("systemAdmin") boolean systemAdmin
    );

    DocumentReadinessBatchVO selectReadableBatchDetail(
            @Param("batchId") Long batchId,
            @Param("currentUserId") Long currentUserId,
            @Param("systemAdmin") boolean systemAdmin
    );
}
